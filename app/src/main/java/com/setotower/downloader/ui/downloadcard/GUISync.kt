package com.setotower.downloader.ui.downloadcard

import com.setotower.downloader.database.models.ResultItem

interface GUISync {
    fun updateTitleAuthor(t: String, a: String)
    fun updateUI(res: ResultItem?)
}