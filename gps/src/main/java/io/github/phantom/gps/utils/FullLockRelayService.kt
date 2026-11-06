package io.github.phantom.gps.utils

import android.app.Service
import android.content.Intent
import android.os.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.*

/** Bootstrap is Full Lock UID only. A private, revocable capability permits the root relay. */
class FullLockRelayService : Service() {
    companion object {
        const val DESCRIPTOR = "io.github.phantom.gps.FullLockRelay.v1"
        const val SESSION = "io.github.phantom.gps.LocationSession.v1"
        const val OPEN = IBinder.FIRST_CALL_TRANSACTION
        const val SUBMIT = IBinder.FIRST_CALL_TRANSACTION
        const val KEEP_ALIVE = IBinder.FIRST_CALL_TRANSACTION + 1
        const val CLOSE = IBinder.FIRST_CALL_TRANSACTION + 2
        const val LEASE_MS = 15000L
    }
    private var active: Session? = null
    private val maintenanceScope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
    private var maintenance:Job?=null
    private fun maintain(session:Session) {
        maintenance?.cancel()
        maintenance=maintenanceScope.launch {
            while(isActive&&session.valid()) {
                if(!GpsAccess.available(applicationContext)){session.close();return@launch}
                repeat(24) { delay(5000);if(!session.valid())return@launch }
            }
        }
    }
    private fun ownerUid(): Int = packageManager.getPackageUid(DirectLocationProtocol.CALLER_PACKAGE, 0)
    private fun requireOwner(): Int {
        val uid = Binder.getCallingUid()
        if (uid != ownerUid() || packageManager.getPackagesForUid(uid)?.toSet() != setOf(DirectLocationProtocol.CALLER_PACKAGE))
            throw SecurityException("Gozza Lalamove caller required")
        return uid
    }
    private val gateway = object : Binder() {
        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            if (code == INTERFACE_TRANSACTION) { reply?.writeString(DESCRIPTOR); return true }
            if (code != OPEN) return super.onTransact(code, data, reply, flags)
            data.enforceInterface(DESCRIPTOR)
            val uid = requireOwner()
            if (data.readInt() != 1) throw IllegalArgumentException("Unsupported bridge version")
            val lifetime = data.readStrongBinder() ?: throw IllegalArgumentException("Owner lifetime required")
            if (data.dataAvail() != 0 || reply == null) throw IllegalArgumentException("Invalid bootstrap")
            val session = Session(uid, lifetime)
            synchronized(this@FullLockRelayService) {
                active?.close()
                active = session
                maintain(session)
            }
            reply.writeNoException()
            reply.writeStrongBinder(session)
            return true
        }
    }
    private inner class Session(val owner: Int, val lifetime: IBinder) : Binder(), IBinder.DeathRecipient {
        private val live = AtomicBoolean(true)
        private val lease = RelayLease({ SystemClock.elapsedRealtime() }, LEASE_MS)
        init { lifetime.linkToDeath(this, 0) }
        fun valid(): Boolean = live.get() && lifetime.isBinderAlive &&
            lease.valid() && runCatching { ownerUid() == owner }.getOrDefault(false)
        fun close() { lease.revoke(); if (live.getAndSet(false)) runCatching { lifetime.unlinkToDeath(this, 0) } }
        override fun binderDied() { close() }
        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            if (code == INTERFACE_TRANSACTION) { reply?.writeString(SESSION); return true }
            data.enforceInterface(SESSION)
            when (code) {
                KEEP_ALIVE -> {
                    if (requireOwner() != owner || !valid()) throw SecurityException("Session expired")
                    if (!lease.renew()) throw SecurityException("Session expired")
                }
                CLOSE -> { if (requireOwner() != owner) throw SecurityException("Session owner required"); close() }
                SUBMIT -> {
                    // UID 0 alone grants nothing: this private Binder must first be issued by Full Lock.
                    if (Binder.getCallingUid() != 0 || !valid()) throw SecurityException("Relay session unavailable")
                    if (data.dataSize() > 8192) throw IllegalArgumentException("Oversized location request")
                    val version = data.readInt()
                    val id = data.readString().orEmpty()
                    val lat = data.readDouble()
                    val lng = data.readDouble()
                    val label = data.readString().orEmpty()
                    val target = data.readString().orEmpty()
                    val callback = ResultReceiver.CREATOR.createFromParcel(data)
                    if (data.dataAvail() != 0) throw IllegalArgumentException("Unexpected request data")
                    val request = Intent().putExtra("api_version", version).putExtra("request_id", id)
                        .putExtra("latitude", lat).putExtra("longitude", lng)
                        .putExtra("label", label).putExtra("target_package", target)
                    // No disk, license work or Hook wait on the Driver/Binder calling thread.
                    DirectLocationEngine.submit(applicationContext, request, callback, allowed = { valid() })
                }
                else -> return super.onTransact(code, data, reply, flags)
            }
            reply?.writeNoException()
            return true
        }
    }
    override fun onBind(intent: Intent?): IBinder = gateway
    override fun onUnbind(intent: Intent?): Boolean {
        maintenance?.cancel()
        synchronized(this) { active?.close(); active = null }
        return false
    }
    override fun onDestroy() {
        maintenanceScope.cancel()
        synchronized(this) { active?.close(); active = null }
        super.onDestroy()
    }
}
