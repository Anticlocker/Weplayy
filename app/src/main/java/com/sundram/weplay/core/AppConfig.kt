package com.sundram.weplay.core

import com.sundram.weplay.BuildConfig

object AppConfig {
    val apiBaseUrl: String = runCatching { BuildConfig.API_BASE_URL }.getOrDefault("").trim().trimEnd('/')

    val isApiConfigured: Boolean = apiBaseUrl.isNotBlank()
}
