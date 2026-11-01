package com.example.extreapp.data.local.entity

import kotlinx.serialization.Serializable

@Serializable
data class MerchantEntity(
    val id: Long = 0,
    val displayName: String,
    val normalizedName: String,
    val defaultCategoryId: Long? = null
)

