package io.github.phantom.gps.utils

import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException
import org.junit.Assert.*
import org.junit.Test

class DirectRequestQueueTest {
    private class ManualExecutor : Executor {
        val pending = ArrayList<Runnable>()
        var reject = false
        override fun execute(command: Runnable) {
            if (reject) throw RejectedExecutionException()
            pending.add(command)
        }
    }
    @Test fun fullQueueDoesNotSupersedeAcceptedRequest() {
        val executor = ManualExecutor()
        val queue = DirectRequestQueue(executor)
        var acceptedIsLatest = false
        queue.execute { acceptedIsLatest = it() }
        executor.reject = true
        try { queue.execute { fail("Rejected task ran") }; fail("Expected rejection") }
        catch (_: RejectedExecutionException) { }
        executor.pending.single().run()
        assertTrue(acceptedIsLatest)
    }
    @Test fun newestAcceptedRequestSupersedesOlderWorkAfterRejection() {
        val executor = ManualExecutor()
        val queue = DirectRequestQueue(executor)
        val results = ArrayList<Boolean>()
        queue.execute { results.add(it()) }
        executor.reject = true
        repeat(3) { try { queue.execute { fail("Rejected task ran") } } catch (_: RejectedExecutionException) { } }
        executor.reject = false
        queue.execute { results.add(it()) }
        executor.pending.forEach { it.run() }
        assertEquals(listOf(false, true), results)
    }
}
