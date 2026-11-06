package io.github.phantom.gps.utils
import com.google.gson.JsonParser
/** Pure, fail-closed policy; no network, Android state or credentials. */
object AccessPolicy {
 const val LEASE_MS=90000L
 fun permits(json:String,device:String):Boolean=runCatching {
  require(json.length<=16384 && device.matches(Regex("[a-f0-9]{64}")))
  val root=JsonParser().parse(json).asJsonObject
  require(root.get("schema").asInt==1)
  val enabled=root.getAsJsonPrimitive("enabled");require(enabled.isBoolean)
  val devices=root.getAsJsonArray("devices");require(devices.size()<=200)
  require(devices.all { it.isJsonPrimitive && it.asJsonPrimitive.isString && it.asString.matches(Regex("[a-f0-9]{64}")) })
  enabled.asBoolean && devices.any { it.asString==device }
 }.getOrDefault(false)
 fun fresh(values:Map<String,*>,now:Long,boot:Int):Boolean {
  val issued=values["tester_access_issued"] as? Long ?: return false
  val until=values["tester_access_until"] as? Long ?: return false
  return values["tester_access_allowed"]==true && boot>=0 && values["tester_access_boot"]==boot &&
   issued>=0 && now>=issued && until>issued && until-issued<=LEASE_MS && now<until
 }
}
