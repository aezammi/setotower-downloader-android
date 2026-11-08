package com.setotower.downloader.ui.more.settings

import android.os.Bundle
import android.util.LayoutDirection
import android.view.View
import androidx.core.text.layoutDirection
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.setotower.downloader.R
import com.setotower.downloader.util.LocaleUtil
import com.setotower.downloader.util.SetotowerProductPolicy
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlin.system.exitProcess


class MainSettingsFragment : PreferenceFragmentCompat() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(listView) { v, insets ->
            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )

            v.setPadding(
                v.paddingLeft,
                v.paddingTop,
                v.paddingRight,
                v.paddingBottom + systemBars.bottom
            )

            insets
        }
    }
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        val navController = findNavController()
        val preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val appearance = findPreference<Preference>("appearance")
        val separator = if (LocaleUtil.currentLocale().layoutDirection == LayoutDirection.RTL) "،" else ","
        appearance?.summary = "${getString(R.string.Theme)}$separator ${getString(R.string.accents)}"
        appearance?.setOnPreferenceClickListener {
            navController.navigate(R.id.action_mainSettingsFragment_to_appearanceSettingsFragment)
            true
        }

        val folders = findPreference<Preference>("folders")
        folders?.summary = if (SetotowerProductPolicy.allowCommandDirectory) {
            "${getString(R.string.music_directory)}$separator ${getString(R.string.video_directory)}$separator ${getString(R.string.command_directory)}"
        } else {
            "${getString(R.string.music_directory)}$separator ${getString(R.string.video_directory)}"
        }
        folders?.setOnPreferenceClickListener {
            navController.navigate(R.id.action_mainSettingsFragment_to_folderSettingsFragment)
            true
        }

        val downloading = findPreference<Preference>("downloading")
        downloading?.summary = "${getString(R.string.quick_download)}$separator ${getString(R.string.concurrent_downloads)}"
        downloading?.setOnPreferenceClickListener {
            navController.navigate(R.id.action_mainSettingsFragment_to_downloadSettingsFragment)
            true
        }

        val processing = findPreference<Preference>("processing")
        processing?.summary = "${getString(R.string.sponsorblock)}$separator ${getString(R.string.embed_subtitles)}$separator ${getString(R.string.add_chapters)}"
        processing?.setOnPreferenceClickListener {
            navController.navigate(R.id.action_mainSettingsFragment_to_processingSettingsFragment)
            true
        }

        val advanced = findPreference<Preference>("advanced")
        advanced?.isVisible = SetotowerProductPolicy.allowAdvancedSettings
        advanced?.summary = "PO Token$separator ${getString(R.string.other_youtube_extractor_args)}"
        advanced?.setOnPreferenceClickListener {
            navController.navigate(R.id.action_mainSettingsFragment_to_advancedSettingsFragment)
            true
        }

        findPreference<Preference>("terminate_app")?.setOnPreferenceClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.kill_app)
                .setMessage(R.string.confirm_terminate)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok) { _, _ ->
                    requireActivity().finishAndRemoveTask()
                    requireActivity().finishAffinity()
                    exitProcess(0)
                }
                .show()
            true
        }

    }
}
