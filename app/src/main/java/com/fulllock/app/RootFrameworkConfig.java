package com.fulllock.app;
import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;import android.content.ContentValues;import java.io.File;import java.util.*;
/** Root-only, schema-checked setup for this unified module. Other modules and scopes are preserved. */
public final class RootFrameworkConfig {
 static final String MODULE="com.gozza.lalamove",DRIVER="com.lalamove.global.driver.sea",MAPS="com.google.android.apps.maps";
 static final Set<String> REQUIRED=Set.of(MODULE,DRIVER,"android",MAPS);
 private static Set<String> columns(SQLiteDatabase db,String table){Set<String> result=new HashSet<>();try(Cursor c=db.rawQuery("PRAGMA table_info("+table+")",null)){while(c.moveToNext())result.add(c.getString(c.getColumnIndexOrThrow("name")));}return result;}
 static String inspectLegacy(SQLiteDatabase db,File apk)throws Exception {
  try(Cursor module=db.rawQuery("SELECT mid,apk_path,enabled FROM modules WHERE module_pkg_name=?",new String[]{MODULE})){
   if(!module.moveToFirst())return "MODULE_MISSING";long mid=module.getLong(0);if(!apk.getCanonicalPath().equals(module.getString(1)))return "APK_STALE";if(module.getInt(2)!=1)return "MODULE_DISABLED";
   Set<String> targets=new HashSet<>();try(Cursor scope=db.rawQuery("SELECT app_pkg_name FROM scope WHERE mid=? AND user_id=0",new String[]{Long.toString(mid)})){while(scope.moveToNext())targets.add(scope.getString(0));}
   return targets.containsAll(REQUIRED)?"CONFIGURED":"SCOPE_MISSING";
  }
 }
 static String inspectModern(SQLiteDatabase db,File apk)throws Exception {
  try(Cursor module=db.rawQuery("SELECT m.apk_path,s.enabled FROM modules m LEFT JOIN modules_state s ON s.module_pkg_name=m.module_pkg_name AND s.user_id=0 WHERE m.module_pkg_name=?",new String[]{MODULE})){
   if(!module.moveToFirst())return "MODULE_MISSING";
   if(!apk.getCanonicalPath().equals(module.getString(0)))return "APK_STALE";
   if(module.isNull(1)||module.getInt(1)!=1)return "MODULE_DISABLED";
  }
  Set<String> targets=new HashSet<>();try(Cursor scope=db.rawQuery("SELECT app_pkg_name FROM scope WHERE module_pkg_name=? AND user_id=0",new String[]{MODULE})){while(scope.moveToNext())targets.add(scope.getString(0));}
  return targets.containsAll(REQUIRED)?"CONFIGURED":"SCOPE_MISSING";
 }
 static String configureModern(SQLiteDatabase db,File apk)throws Exception {
  db.beginTransaction();try{
   ContentValues module=new ContentValues();module.put("module_pkg_name",MODULE);module.put("apk_path",apk.getCanonicalPath());
   if(db.update("modules",module,"module_pkg_name=?",new String[]{MODULE})==0)db.insertOrThrow("modules",null,module);
   ContentValues state=new ContentValues();state.put("enabled",1);
   if(db.update("modules_state",state,"module_pkg_name=? AND user_id=0",new String[]{MODULE})==0){state.put("module_pkg_name",MODULE);state.put("user_id",0);state.put("scope_request_blocked",0);db.insertOrThrow("modules_state",null,state);}
   Set<String> current=new HashSet<>();try(Cursor scope=db.rawQuery("SELECT app_pkg_name FROM scope WHERE module_pkg_name=? AND user_id=0",new String[]{MODULE})){while(scope.moveToNext())current.add(scope.getString(0));}
   for(String target:REQUIRED)if(!current.contains(target)){ContentValues scope=new ContentValues();scope.put("module_pkg_name",MODULE);scope.put("app_pkg_name",target);scope.put("user_id",0);db.insertOrThrow("scope",null,scope);}
   String verified=inspectModern(db,apk);if(!verified.equals("CONFIGURED"))throw new IllegalStateException(verified);
   db.setTransactionSuccessful();return verified;
  }finally{db.endTransaction();}
 }
 public static void main(String[] args){
  if(android.os.Process.myUid()!=0||args.length<1||args.length>2||(args.length==2&&!Set.of("--check","--inspect").contains(args[1]))){System.out.println("UNSUPPORTED");return;}
  boolean readOnly=args.length==2;
  try{
   File apk=new File(args[0]);String path=apk.getCanonicalPath();if(!apk.isFile()||!path.startsWith("/data/app/")||!path.contains("/"+MODULE+"-")||!apk.getName().equals("base.apk")){System.out.println("UNSUPPORTED");return;}
   File config=new File("/data/adb/lspd/config/modules_config.db");if(!config.isFile()){System.out.println("UNSUPPORTED");return;}
   try(SQLiteDatabase db=SQLiteDatabase.openDatabase(config.getPath(),null,SQLiteDatabase.OPEN_READONLY)){
    Set<String> moduleCols=columns(db,"modules"),scopeCols=columns(db,"scope");
    if(moduleCols.equals(Set.of("mid","module_pkg_name","apk_path","enabled"))&&scopeCols.equals(Set.of("mid","app_pkg_name","user_id"))&&columns(db,"modules_state").isEmpty()){System.out.println(inspectLegacy(db,apk));return;}
    if(!moduleCols.equals(Set.of("module_pkg_name","apk_path"))||!scopeCols.equals(Set.of("module_pkg_name","app_pkg_name","user_id"))||!columns(db,"modules_state").equals(Set.of("module_pkg_name","user_id","enabled","scope_request_blocked"))){System.out.println("UNSUPPORTED");return;}
    String state=inspectModern(db,apk);if(readOnly||state.equals("CONFIGURED")){System.out.println(state);return;}
   }
   try(SQLiteDatabase db=SQLiteDatabase.openDatabase(config.getPath(),null,SQLiteDatabase.OPEN_READWRITE)){
    // SQLite creates a consistent snapshot before any configuration changes.
    String backup=new File(config.getParentFile(),"gozza-backup-"+System.currentTimeMillis()+".db").getPath();
    db.execSQL("VACUUM INTO ?",new Object[]{backup});
    System.out.println(configureModern(db,apk));
   }
  }catch(Exception e){System.out.println(readOnly?"READ_FAILED":"WRITE_FAILED");}
 }
}
