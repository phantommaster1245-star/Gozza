package io.github.phantom.gps.utils
import org.junit.Assert.*
import org.junit.Test
class DeviceProofTest {
 @Test fun canonicalMessageHasFixedOrderAndExactNumbers(){
  val message=DeviceProof.message("uid","renew","ab".repeat(16),"N".repeat(43),"cd".repeat(32),5,123456789L,"session")
  assertEquals(listOf("PHANTOM-DEVICE-PROOF","1","uid","renew","ab".repeat(16),"N".repeat(43),"cd".repeat(32),"5","123456789","session","io.github.phantom.gps").joinToString("\n"),message)
 }
 @Test fun rejectsLineInjectionAndUnsupportedActions(){
  for(pair in listOf("bad\nuid" to "open","uid" to "other")){
   try{DeviceProof.message(pair.first,pair.second,"ab".repeat(16),"N".repeat(43),"cd".repeat(32),5,1L,"");fail("Accepted invalid proof message") }catch(_:IllegalArgumentException){}
  }
 }
}
