package com.example.extreapp.domain.parser

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

data class DecodedTextResult(
    val content: String,
    val charsetName: String
)

object CharsetDetector {
    private val windows1254: Charset = try {
        Charset.forName("windows-1254")
    } catch (_: Exception) {
        Charset.forName("ISO-8859-9")
    }

    /**
     * Decodes a byte array by checking BOM first, then validating strict UTF-8,
     * and automatically falling back to Turkish Windows-1254 (CP1254 / ISO-8859-9)
     * when legacy Turkish bank encodings are encountered (such as HESAP_OZETI.csv).
     */
    fun decodeBytes(bytes: ByteArray): DecodedTextResult {
        if (bytes.isEmpty()) return DecodedTextResult("", "UTF-8")

        // Check UTF-8 BOM (EF BB BF)
        if (bytes.size >= 3 &&
            bytes[0] == 0xEF.toByte() &&
            bytes[1] == 0xBB.toByte() &&
            bytes[2] == 0xBF.toByte()
        ) {
            return DecodedTextResult(
                content = String(bytes, 3, bytes.size - 3, Charsets.UTF_8),
                charsetName = "UTF-8 (BOM)"
            )
        }

        // Check UTF-16LE BOM (FF FE)
        if (bytes.size >= 2 &&
            bytes[0] == 0xFF.toByte() &&
            bytes[1] == 0xFE.toByte()
        ) {
            return DecodedTextResult(
                content = String(bytes, 2, bytes.size - 2, Charsets.UTF_16LE),
                charsetName = "UTF-16LE"
            )
        }

        // Check UTF-16BE BOM (FE FF)
        if (bytes.size >= 2 &&
            bytes[0] == 0xFE.toByte() &&
            bytes[1] == 0xFF.toByte()
        ) {
            return DecodedTextResult(
                content = String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE),
                charsetName = "UTF-16BE"
            )
        }

        // Try strict UTF-8 decoding
        val utf8Decoder = Charsets.UTF_8
            .newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)

        return try {
            val charBuffer = utf8Decoder.decode(ByteBuffer.wrap(bytes))
            DecodedTextResult(
                content = charBuffer.toString(),
                charsetName = "UTF-8"
            )
        } catch (_: CharacterCodingException) {
            // Fallback to Turkish Windows-1254
            DecodedTextResult(
                content = String(bytes, windows1254),
                charsetName = "windows-1254"
            )
        }
    }
}

