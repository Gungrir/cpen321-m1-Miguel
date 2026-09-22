package com.example.cpen321application.m1

import com.example.cpen321application.BuildConfig

object M1Config {
    val API_BASE_URL: String = BuildConfig.API_BASE_URL.trimEnd('/')

    val PIXEL_WS_URL: String = API_BASE_URL
        .replaceFirst("https://", "wss://")
        .replaceFirst("http://", "ws://") + "/pixels"

    val GOOGLE_WEB_CLIENT_ID: String = BuildConfig.GOOGLE_CLIENT_ID.trim()
}