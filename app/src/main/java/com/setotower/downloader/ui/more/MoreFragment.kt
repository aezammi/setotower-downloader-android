package com.setotower.downloader.ui.more

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.color.MaterialColors
import com.setotower.downloader.R
import com.setotower.downloader.ui.more.settings.SettingsActivity
import kotlinx.coroutines.launch

class MoreFragment : Fragment() {
    private lateinit var preferences: SharedPreferences
    private lateinit var profileScroll: android.widget.ScrollView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_more, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        profileScroll = view.findViewById(R.id.profile_scroll)

        val loggedIn = preferences.getBoolean("logged_in", false) ||
            !preferences.getString("auth_token", null).isNullOrBlank() ||
            !preferences.getString("session_token", null).isNullOrBlank()

        val displayName = preferences.getString("profile_name", "")?.takeIf { it.isNotBlank() }
            ?: preferences.getString("username", "")?.takeIf { it.isNotBlank() }
            ?: getString(R.string.you)
        view.findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.profile_toolbar).title = displayName
        view.findViewById<TextView>(R.id.profile_name_text).text = displayName
        view.findViewById<TextView>(R.id.profile_handle_text).text =
            preferences.getString("profile_handle", "")?.takeIf { it.isNotBlank() }
                ?: if (displayName == getString(R.string.you)) getString(R.string.profile_handle) else "@$displayName"

        view.findViewById<View>(R.id.settings).setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }
        view.findViewById<View>(R.id.profile_setotower).setOnClickListener {
            openUrl("https://downloader.setotower.com")
        }
        view.findViewById<View>(R.id.profile_discord).setOnClickListener {
            openUrl("https://discord.gg/qgh7CNe7Su")
        }
        val sessionAction = view.findViewById<TextView>(R.id.profile_session_action)
        sessionAction.text = getString(if (loggedIn) R.string.logout else R.string.login)
        sessionAction.setTextColor(
            MaterialColors.getColor(
                sessionAction,
                if (loggedIn) com.google.android.material.R.attr.colorError
                else com.google.android.material.R.attr.colorPrimary
            )
        )
        sessionAction.setOnClickListener {
            if (loggedIn) confirmLogout() else openUrl("https://cineb.setotower.com/auth/login?return=/")
        }
    }

    private fun openUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    private fun confirmLogout() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.logout)
            .setMessage(R.string.logout_confirmation)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.logout) { _, _ ->
                lifecycleScope.launch {
                    // Keep downloads/settings; remove only optional account session keys.
                    preferences.edit()
                        .remove("auth_token")
                        .remove("session_token")
                        .remove("logged_in")
                        .apply()
                    Toast.makeText(requireContext(), R.string.logged_out, Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    fun scrollToTop() {
        if (::profileScroll.isInitialized) profileScroll.smoothScrollTo(0, 0)
    }
}
