package com.example.extreapp.data.local.entity

import kotlinx.serialization.Serializable

@Serializable
enum class TransactionType {
    EXPENSE,        // Normal veya taksitli harcama
    REFUND,         // Ürün / hizmet / taksit iadesi
    PAYMENT,        // Ekstre borç ödemesi (Örn: İNTERNET Şb-Ödemeniz için Teşekkürler)
    REWARD_INFO     // 0,00 TL tutarlı Chip-Para / Kampanya bilgi satırları
}

@Serializable
data class TransactionEntity(
    val id: Long = 0,
    val dateEpochDay: Long,
    val yearMonth: String,                    // İşlem tarihine göre "YYYY-MM"
    val statementYearMonth: String,           // Ekstre hesap kesim tarihine göre "YYYY-MM"
    val rawDescription: String,
    val cleanedDescription: String,
    val amount: Double,                       // Mutlak pozitif değer
    val signedAmount: Double,                 // Ekstredeki işaretli değer (Harcama +, İade/Ödeme -)
    val currency: String = "TRY",
    val foreignCurrencyNote: String? = null,  // Örn: "44.48 USD"
    val rewardPointsText: String? = null,     // Örn: "14,96 TL" veya "200,00 TL"
    val type: TransactionType = TransactionType.EXPENSE,
    val installmentCurrent: Int? = null,
    val installmentTotal: Int? = null,
    val cardInfo: String? = null,
    val merchantId: Long? = null,
    val categoryId: Long? = null,
    val matchedRuleId: Long? = null,
    val isApproved: Boolean = false,
    val sourceFileName: String = "",
    val dedupHash: String
)

