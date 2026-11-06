package com.fulllock.app;
import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;import android.content.ContentValues;import java.io.File;import java.util.*;
/** app_process root helper. Only the known LSPosed schema and this module's row are writable. */
public final class RootFrameworkConfig {
 private static final String MODULE="com.gozza.lalamove",DRIVER="com.lalamove.global.driver.sea";
 private static Set<String> columns(SQLiteDatabase db,String table){Set<String> result=new HashSet<>();try(Cursor c=db.rawQuery("PRAGMA table_info("+table+")",null)){while(c.moveToNext())result.add(c.getString(c.getColumnIndexOrThrow("name")));}return result;}
 static String inspectLegacy(SQLiteDatabase db,File apk)throws Exception{
  try(Cursor module=db.rawQuery("SELECT mid,apk_path,enabled FROM modules WHERE module_pkg_name=?",new String[]{MODULE})){
   if(!module.moveToFirst())return "MODULE_MISSING";long mid=module.getLong(0);if(!apk.getCanonicalPath().equals(module.getString(1)))return "APK_STALE";if(module.getInt(2)!=1)return "MODULE_DISABLED";
   boolean driver=false,system=false;try(Cursor scope=db.rawQuery("SELECT app_pkg_name FROM scope WHERE mid=? AND user_id=0",new String[]{Long.toString(mid)})){while(scope.moveToNext()){String target=scope.getString(0);if(DRIVER.equals(target))driver=true;if("android".equals(target))system=true;}}
   return driver&&system?"CONFIGURED":"SCOPE_MISSING";
  }
 }
 public static void main(String[] args){if(android.os.Process.myUid()!=0||(args.length!=1&&args.length!=2)||(args.length==2&&!args[1].equals("--check"))){System.out.println("UNSUPPORTED");return;}try{
  File apk=new File(args[0]);if(!apk.isFile()||!apk.getCanonicalPath().startsWith("/data/app/")||!apk.getName().equals("base.apk")){System.out.println("UNSUPPORTED");return;}
  File config=new File("/data/adb/lspd/config/modules_config.db");if(!config.isFile()){System.out.println("UNSUPPORTED");return;}
  boolean check=args.length==2&&args[1].equals("--check");
  try(SQLiteDatabase db=SQLiteDatabase.openDatabase(config.getPath(),null,SQLiteDatabase.OPEN_READONLY)){
   Set<String> moduleCols=columns(db,"modules"),scopeCols=columns(db,"scope");
   // Legacy 1.9.2 is inspected read-only, including when invoked without --check.
   if(moduleCols.equals(Set.of("mid","module_pkg_name","apk_path","enabled"))&&scopeCols.equals(Set.of("mid","app_pkg_name","user_id"))&&columns(db,"modules_state").isEmpty()){System.out.println(inspectLegacy(db,apk));return;}
   if(!moduleCols.equals(Set.of("module_pkg_name","apk_path"))||!scopeCols.equals(Set.of("module_pkg_name","app_pkg_name","user_id"))||!columns(db,"modules_state").equals(Set.of("module_pkg_name","user_id","enabled","scope_request_blocked"))){System.out.println("UNSUPPORTED");return;}
   if(check){boolean ready=false;try(Cursor module=db.rawQuery("SELECT m.apk_path,s.enabled FROM modules m JOIN modules_state s ON s.module_pkg_name=m.module_pkg_name WHERE m.module_pkg_name=? AND s.user_id=0",new String[]{MODULE})){ready=module.moveToFirst()&&apk.getCanonicalPath().equals(module.getString(0))&&module.getInt(1)==1;}int targets=0;try(Cursor scope=db.rawQuery("SELECT app_pkg_name FROM scope WHERE module_pkg_name=? AND user_id=0",new String[]{MODULE})){while(scope.moveToNext()){String name=scope.getString(0);if(DRIVER.equals(name)||"android".equals(name))targets++;}}System.out.println(ready&&targets==2?"CONFIGURED":"CONFIG_NOT_READY");return;}
   }
  System.out.println("CONFIG_NOT_READY");
 }catch(Exception e){System.out.println("READ_FAILED");}}
}
