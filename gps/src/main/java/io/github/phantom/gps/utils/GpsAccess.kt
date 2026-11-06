package io.github.phantom.gps.utils
import android.content.Context
/** Tester build: app integrity plus a short-lived repo authorization; no account login. */
object GpsAccess { fun available(context: Context): Boolean = AppIntegrity.isSelfValid(context) && TesterAccess.available(context) }
