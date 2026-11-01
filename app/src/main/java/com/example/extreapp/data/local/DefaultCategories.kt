package com.example.extreapp.data.local

import com.example.extreapp.data.local.entity.CategoryEntity

object DefaultCategories {
    val INITIAL_CATEGORIES = listOf(
        CategoryEntity(id = 1, name = "Market & Gıda", iconName = "ShoppingCart", colorHex = 0xFF10B981, isDefault = true),
        CategoryEntity(id = 2, name = "Restoran & Kafe", iconName = "Restaurant", colorHex = 0xFFF59E0B, isDefault = true),
        CategoryEntity(id = 3, name = "Akaryakıt & Otomotiv", iconName = "LocalGasStation", colorHex = 0xFF3B82F6, isDefault = true),
        CategoryEntity(id = 4, name = "E-Ticaret & Alışveriş", iconName = "ShoppingBag", colorHex = 0xFF8B5CF6, isDefault = true),
        CategoryEntity(id = 5, name = "Ulaşım & Seyahat", iconName = "Flight", colorHex = 0xFF06B6D4, isDefault = true),
        CategoryEntity(id = 6, name = "Dijital Abonelik & Oyun", iconName = "Subscriptions", colorHex = 0xFFEC4899, isDefault = true),
        CategoryEntity(id = 7, name = "Ev & Dekorasyon", iconName = "Home", colorHex = 0xFF14B8A6, isDefault = true),
        CategoryEntity(id = 8, name = "Spor & Sağlık", iconName = "FitnessCenter", colorHex = 0xFFEF4444, isDefault = true),
        CategoryEntity(id = 9, name = "Eğlence & Gece Hayatı", iconName = "Nightlife", colorHex = 0xFFA855F7, isDefault = true),
        CategoryEntity(id = 10, name = "Faturalar & Kamu", iconName = "ReceiptLong", colorHex = 0xFF6366F1, isDefault = true),
        CategoryEntity(id = 11, name = "Diğer", iconName = "Category", colorHex = 0xFF64748B, isDefault = true)
    )
}

