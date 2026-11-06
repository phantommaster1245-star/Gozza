package com.fulllock.transport;

import android.os.Bundle;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;

/** Bounded, primitive-only local messages. No Java object deserialization. */
public final class LocalFrames {
 public static final int MAX=65536;
 private LocalFrames(){}
 public static JSONArray encode(Bundle bundle)throws Exception{
  JSONArray out=new JSONArray();if(bundle==null)return out;if(bundle.size()>64)throw new IOException("Too many fields");
  for(String key:bundle.keySet()){if(key.length()>128)throw new IOException("Invalid key");Object v=bundle.get(key);String type;
   if(v==null)continue;else if(v instanceof String)type="s";else if(v instanceof Boolean)type="b";else if(v instanceof Integer)type="i";else if(v instanceof Long)type="l";else if(v instanceof ArrayList<?> list&&list.stream().allMatch(x->x instanceof String))type="a";else throw new IOException("Unsupported field");
   out.put(new JSONArray().put(key).put(type).put(type.equals("a")?new JSONArray((Collection<?>)v):v));
  }return out;
 }
 public static Bundle decode(JSONArray fields)throws Exception{
  if(fields==null||fields.length()>64)throw new IOException("Invalid fields");Bundle out=new Bundle();
  for(int i=0;i<fields.length();i++){JSONArray f=fields.getJSONArray(i);String key=f.getString(0);if(key.length()>128||out.containsKey(key))throw new IOException("Invalid key");switch(f.getString(1)){
   case "s" -> out.putString(key,f.getString(2));case "b" -> out.putBoolean(key,f.getBoolean(2));case "i" -> out.putInt(key,f.getInt(2));case "l" -> out.putLong(key,f.getLong(2));
   case "a" -> {JSONArray a=f.getJSONArray(2);if(a.length()>128)throw new IOException("Array too large");ArrayList<String> list=new ArrayList<>();for(int n=0;n<a.length();n++)list.add(a.getString(n));out.putStringArrayList(key,list);}
   default -> throw new IOException("Unsupported type");
  }}return out;
 }
}
