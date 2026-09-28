package com.yoyo.privatechat;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import androidx.core.content.FileProvider;
import okhttp3.*;
import org.json.JSONObject;
import java.io.File;

final class UpdateManager {
 private static final String META="https://raw.githubusercontent.com/ahmdmhmdabrahymbdalghnyqndylqn-collab/YoYo/main/update.json";
 private static final Handler MAIN=new Handler(Looper.getMainLooper());

 static void check(Activity a){
  new Thread(()->{
   try(Response r=Api.HTTP.newCall(new Request.Builder().url(META+"?t="+System.currentTimeMillis()).cacheControl(CacheControl.FORCE_NETWORK).build()).execute()){
    if(!r.isSuccessful()||r.body()==null)return;
    JSONObject j=new JSONObject(r.body().string());
    int remote=j.optInt("versionCode",0);
    if(remote<=BuildConfig.VERSION_CODE)return;
    String url=j.optString("apk_url"),name=j.optString("versionName","");
    if(url.isEmpty())return;
    MAIN.post(()->prepare(a,remote,name,url));
   }catch(Exception ignored){}
  },"yoyo-update-check").start();
 }

 private static void prepare(Activity a,int version,String name,String url){
  if(a.isFinishing()||a.isDestroyed())return;
  if(Build.VERSION.SDK_INT>=26&&!a.getPackageManager().canRequestPackageInstalls()){
   if(!Prefs.get(a,"update_permission_asked_"+version).equals("1")){
    Prefs.put(a,"update_permission_asked_"+version,"1");
    Ui.toast(a,"اسمح ليويو بتثبيت التحديثات، وبعدها ارجع للتطبيق");
    Intent s=new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+a.getPackageName()));
    a.startActivity(s);
   }
   return;
  }
  if(Prefs.get(a,"update_downloading").equals(String.valueOf(version)))return;
  Prefs.put(a,"update_downloading",String.valueOf(version));
  Ui.toast(a,"يويو "+name+" قيد التنزيل…");
  File out=new File(a.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),"YOYO-update.apk");
  if(out.exists())out.delete();
  DownloadManager dm=(DownloadManager)a.getSystemService(Context.DOWNLOAD_SERVICE);
  DownloadManager.Request q=new DownloadManager.Request(Uri.parse(url))
    .setTitle("YOYO "+name)
    .setDescription("تنزيل تحديث يويو")
    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
    .setDestinationInExternalFilesDir(a,Environment.DIRECTORY_DOWNLOADS,"YOYO-update.apk");
  long id=dm.enqueue(q);
  poll(a,dm,id,out,version,0);
 }

 private static void poll(Activity a,DownloadManager dm,long id,File out,int version,int tries){
  MAIN.postDelayed(()->{
   if(a.isFinishing()||a.isDestroyed())return;
   try(android.database.Cursor c=dm.query(new DownloadManager.Query().setFilterById(id))){
    if(c!=null&&c.moveToFirst()){
     int status=c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
     if(status==DownloadManager.STATUS_SUCCESSFUL){
      Prefs.put(a,"update_downloading","");
      Uri uri=FileProvider.getUriForFile(a,a.getPackageName()+".files",out);
      Intent i=new Intent(Intent.ACTION_VIEW).setDataAndType(uri,"application/vnd.android.package-archive")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_ACTIVITY_NEW_TASK);
      a.startActivity(i);
      return;
     }
     if(status==DownloadManager.STATUS_FAILED){
      Prefs.put(a,"update_downloading","");
      Ui.toast(a,"تعذر تنزيل التحديث؛ رح نحاول لاحقًا");
      return;
     }
    }
   }catch(Exception e){Prefs.put(a,"update_downloading","");return;}
   if(tries<300)poll(a,dm,id,out,version,tries+1);else Prefs.put(a,"update_downloading","");
  },1000);
 }
}
