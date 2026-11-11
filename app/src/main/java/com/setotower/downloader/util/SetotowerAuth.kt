package com.setotower.downloader.util

import android.content.Context
import android.content.Intent
import com.setotower.downloader.ui.auth.AuthWebViewActivity

/** Downloader-owned account entry point. This must never use Cineb's session. */
object SetotowerAuth {
    private const val LOGIN_URL =
        "https://downloader.setotower.com/auth/login?flow=android"

    fun startLogin(context: Context) {
        context.startActivity(
            Intent(context, AuthWebViewActivity::class.java)
                .putExtra(AuthWebViewActivity.EXTRA_URL, LOGIN_URL)
        )
    }
}
