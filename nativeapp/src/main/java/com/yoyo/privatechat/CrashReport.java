package com.yoyo.privatechat;
import android.content.Context;import android.os.Build;
/** Local-only diagnostics. Exception messages may contain secrets, so are excluded. */
final class CrashReport {
 static String format(Throwable error){StringBuilder b=new StringBuilder("YOYO "+BuildConfig.VERSION_NAME+"\nAndroid "+Build.VERSION.RELEASE+" / API "+Build.VERSION.SDK_INT+"\nDevice: "+Build.MANUFACTURER+" "+Build.MODEL+"\n");for(int depth=0;error!=null&&depth<5;depth++,error=error.getCause()){b.append(error.getClass().getName()).append('\n');StackTraceElement[] frames=error.getStackTrace();for(int i=0;i<Math.min(frames.length,20);i++)b.append(" at ").append(frames[i]).append('\n');}return b.toString();}
 static void save(Context c,Throwable error){try{c.getSharedPreferences("yoyo_diagnostics",0).edit().putString("report",format(error)).commit();}catch(Exception ignored){}}
 static String get(Context c){return c.getSharedPreferences("yoyo_diagnostics",0).getString("report","");}
 static void clear(Context c){c.getSharedPreferences("yoyo_diagnostics",0).edit().remove("report").commit();}
}
