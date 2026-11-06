package com.setotower.downloader.database.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AlreadyExistsItem(
    var downloadItem: DownloadItem,
    var historyID: Long? = null
) : Parcelable
