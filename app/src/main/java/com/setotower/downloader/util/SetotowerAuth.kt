package com.setotower.downloader.util

import android.content.Context
import android.content.Intent
import android.net.Uri

/** Downloader-owned account entry point. This must never use Cineb's session. */
object SetotowerAuth {
    private const val LOGIN_URL =
        "https://downloader.setotower.com/auth/login?flow=android"

    fun startLogin(context: Context) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(LOGIN_URL)))
    }
}
