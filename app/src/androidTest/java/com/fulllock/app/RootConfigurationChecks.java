package com.fulllock.app;
import android.database.sqlite.SQLiteDatabase;import android.database.Cursor;import java.io.File;
final class RootConfigurationChecks {
 private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 private static int count(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){c.moveToFirst();return c.getInt(0);}}
 static String run()throws Exception{
  File apk=new File("/data/app/com.gozza.lalamove-test/base.apk");
  try(SQLiteDatabase db=SQLiteDatabase.create(null)){
   db.execSQL("CREATE TABLE modules(module_pkg_name TEXT PRIMARY KEY,apk_path TEXT NOT NULL)");
   db.execSQL("CREATE TABLE modules_state(module_pkg_name TEXT,user_id INTEGER,enabled INTEGER,scope_request_blocked INTEGER,PRIMARY KEY(module_pkg_name,user_id))");
   db.execSQL("CREATE TABLE scope(module_pkg_name TEXT,app_pkg_name TEXT,user_id INTEGER,PRIMARY KEY(module_pkg_name,app_pkg_name,user_id))");
   db.execSQL("INSERT INTO modules VALUES('other.module','/keep/base.apk')");db.execSQL("INSERT INTO modules_state VALUES('other.module',0,0,1)");db.execSQL("INSERT INTO scope VALUES('other.module','other.target',0)");
   db.execSQL("INSERT INTO scope VALUES('com.gozza.lalamove','selected.gps.app',0)");db.execSQL("INSERT INTO scope VALUES('com.gozza.lalamove','profile.target',10)");
   check(RootFrameworkConfig.inspectModern(db,apk).equals("MODULE_MISSING"),"missing module diagnosed");
   check(RootFrameworkConfig.configureModern(db,apk).equals("CONFIGURED"),"module configured");
   check(count(db,"SELECT count(*) FROM scope WHERE module_pkg_name='com.gozza.lalamove' AND user_id=0")==5,"four required scopes plus existing GPS selection");
   check(count(db,"SELECT count(*) FROM scope WHERE module_pkg_name='com.gozza.lalamove' AND app_pkg_name='com.google.android.apps.maps' AND user_id=0")==1,"Google Maps added exactly once");
   RootFrameworkConfig.configureModern(db,apk);check(count(db,"SELECT count(*) FROM scope")==7,"idempotent setup");
   check(count(db,"SELECT count(*) FROM modules_state WHERE module_pkg_name='other.module' AND enabled=0 AND scope_request_blocked=1")==1,"other module preserved");
   check(count(db,"SELECT count(*) FROM scope WHERE module_pkg_name='com.gozza.lalamove' AND user_id=10")==1,"other user preserved");
   db.execSQL("UPDATE modules SET apk_path='/stale/base.apk' WHERE module_pkg_name='com.gozza.lalamove'");check(RootFrameworkConfig.inspectModern(db,apk).equals("APK_STALE"),"stale APK diagnosed");
   db.execSQL("DELETE FROM scope WHERE module_pkg_name='com.gozza.lalamove' AND app_pkg_name='android'");
   db.execSQL("CREATE TRIGGER reject_scope BEFORE INSERT ON scope WHEN NEW.app_pkg_name='android' BEGIN SELECT RAISE(ABORT,'test rollback'); END");
   boolean failed=false;try{RootFrameworkConfig.configureModern(db,apk);}catch(Exception expected){failed=true;}check(failed,"failure surfaced");
   check(RootFrameworkConfig.inspectModern(db,apk).equals("APK_STALE"),"transaction rolls back path update");
   check(count(db,"SELECT count(*) FROM scope WHERE module_pkg_name='other.module'")==1,"other scopes retained after rollback");
   return "11 root configuration checks passed on in-memory SQLite; no live framework modified";
  }
 }
}
