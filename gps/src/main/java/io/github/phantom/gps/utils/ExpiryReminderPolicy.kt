package io.github.phantom.gps.utils

object ExpiryReminderPolicy {
    fun shouldShow(remaining:Long, now:Long, count:Int, lastShown:Long, suppressed:Boolean):Boolean =
        remaining in 1 until 3L*86400000 && !suppressed && count < 3 && (lastShown == 0L || now-lastShown >= 4L*3600000)
}
