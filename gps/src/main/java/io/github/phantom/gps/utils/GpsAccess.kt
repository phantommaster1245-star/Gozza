package io.github.phantom.gps.utils
import android.content.Context
/** Private tester build: local app integrity, no account or server entitlement. */
object GpsAccess { fun available(context: Context): Boolean = AppIntegrity.isSelfValid(context) }
