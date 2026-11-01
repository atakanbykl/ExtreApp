package com.example.extreapp.domain.parser

import java.io.ByteArrayInputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

object ExcelStatementParser {

    fun parseRawTable(bytes: ByteArray, fileName: String): RawStatementTable {
        if (bytes.size >= 4 &&
            bytes[0] == 0x50.toByte() && // 'P'
            bytes[1] == 0x4B.toByte()    // 'K' (ZIP header -> .xlsx)
        ) {
            return parseXlsxZip(bytes, fileName)
        }

        // Many Turkish banks export HTML tables or XML SpreadsheetML or TSV files with a .xls extension
        val decoded = CharsetDetector.decodeBytes(bytes)
        val trimmed = decoded.content.trimStart()
        if (trimmed.contains("<table", ignoreCase = true) || trimmed.contains("<tr", ignoreCase = true)) {
            return parseHtmlTableXls(decoded.content, fileName, decoded.charsetName)
        }
        if (trimmed.contains("<Row", ignoreCase = true) && trimmed.contains("<Cell", ignoreCase = true)) {
            return parseSpreadsheetXml(decoded.content, fileName, decoded.charsetName)
        }

        // Fallback to CSV / TSV parser
        return CsvStatementParser.parseFromText(decoded.content, fileName, decoded.charsetName)
    }

    private fun parseXlsxZip(bytes: ByteArray, fileName: String): RawStatementTable {
        val sharedStrings = mutableListOf<String>()
        var sheetXmlBytes: ByteArray? = null

        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val name = entry.name
                when {
                    name.equals("xl/sharedStrings.xml", ignoreCase = true) -> {
                        sharedStrings.addAll(parseSharedStrings(zis.readBytes()))
                    }
                    sheetXmlBytes == null && name.startsWith("xl/worksheets/sheet", ignoreCase = true) && name.endsWith(".xml") -> {
                        sheetXmlBytes = zis.readBytes()
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        val rows = if (sheetXmlBytes != null) {
            parseWorksheetRows(sheetXmlBytes!!, sharedStrings)
        } else {
            emptyList()
        }

        return buildTableFromRows(rows, fileName, "XLSX (OpenXML)")
    }

    private fun parseSharedStrings(xmlBytes: ByteArray): List<String> {
        val result = mutableListOf<String>()
        try {
            val factory = DocumentBuilderFactory.newInstance()
            val doc = factory.newDocumentBuilder().parse(ByteArrayInputStream(xmlBytes))
            val siNodes = doc.getElementsByTagName("si")
            for (i in 0 until siNodes.length) {
                val si = siNodes.item(i)
                result.add(si.textContent ?: "")
            }
        } catch (_: Exception) {
            // ignore malformed sharedStrings
        }
        return result
    }

    private fun parseWorksheetRows(xmlBytes: ByteArray, sharedStrings: List<String>): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        try {
            val factory = DocumentBuilderFactory.newInstance()
            val doc = factory.newDocumentBuilder().parse(ByteArrayInputStream(xmlBytes))
            val rowNodes = doc.getElementsByTagName("row")
            for (r in 0 until rowNodes.length) {
                val rowElem = rowNodes.item(r)
                val cellNodes = rowElem.childNodes
                val rowValues = mutableListOf<String>()
                for (c in 0 until cellNodes.length) {
                    val cell = cellNodes.item(c)
                    if (cell.nodeName != "c") continue
                    val typeAttr = cell.attributes?.getNamedItem("t")?.nodeValue
                    val cellRef = cell.attributes?.getNamedItem("r")?.nodeValue
                    val colIndex = cellRef?.let { columnRefToIndex(it) } ?: rowValues.size
                    while (rowValues.size < colIndex) {
                        rowValues.add("")
                    }

                    var rawValue = ""
                    val children = cell.childNodes
                    for (k in 0 until children.length) {
                        val child = children.item(k)
                        if (child.nodeName == "v" || child.nodeName == "is") {
                            rawValue = child.textContent ?: ""
                            break
                        }
                    }

                    val resolved = when (typeAttr) {
                        "s" -> {
                            val idx = rawValue.toIntOrNull()
                            if (idx != null && idx in sharedStrings.indices) sharedStrings[idx] else rawValue
                        }
                        else -> maybeConvertExcelSerialDate(rawValue)
                    }
                    rowValues.add(resolved.trim())
                }
                if (rowValues.any { it.isNotEmpty() }) {
                    rows.add(rowValues)
                }
            }
        } catch (_: Exception) {
            // ignore
        }
        return rows
    }

    private fun columnRefToIndex(cellRef: String): Int {
        val letters = cellRef.takeWhile { it.isLetter() }.uppercase()
        var index = 0
        for (ch in letters) {
            index = index * 26 + (ch - 'A' + 1)
        }
        return (index - 1).coerceAtLeast(0)
    }

    private fun maybeConvertExcelSerialDate(raw: String): String {
        val serial = raw.toIntOrNull() ?: return raw
        // Excel serial dates between year 2015 (~42005) and 2035 (~49310)
        if (serial in 42000..49500) {
            val baseDate = LocalDate.of(1899, 12, 30)
            val date = baseDate.plusDays(serial.toLong())
            return date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        }
        return raw
    }

    private fun parseHtmlTableXls(html: String, fileName: String, charsetName: String): RawStatementTable {
        val trRegex = Regex("""<tr[^>]*>(.*?)</tr>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        val tdRegex = Regex("""<t[dh][^>]*>(.*?)</t[dh]>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        val tagStripRegex = Regex("""<[^>]+>""")

        val rows = mutableListOf<List<String>>()
        for (trMatch in trRegex.findAll(html)) {
            val rowHtml = trMatch.groupValues[1]
            val cells = tdRegex.findAll(rowHtml).map { td ->
                tagStripRegex.replace(td.groupValues[1], " ")
                    .replace("&nbsp;", " ")
                    .replace("&amp;", "&")
                    .replace(Regex("""\s+"""), " ")
                    .trim()
            }.toList()
            if (cells.any { it.isNotEmpty() }) {
                rows.add(cells)
            }
        }
        return buildTableFromRows(rows, fileName, charsetName)
    }

    private fun parseSpreadsheetXml(xml: String, fileName: String, charsetName: String): RawStatementTable {
        val rowRegex = Regex("""<Row[^>]*>(.*?)</Row>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        val cellRegex = Regex("""<Data[^>]*>(.*?)</Data>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        val rows = rowRegex.findAll(xml).map { rowMatch ->
            cellRegex.findAll(rowMatch.groupValues[1]).map { it.groupValues[1].trim() }.toList()
        }.filter { it.any { cell -> cell.isNotEmpty() } }.toList()
        return buildTableFromRows(rows, fileName, charsetName)
    }

    private fun buildTableFromRows(rows: List<List<String>>, fileName: String, charsetName: String): RawStatementTable {
        if (rows.isEmpty()) {
            return RawStatementTable(fileName = fileName, detectedCharset = charsetName)
        }
        val syntheticCsv = rows.joinToString("\n") { row ->
            row.joinToString(";") { it.replace(";", ",") }
        }
        return CsvStatementParser.parseFromText(syntheticCsv, fileName, charsetName)
    }
}

