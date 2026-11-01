package com.example.extreapp.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.extreapp.data.local.entity.CategoryEntity
import com.example.extreapp.data.local.entity.MerchantEntity
import com.example.extreapp.data.local.entity.RuleEntity
import com.example.extreapp.data.local.entity.TransactionEntity
import com.example.extreapp.data.local.entity.TransactionType

interface ExtrePersistenceStore {
    fun loadCategories(): List<CategoryEntity>
    fun loadMerchants(): List<MerchantEntity>
    fun loadRules(): List<RuleEntity>
    fun loadTransactions(): List<TransactionEntity>
    fun saveAll(
        categories: List<CategoryEntity>,
        merchants: List<MerchantEntity>,
        rules: List<RuleEntity>,
        transactions: List<TransactionEntity>
    )
}

class InMemoryPersistenceStore(
    initialCategories: List<CategoryEntity> = DefaultCategories.INITIAL_CATEGORIES
) : ExtrePersistenceStore {
    private var categories = initialCategories.toList()
    private var merchants = emptyList<MerchantEntity>()
    private var rules = emptyList<RuleEntity>()
    private var transactions = emptyList<TransactionEntity>()

    override fun loadCategories(): List<CategoryEntity> = categories
    override fun loadMerchants(): List<MerchantEntity> = merchants
    override fun loadRules(): List<RuleEntity> = rules
    override fun loadTransactions(): List<TransactionEntity> = transactions

    override fun saveAll(
        categories: List<CategoryEntity>,
        merchants: List<MerchantEntity>,
        rules: List<RuleEntity>,
        transactions: List<TransactionEntity>
    ) {
        this.categories = categories.toList()
        this.merchants = merchants.toList()
        this.rules = rules.toList()
        this.transactions = transactions.toList()
    }
}

class ExtreSqliteHelper(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION),
    ExtrePersistenceStore {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE categories (
                id INTEGER PRIMARY KEY,
                name TEXT NOT NULL,
                iconName TEXT NOT NULL,
                colorHex INTEGER NOT NULL,
                isDefault INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE merchants (
                id INTEGER PRIMARY KEY,
                displayName TEXT NOT NULL,
                normalizedName TEXT NOT NULL UNIQUE,
                defaultCategoryId INTEGER
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE rules (
                id INTEGER PRIMARY KEY,
                keyword TEXT NOT NULL,
                merchantId INTEGER NOT NULL,
                categoryId INTEGER NOT NULL,
                priority INTEGER NOT NULL,
                createdAtEpoch INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE transactions (
                id INTEGER PRIMARY KEY,
                dateEpochDay INTEGER NOT NULL,
                yearMonth TEXT NOT NULL,
                statementYearMonth TEXT NOT NULL,
                rawDescription TEXT NOT NULL,
                cleanedDescription TEXT NOT NULL,
                amount REAL NOT NULL,
                signedAmount REAL NOT NULL,
                currency TEXT NOT NULL,
                foreignCurrencyNote TEXT,
                rewardPointsText TEXT,
                type TEXT NOT NULL,
                installmentCurrent INTEGER,
                installmentTotal INTEGER,
                cardInfo TEXT,
                merchantId INTEGER,
                categoryId INTEGER,
                matchedRuleId INTEGER,
                isApproved INTEGER NOT NULL,
                sourceFileName TEXT NOT NULL,
                dedupHash TEXT NOT NULL UNIQUE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_tx_yearMonth ON transactions(yearMonth)")
        db.execSQL("CREATE INDEX idx_tx_stmtYearMonth ON transactions(statementYearMonth)")
        db.execSQL("CREATE INDEX idx_tx_merchantId ON transactions(merchantId)")
        db.execSQL("CREATE INDEX idx_tx_categoryId ON transactions(categoryId)")
        db.execSQL("CREATE INDEX idx_tx_isApproved ON transactions(isApproved)")

        for (cat in DefaultCategories.INITIAL_CATEGORIES) {
            val cv = ContentValues().apply {
                put("id", cat.id)
                put("name", cat.name)
                put("iconName", cat.iconName)
                put("colorHex", cat.colorHex)
                put("isDefault", if (cat.isDefault) 1 else 0)
            }
            db.insert("categories", null, cv)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS transactions")
        db.execSQL("DROP TABLE IF EXISTS rules")
        db.execSQL("DROP TABLE IF EXISTS merchants")
        db.execSQL("DROP TABLE IF EXISTS categories")
        onCreate(db)
    }

    override fun loadCategories(): List<CategoryEntity> {
        val list = mutableListOf<CategoryEntity>()
        readableDatabase.rawQuery("SELECT id, name, iconName, colorHex, isDefault FROM categories ORDER BY id ASC", null).use { c ->
            while (c.moveToNext()) {
                list.add(
                    CategoryEntity(
                        id = c.getLong(0),
                        name = c.getString(1),
                        iconName = c.getString(2),
                        colorHex = c.getLong(3),
                        isDefault = c.getInt(4) == 1
                    )
                )
            }
        }
        if (list.isEmpty()) {
            return DefaultCategories.INITIAL_CATEGORIES
        }
        return list
    }

    override fun loadMerchants(): List<MerchantEntity> {
        val list = mutableListOf<MerchantEntity>()
        readableDatabase.rawQuery("SELECT id, displayName, normalizedName, defaultCategoryId FROM merchants ORDER BY displayName ASC", null).use { c ->
            while (c.moveToNext()) {
                list.add(
                    MerchantEntity(
                        id = c.getLong(0),
                        displayName = c.getString(1),
                        normalizedName = c.getString(2),
                        defaultCategoryId = if (c.isNull(3)) null else c.getLong(3)
                    )
                )
            }
        }
        return list
    }

    override fun loadRules(): List<RuleEntity> {
        val list = mutableListOf<RuleEntity>()
        readableDatabase.rawQuery("SELECT id, keyword, merchantId, categoryId, priority, createdAtEpoch FROM rules ORDER BY priority DESC, id ASC", null).use { c ->
            while (c.moveToNext()) {
                list.add(
                    RuleEntity(
                        id = c.getLong(0),
                        keyword = c.getString(1),
                        merchantId = c.getLong(2),
                        categoryId = c.getLong(3),
                        priority = c.getInt(4),
                        createdAtEpoch = c.getLong(5)
                    )
                )
            }
        }
        return list
    }

    override fun loadTransactions(): List<TransactionEntity> {
        val list = mutableListOf<TransactionEntity>()
        readableDatabase.rawQuery(
            """
            SELECT id, dateEpochDay, yearMonth, statementYearMonth, rawDescription, cleanedDescription,
                   amount, signedAmount, currency, foreignCurrencyNote, rewardPointsText, type,
                   installmentCurrent, installmentTotal, cardInfo, merchantId, categoryId, matchedRuleId,
                   isApproved, sourceFileName, dedupHash
            FROM transactions
            ORDER BY dateEpochDay DESC, id DESC
            """.trimIndent(),
            null
        ).use { c ->
            while (c.moveToNext()) {
                list.add(
                    TransactionEntity(
                        id = c.getLong(0),
                        dateEpochDay = c.getLong(1),
                        yearMonth = c.getString(2),
                        statementYearMonth = c.getString(3),
                        rawDescription = c.getString(4),
                        cleanedDescription = c.getString(5),
                        amount = c.getDouble(6),
                        signedAmount = c.getDouble(7),
                        currency = c.getString(8),
                        foreignCurrencyNote = if (c.isNull(9)) null else c.getString(9),
                        rewardPointsText = if (c.isNull(10)) null else c.getString(10),
                        type = runCatching { TransactionType.valueOf(c.getString(11)) }.getOrDefault(TransactionType.EXPENSE),
                        installmentCurrent = if (c.isNull(12)) null else c.getInt(12),
                        installmentTotal = if (c.isNull(13)) null else c.getInt(13),
                        cardInfo = if (c.isNull(14)) null else c.getString(14),
                        merchantId = if (c.isNull(15)) null else c.getLong(15),
                        categoryId = if (c.isNull(16)) null else c.getLong(16),
                        matchedRuleId = if (c.isNull(17)) null else c.getLong(17),
                        isApproved = c.getInt(18) == 1,
                        sourceFileName = c.getString(19),
                        dedupHash = c.getString(20)
                    )
                )
            }
        }
        return list
    }

    override fun saveAll(
        categories: List<CategoryEntity>,
        merchants: List<MerchantEntity>,
        rules: List<RuleEntity>,
        transactions: List<TransactionEntity>
    ) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("categories", null, null)
            db.delete("merchants", null, null)
            db.delete("rules", null, null)
            db.delete("transactions", null, null)

            for (cat in categories) {
                val cv = ContentValues().apply {
                    put("id", cat.id)
                    put("name", cat.name)
                    put("iconName", cat.iconName)
                    put("colorHex", cat.colorHex)
                    put("isDefault", if (cat.isDefault) 1 else 0)
                }
                db.insertWithOnConflict("categories", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            }

            for (m in merchants) {
                val cv = ContentValues().apply {
                    put("id", m.id)
                    put("displayName", m.displayName)
                    put("normalizedName", m.normalizedName)
                    if (m.defaultCategoryId != null) put("defaultCategoryId", m.defaultCategoryId) else putNull("defaultCategoryId")
                }
                db.insertWithOnConflict("merchants", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            }

            for (r in rules) {
                val cv = ContentValues().apply {
                    put("id", r.id)
                    put("keyword", r.keyword)
                    put("merchantId", r.merchantId)
                    put("categoryId", r.categoryId)
                    put("priority", r.priority)
                    put("createdAtEpoch", r.createdAtEpoch)
                }
                db.insertWithOnConflict("rules", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            }

            for (tx in transactions) {
                val cv = ContentValues().apply {
                    put("id", tx.id)
                    put("dateEpochDay", tx.dateEpochDay)
                    put("yearMonth", tx.yearMonth)
                    put("statementYearMonth", tx.statementYearMonth)
                    put("rawDescription", tx.rawDescription)
                    put("cleanedDescription", tx.cleanedDescription)
                    put("amount", tx.amount)
                    put("signedAmount", tx.signedAmount)
                    put("currency", tx.currency)
                    if (tx.foreignCurrencyNote != null) put("foreignCurrencyNote", tx.foreignCurrencyNote) else putNull("foreignCurrencyNote")
                    if (tx.rewardPointsText != null) put("rewardPointsText", tx.rewardPointsText) else putNull("rewardPointsText")
                    put("type", tx.type.name)
                    if (tx.installmentCurrent != null) put("installmentCurrent", tx.installmentCurrent) else putNull("installmentCurrent")
                    if (tx.installmentTotal != null) put("installmentTotal", tx.installmentTotal) else putNull("installmentTotal")
                    if (tx.cardInfo != null) put("cardInfo", tx.cardInfo) else putNull("cardInfo")
                    if (tx.merchantId != null) put("merchantId", tx.merchantId) else putNull("merchantId")
                    if (tx.categoryId != null) put("categoryId", tx.categoryId) else putNull("categoryId")
                    if (tx.matchedRuleId != null) put("matchedRuleId", tx.matchedRuleId) else putNull("matchedRuleId")
                    put("isApproved", if (tx.isApproved) 1 else 0)
                    put("sourceFileName", tx.sourceFileName)
                    put("dedupHash", tx.dedupHash)
                }
                db.insertWithOnConflict("transactions", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    companion object {
        private const val DATABASE_NAME = "extre_app.db"
        private const val DATABASE_VERSION = 1
    }
}

