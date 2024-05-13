package com.apidemo.util

data class LogMessage(
    val ts: Long = System.currentTimeMillis(),
    val url: String,
    val message: String,
    val method: String,
    val type: String = "error",
    val code: Int
)
