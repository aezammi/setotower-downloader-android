package com.setotower.downloader.ui.more.settings.advanced

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.graphics.Typeface
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.setotower.downloader.R
import com.setotower.downloader.ui.adapter.SortableTextItemAdapter
import com.setotower.downloader.ui.more.settings.BaseSettingsFragment
import com.setotower.downloader.ui.more.settings.SettingsRegistry
import com.setotower.downloader.util.SetotowerProductPolicy
import com.setotower.downloader.util.UiUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder


class AdvancedSettingsFragment : BaseSettingsFragment() {
    override val title: Int = R.string.advanced
    @SuppressLint("RestrictedApi")
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val preferenceXMLRes = R.xml.advanced_preferences
        setPreferencesFromResource(preferenceXMLRes, rootKey)
        SettingsRegistry.bindFragment(this, preferenceXMLRes)
        findPreference<Preference>("command_templates_category")?.isVisible =
            SetotowerProductPolicy.allowCommandTemplates

        val prefs = PreferenceManager.getDefaultSharedPreferences(requireActivity())
        val editor = prefs.edit()

        findPreference<Preference>("reset_preferences")?.setOnPreferenceClickListener {
            UiUtil.showGenericConfirmDialog(requireContext(), getString(R.string.reset), getString(R.string.reset_preferences_in_screen)) {
                resetPreferences(editor, preferenceXMLRes)
                requireActivity().recreate()
                findNavController().currentDestination?.id?.apply {
                    findNavController().popBackStack(this,true)
                    findNavController().navigate(this)
                }
            }
            true
        }

    }
}
