package com.example.extreapp.data.local.entity

import kotlinx.serialization.Serializable

@Serializable
data class RuleEntity(
    val id: Long = 0,
    val keyword: String,
    val merchantId: Long,
    val categoryId: Long,
    val priority: Int = 10,
    val createdAtEpoch: Long = System.currentTimeMillis()
)

