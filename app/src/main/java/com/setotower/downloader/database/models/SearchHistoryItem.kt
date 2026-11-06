package com.setotower.downloader.database.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "searchHistory")
data class SearchHistoryItem(
    @PrimaryKey(autoGenerate = true)
    var id: Long,
    val query: String
)