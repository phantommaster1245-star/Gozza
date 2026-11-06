package io.github.phantom.gps.utils

object LocationNoticePolicy {
    data class State(val visible:Boolean,val start:Boolean,val stop:Boolean,val ongoing:Boolean)
    fun state(active:Boolean,candidate:Boolean,autoStart:Boolean):State {
        val start=candidate&&!autoStart
        return State(active||start,start,active,active)
    }
    fun needsActiveService(signedIn:Boolean,active:Boolean)=signedIn&&active
}
