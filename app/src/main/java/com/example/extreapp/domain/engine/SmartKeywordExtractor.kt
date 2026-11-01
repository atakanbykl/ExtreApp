package com.example.extreapp.domain.engine

data class KeywordSuggestion(
    val cleanedDescription: String,
    val suggestedKeyword: String,
    val suggestedMerchantName: String
)

object SmartKeywordExtractor {
    private val installmentRegex = Regex("""\(\s*0?\d{1,2}\s*/\s*0?\d{1,2}\s*\)""")
    private val refundTagRegex = Regex("""(?i)(-\s*İADE|-\s*IADE|^İADE\s*-\s*|^IADE\s*-\s*)""")
    private val trailingCountryRegex = Regex("""\s+(TR|TUR|GBR|USA|NLD|IRL|LUX|DEU)\s*$""", RegexOption.IGNORE_CASE)
    private val trailingCityRegex = Regex(
        """\s+(ANKARA|İSTANBUL|ISTANBUL|İZMİR|IZMIR|ANTALYA|KONYA|ESKİŞEHİR|ESKISEHIR|KOCAELİ|KOCAELI|BURSA|ADANA|MUĞLA|MUGLA|LONDON|BELLEVUE|DUBLIN)\s*$""",
        RegexOption.IGNORE_CASE
    )
    private val gatewayPrefixRegex = Regex(
        """^(IYZICO|İYZİCO|PAYCELL|TKPAY\.?|ELEKSE|PARAM|SIPAY|PAPARA|MOKA|ODEAL)\s*/\s*""",
        RegexOption.IGNORE_CASE
    )
    private val trailingDigitsRegex = Regex("""\s+\d{4,}\s*$""")

    fun analyze(rawDescription: String): KeywordSuggestion {
        var working = rawDescription.trim()

        // 1. Remove installment tags like (05/09) or (01/06)
        working = installmentRegex.replace(working, " ").trim()

        // 2. Remove refund tags like "- İADE" or "İade-"
        working = refundTagRegex.replace(working, " ").trim()

        // 3. Remove fixed-width city/country suffixes or trailing country/city tokens
        working = trailingCountryRegex.replace(working, "").trim()
        // If there was a big space gap before the city (typical 25-char POS merchant name + 13-char city)
        val multiSpaceParts = working.split(Regex("""\s{3,}""")).map { it.trim() }.filter { it.isNotEmpty() }
        if (multiSpaceParts.size >= 2) {
            val lastPartNorm = TurkishTextNormalizer.normalize(multiSpaceParts.last())
            if (isKnownCityOrLocation(lastPartNorm)) {
                working = multiSpaceParts.dropLast(1).joinToString(" ")
            } else {
                working = multiSpaceParts.joinToString(" ")
            }
        } else {
            working = trailingCityRegex.replace(working, "").trim()
        }

        // Collapse multiple spaces
        val cleanedDescription = working.replace(Regex("""\s+"""), " ").trim()

        // 4. Derive smart keyword and merchant name
        val norm = TurkishTextNormalizer.normalize(cleanedDescription)

        return when {
            norm.contains("amazon.com") -> KeywordSuggestion(
                cleanedDescription = cleanedDescription,
                suggestedKeyword = "AMAZON.COM",
                suggestedMerchantName = "Amazon"
            )
            norm.contains("spotify") -> KeywordSuggestion(
                cleanedDescription = cleanedDescription,
                suggestedKeyword = "SPOTİFY",
                suggestedMerchantName = "Spotify"
            )
            norm.contains("yemeksepet") -> KeywordSuggestion(
                cleanedDescription = cleanedDescription,
                suggestedKeyword = "YEMEKSEPET",
                suggestedMerchantName = "Yemeksepeti"
            )
            norm.contains("hepsipay") || norm.contains("hepsibu") -> KeywordSuggestion(
                cleanedDescription = cleanedDescription,
                suggestedKeyword = "HEPSİPAY",
                suggestedMerchantName = "Hepsiburada"
            )
            norm.contains("youtube") -> KeywordSuggestion(
                cleanedDescription = cleanedDescription,
                suggestedKeyword = "GOOGLE *YOUTUBE",
                suggestedMerchantName = "YouTube"
            )
            norm.contains("google one") -> KeywordSuggestion(
                cleanedDescription = cleanedDescription,
                suggestedKeyword = "GOOGLE *GOOGLE ONE",
                suggestedMerchantName = "Google One"
            )
            norm.contains("steamgames") -> KeywordSuggestion(
                cleanedDescription = cleanedDescription,
                suggestedKeyword = "STEAMGAMES.COM",
                suggestedMerchantName = "Steam"
            )
            norm.contains("getir") -> KeywordSuggestion(
                cleanedDescription = cleanedDescription,
                suggestedKeyword = "GETIR",
                suggestedMerchantName = "Getir"
            )
            norm.contains("starbucks") || norm.startsWith("sbux ") -> KeywordSuggestion(
                cleanedDescription = cleanedDescription,
                suggestedKeyword = if (norm.startsWith("sbux")) "SBUX" else "STARBUCKS",
                suggestedMerchantName = "Starbucks"
            )
            norm.contains("burger king") -> KeywordSuggestion(
                cleanedDescription = cleanedDescription,
                suggestedKeyword = "BURGER KİNG",
                suggestedMerchantName = "Burger King"
            )
            norm.contains("ajet hava") -> KeywordSuggestion(
                cleanedDescription = cleanedDescription,
                suggestedKeyword = "AJET HAVA",
                suggestedMerchantName = "AJet"
            )
            else -> {
                // Strip payment gateway prefix if present (e.g., PAYCELL/TORYUM GIDA -> TORYUM GIDA)
                var core = gatewayPrefixRegex.replace(cleanedDescription, "").trim()
                core = trailingDigitsRegex.replace(core, "").trim()
                if (core.isEmpty()) core = cleanedDescription

                val displayMerchant = cleanCorporateSuffixes(core)
                KeywordSuggestion(
                    cleanedDescription = cleanedDescription,
                    suggestedKeyword = core.uppercase(java.util.Locale.forLanguageTag("tr-TR")),
                    suggestedMerchantName = TurkishTextNormalizer.toDisplayTitleCase(displayMerchant)
                )
            }
        }
    }

    private fun cleanCorporateSuffixes(name: String): String {
        return name
            .replace(Regex("""(?i)\b(TİC\.?|TIC\.?|SAN\.?|LTD\.?|ŞTİ\.?|STI\.?|A\.?Ş\.?|VE)\b.*$"""), "")
            .trim()
            .ifEmpty { name }
    }

    private fun isKnownCityOrLocation(norm: String): Boolean {
        val cities = setOf(
            "ankara", "istanbul", "izmir", "antalya", "konya", "eskisehir",
            "kocaeli", "bursa", "adana", "mugla", "mersin", "gaziantep",
            "kayseri", "samsun", "trabzon", "london", "bellevue", "dublin",
            "ankara tr", "istanbul tr", "istanbul tur", "ankara tur", "london gbr"
        )
        return norm in cities || cities.any { norm.endsWith(" $it") }
    }
}

