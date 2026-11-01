package com.example.extreapp.data.local.entity

import kotlinx.serialization.Serializable

@Serializable
data class CategoryEntity(
    val id: Long = 0,
    val name: String,
    val iconName: String,
    val colorHex: Long,
    val isDefault: Boolean = false
)

