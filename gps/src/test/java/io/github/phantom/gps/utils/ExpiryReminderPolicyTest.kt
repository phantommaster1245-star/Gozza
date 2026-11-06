package io.github.phantom.gps.utils
import org.junit.Assert.*
import org.junit.Test
class ExpiryReminderPolicyTest {
 @Test fun respectsThresholdExpirySuppressionAndDailyCap(){
  assertTrue(ExpiryReminderPolicy.shouldShow(86400000,100000000,0,0,false))
  for(r in listOf(0L,-1L,3L*86400000))assertFalse(ExpiryReminderPolicy.shouldShow(r,100000000,0,0,false))
  assertFalse(ExpiryReminderPolicy.shouldShow(1,100000000,3,0,false))
  assertFalse(ExpiryReminderPolicy.shouldShow(1,100000000,0,0,true))
  assertFalse(ExpiryReminderPolicy.shouldShow(1,100000000,1,99999999,false))
  assertTrue(ExpiryReminderPolicy.shouldShow(1,100000000,1,100000000-4L*3600000,false))
 }
}
