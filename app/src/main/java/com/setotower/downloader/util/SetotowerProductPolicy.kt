package com.setotower.downloader.util

/** Customer-facing feature policy for the Setotower-branded client. */
object SetotowerProductPolicy {
    /** Setotower downloads are designed to work without an interactive account login. */
    const val allowInteractiveCookieLogin = false

    /** The customer app does not expose the upstream command-terminal editor. */
    const val allowCommandTemplates = false

    /** Engine/package maintenance is handled by Setotower releases, not customers. */
    const val allowEngineMaintenanceSettings = false

    /** The phone app has no customer-facing terminal command directory. */
    const val allowCommandDirectory = false
}
