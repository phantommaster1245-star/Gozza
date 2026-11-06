package io.github.phantom.gps.utils

import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException

/** Only successfully admitted requests can supersede earlier work. */
internal class DirectRequestQueue(private val executor: Executor) {
    private var latest = 0L
    @Synchronized fun execute(task: (() -> Boolean) -> Unit) {
        val previous = latest
        val sequence = previous + 1
        latest = sequence
        try {
            executor.execute { task { isLatest(sequence) } }
        } catch (rejected: RejectedExecutionException) {
            // Workers check under the same lock and cannot observe rejected admission.
            latest = previous
            throw rejected
        }
    }
    @Synchronized private fun isLatest(sequence: Long): Boolean = sequence == latest
}
