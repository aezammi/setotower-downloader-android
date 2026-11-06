package com.setotower.downloader.ui.more.settings.folder.temporary

import android.os.Bundle
import com.setotower.downloader.R
import com.setotower.downloader.ui.more.settings.BaseSettingsFragment
import com.setotower.downloader.ui.more.settings.SettingsRegistry


class TemporaryFilesFragment : BaseSettingsFragment() {
    override val title: Int = R.string.temporary_files

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val preferenceXMLRes = R.xml.temporary_files_preferences
        setPreferencesFromResource(preferenceXMLRes, rootKey)
        SettingsRegistry.bindFragment(this, preferenceXMLRes)
    }
}
