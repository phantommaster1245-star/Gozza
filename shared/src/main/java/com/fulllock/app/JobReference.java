package com.fulllock.app;
public final class JobReference {
 public static String hash(String uuid){if(uuid==null||uuid.isBlank()||uuid.length()>128)return "";try{byte[] bytes=java.security.MessageDigest.getInstance("SHA-256").digest(uuid.getBytes(java.nio.charset.StandardCharsets.UTF_8));char[] chars=new char[24],digits="0123456789abcdef".toCharArray();for(int i=0;i<12;i++){chars[i*2]=digits[(bytes[i]&255)>>>4];chars[i*2+1]=digits[bytes[i]&15];}return new String(chars);}catch(Exception e){return "";}}
}
