package io.github.phantom.gps.utils
import org.junit.Assert.*
import org.junit.Test
class AccessPolicyTest {
 private val id="ab".repeat(32)
 private fun json(enabled:String="true",devices:String="\"$id\"")="""{"schema":1,"enabled":$enabled,"devices":[$devices]}"""
 @Test fun onlyExplicitDeviceIsAllowed(){assertTrue(AccessPolicy.permits(json(),id));assertFalse(AccessPolicy.permits(json(),"cd".repeat(32)));assertFalse(AccessPolicy.permits(json(devices=""),id))}
 @Test fun disabledMissingAndMalformedDeny(){for(raw in listOf(json("false"),"{}","null","",json("\"true\""),json().replace("schema\":1","schema\":2"),json(devices="123")))assertFalse(raw,AccessPolicy.permits(raw,id))}
 private fun lease()=mapOf("tester_access_allowed" to true,"tester_access_issued" to 1000L,"tester_access_until" to (1000L+AccessPolicy.LEASE_MS),"tester_access_boot" to 2)
 @Test fun expiresAndSurvivesNeitherRebootNorClockRollback(){assertTrue(AccessPolicy.fresh(lease(),2000,2));assertFalse(AccessPolicy.fresh(lease(),1000L+AccessPolicy.LEASE_MS,2));assertFalse(AccessPolicy.fresh(lease(),2000,3));assertFalse(AccessPolicy.fresh(lease(),999,2));assertFalse(AccessPolicy.fresh(lease(),2000,-1))}
 @Test fun forgedOrMissingLeaseDenies(){assertFalse(AccessPolicy.fresh(emptyMap<String,Any>(),2000,2));assertFalse(AccessPolicy.fresh(lease()+mapOf("tester_access_allowed" to false),2000,2));assertFalse(AccessPolicy.fresh(lease()+mapOf("tester_access_until" to (1001L+AccessPolicy.LEASE_MS)),2000,2))}
}
