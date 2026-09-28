package com.yoyo.privatechat;
import android.app.Application;import android.content.Context;
public final class YoYoApp extends Application {
 @Override protected void attachBaseContext(Context base){super.attachBaseContext(base);Thread.UncaughtExceptionHandler previous=Thread.getDefaultUncaughtExceptionHandler();Thread.setDefaultUncaughtExceptionHandler((thread,error)->{CrashReport.save(base,error);if(previous!=null)previous.uncaughtException(thread,error);else{android.os.Process.killProcess(android.os.Process.myPid());System.exit(10);}});}
 @Override public void onCreate(){super.onCreate();if(Prefs.hasProfile(this))try{RealtimeService.start(this);}catch(Exception ignored){}}
}
