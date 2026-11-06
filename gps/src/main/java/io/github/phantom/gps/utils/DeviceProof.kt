package io.github.phantom.gps.utils

/** Canonical P-256 challenge text, bound to account, action, session and timing. */
object DeviceProof {
    fun message(uid:String,operation:String,challengeId:String,nonce:String,deviceId:String,boot:Int,elapsed:Long,session:String):String {
        require(uid.isNotEmpty() && uid.length<=128 && !uid.contains('\n') && !uid.contains('\r'))
        require(operation=="open" || operation=="renew")
        require(challengeId.matches(Regex("[a-f0-9]{32}")) && nonce.matches(Regex("[A-Za-z0-9_-]{43}")))
        require(deviceId.matches(Regex("[a-f0-9]{64}")) && boot>=0 && elapsed>=0)
        require(session.length<=128 && !session.contains('\n') && !session.contains('\r'))
        return listOf("PHANTOM-DEVICE-PROOF","1",uid,operation,challengeId,nonce,deviceId,boot.toString(),elapsed.toString(),session,"io.github.phantom.gps").joinToString("\n")
    }
}
