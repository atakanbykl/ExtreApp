package com.example.extreapp.domain.backup

import com.example.extreapp.data.local.entity.CategoryEntity
import com.example.extreapp.data.local.entity.MerchantEntity
import com.example.extreapp.data.local.entity.RuleEntity
import com.example.extreapp.data.local.entity.TransactionEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class ExtreBackupPayload(
    val schemaVersion: Int = 1,
    val exportedAtEpochMs: Long = System.currentTimeMillis(),
    val appName: String = "ExtreApp",
    val categories: List<CategoryEntity>,
    val merchants: List<MerchantEntity>,
    val rules: List<RuleEntity>,
    val transactions: List<TransactionEntity>
)

data class BackupRestoreReport(
    val categoriesCount: Int,
    val merchantsCount: Int,
    val rulesCount: Int,
    val transactionsCount: Int
)

object BackupManager {
    private val jsonFormat = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun serializeToJson(payload: ExtreBackupPayload): String {
        return jsonFormat.encodeToString(payload)
    }

    fun deserializeFromJson(jsonString: String): ExtreBackupPayload {
        return jsonFormat.decodeFromString<ExtreBackupPayload>(jsonString)
    }
}

