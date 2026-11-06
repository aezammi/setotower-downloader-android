package com.setotower.downloader.database.models

import androidx.preference.Preference
import com.setotower.downloader.ui.more.settings.SettingModule

data class SearchSettingsItem(
    val preference: Preference,
    val xmlId: Int,
    val module: SettingModule?,
    val groupTitle: String? = null,
    val isHeader: Boolean = false,
    var canRebind: Boolean = true
)