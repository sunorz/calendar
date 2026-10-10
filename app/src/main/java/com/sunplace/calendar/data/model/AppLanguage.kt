package com.sunplace.calendar.data.model

enum class AppLanguage(val id: String) {
    SYSTEM("system"),
    ZH("zh"),
    EN("en");

    companion object {
        fun fromId(id: String?): AppLanguage {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: SYSTEM
        }
    }
}
