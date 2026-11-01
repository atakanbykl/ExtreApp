package com.example.extreapp.data.local.entity

enum class LockTimeout(val title: String, val durationMs: Long) {
    IMMEDIATE("Hemen", 0L),
    ONE_MINUTE("1 Dakika", 60_000L),
    FIVE_MINUTES("5 Dakika", 300_000L);

    companion object {
        fun fromName(name: String?): LockTimeout {
            return entries.firstOrNull { it.name == name } ?: IMMEDIATE
        }
    }
}

enum class BiometricAvailability(val title: String, val isUsable: Boolean) {
    AVAILABLE("Parmak İzi / Yüz Tanıma Aktif", true),
    NOT_ENROLLED("Biyometri Tanımlı Değil (Cihaz Şifresi Geçerli)", true),
    NO_HARDWARE("Biyometrik Donanım Bulunamadı", false),
    UNAVAILABLE("Güvenlik Servisi Kullanılamıyor", false)
}

