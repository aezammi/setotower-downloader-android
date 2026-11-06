package com.setotower.downloader.database.models

import com.setotower.downloader.database.enums.DownloadType

data class DownloadSizeMetadata(
    val id: Long,
    val type: DownloadType,
    val format: Format,
    val allFormats: List<Format>,
    val videoPreferences: VideoPreferences
)