package com.setotower.downloader.ui.more.settings.general

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.PowerManager
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import com.setotower.downloader.R
import com.setotower.downloader.ui.more.settings.BaseSettingsFragment
import com.setotower.downloader.ui.more.settings.SettingsRegistry
import com.setotower.downloader.util.ThemeUtil
import com.setotower.downloader.util.UiUtil

class GeneralSettingsFragment : BaseSettingsFragment() {
    override val title: Int = R.string.general
    private lateinit var preferences: SharedPreferences
    @SuppressLint("BatteryLife")
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val preferenceXMLRes = R.xml.general_preferences
        setPreferencesFromResource(preferenceXMLRes, rootKey)
        SettingsRegistry.bindFragment(this, preferenceXMLRes)
        preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val editor = preferences.edit()

        findPreference<Preference>("reset_preferences")?.setOnPreferenceClickListener {
            UiUtil.showGenericConfirmDialog(requireContext(), getString(R.string.reset), getString(R.string.reset_preferences_in_screen)) {
                resetPreferences(editor, preferenceXMLRes)
                ThemeUtil.updateThemes()
                findNavController().currentDestination?.id?.apply {
                    findNavController().popBackStack(this,true)
                    findNavController().navigate(this)
                }
            }
            true
        }
    }

    override fun onResume() {
        val packageName: String = requireContext().packageName
        val pm = requireContext().applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (pm.isIgnoringBatteryOptimizations(packageName)) {
            findPreference<Preference>("ignore_battery")?.isVisible = false
        }
        super.onResume()
    }
}