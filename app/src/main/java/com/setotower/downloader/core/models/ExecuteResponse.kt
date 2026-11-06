package com.setotower.downloader.core.models

class ExecuteResponse(
    val command: List<String?>,
    val exitCode: Int,
    val elapsedTime: Long,
    val out: String,
    val err: String
)