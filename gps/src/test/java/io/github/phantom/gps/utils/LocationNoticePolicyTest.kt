package io.github.phantom.gps.utils
import org.junit.Assert.*
import org.junit.Test
class LocationNoticePolicyTest {
 @Test fun idleNeverRequiresAnOngoingCardOrForegroundService(){
  for(candidate in listOf(false,true))for(auto in listOf(false,true)){
   val state=LocationNoticePolicy.state(false,candidate,auto)
   assertFalse(state.ongoing);assertFalse(state.stop);assertEquals(candidate&&!auto,state.start)
   assertEquals(candidate&&!auto,state.visible)
  }
  assertFalse(LocationNoticePolicy.needsActiveService(true,false))
  assertFalse(LocationNoticePolicy.needsActiveService(false,true))
 }
 @Test fun activeKeepsStopAndOnlyManualCandidatesGetStart(){
  for(candidate in listOf(false,true))for(auto in listOf(false,true)){
   val state=LocationNoticePolicy.state(true,candidate,auto)
   assertTrue(state.visible);assertTrue(state.stop);assertTrue(state.ongoing)
   assertEquals(candidate&&!auto,state.start)
  }
  assertTrue(LocationNoticePolicy.needsActiveService(true,true))
 }
}
