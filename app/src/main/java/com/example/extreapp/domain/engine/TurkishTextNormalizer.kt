package com.example.extreapp.domain.engine

import java.util.Locale

object TurkishTextNormalizer {
    private val turkishLocale = Locale.forLanguageTag("tr-TR")

    /**
     * Normalizes Turkish and English characters into a canonical ASCII lowercase string
     * and collapses multiple whitespace characters into a single space.
     * Example: "İYZİCO /SPOTİFY.COM  ISTANBUL" -> "iyzico /spotify.com istanbul"
     */
    fun normalize(input: String): String {
        if (input.isBlank()) return ""
        val sb = StringBuilder(input.length)
        var lastWasSpace = true
        for (ch in input) {
            val mapped = when (ch) {
                'İ', 'I', 'ı', 'i' -> 'i'
                'Ş', 'ş' -> 's'
                'Ğ', 'ğ' -> 'g'
                'Ü', 'ü' -> 'u'
                'Ö', 'ö' -> 'o'
                'Ç', 'ç' -> 'c'
                'Â', 'â' -> 'a'
                'Î', 'î' -> 'i'
                'Û', 'û' -> 'u'
                else -> ch.lowercaseChar()
            }
            if (mapped.isWhitespace()) {
                if (!lastWasSpace) {
                    sb.append(' ')
                    lastWasSpace = true
                }
            } else {
                sb.append(mapped)
                lastWasSpace = false
            }
        }
        return sb.toString().trim()
    }

    /**
     * Converts a raw merchant string into a clean Title Case display name in Turkish locale.
     */
    fun toDisplayTitleCase(input: String): String {
        val trimmed = input.trim().replace(Regex("\\s+"), " ")
        if (trimmed.isEmpty()) return ""
        return trimmed.split(" ").joinToString(" ") { word ->
            if (word.length <= 1) {
                word.uppercase(turkishLocale)
            } else {
                val first = word.substring(0, 1).uppercase(turkishLocale)
                val rest = word.substring(1).lowercase(turkishLocale)
                first + rest
            }
        }
    }
}

