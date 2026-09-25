package com.yoyo.privatechat;

import android.app.DownloadManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;

import androidx.core.app.NotificationCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class UpdateManager {
    private static final String LATEST_API =
            "https://api.github.com/repos/ahmdmhmdabrahymbdalghnyqndylqn-collab/YoYo/releases/latest";
    private static final String P = "yoyo_updater";
    private static final String CHANNEL = "yoyo_updates";
    private static final long CHECK_INTERVAL = 6L * 60L * 60L * 1000L;

    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();

    private UpdateManager() {}

    private static SharedPreferences sp(Context c){
        return c.getSharedPreferences(P, Context.MODE_PRIVATE);
    }

    public static void checkAndDownload(Context c){
        long now=System.currentTimeMillis();
        long last=sp(c).getLong("last_check",0);
        if(now-last<CHECK_INTERVAL) return;
        sp(c).edit().putLong("last_check",now).apply();

        Request req=new Request.Builder()
                .url(LATEST_API)
                .header("User-Agent","YoYo-Android")
                .get()
                .build();

        HTTP.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, java.io.IOException e) {}

            @Override public void onResponse(Call call, Response response) {
                try{
                    if(!response.isSuccessful()) return;
                    String raw=response.body()!=null?response.body().string():"{}";
                    JSONObject o=new JSONObject(raw);
                    String tag=o.optString("tag_name","").replaceFirst("^v","");
                    if(tag.isEmpty() || compareVersions(tag, currentVersion(c))<=0) return;

                    JSONArray assets=o.optJSONArray("assets");
                    String url="";
                    if(assets!=null){
                        for(int i=0;i<assets.length();i++){
                            JSONObject a=assets.optJSONObject(i);
                            if(a!=null && "YoYo.apk".equals(a.optString("name"))){
                                url=a.optString("browser_download_url","");
                                break;
                            }
                        }
                    }
                    if(url.isEmpty()) return;
                    enqueue(c,tag,url);
                }catch(Exception ignored){}
                finally{response.close();}
            }
        });
    }

    private static synchronized void enqueue(Context c,String version,String url){
        SharedPreferences s=sp(c);
        String current=s.getString("downloading_version","");
        if(version.equals(current) || version.equals(s.getString("ready_version",""))) return;

        DownloadManager dm=(DownloadManager)c.getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Request r=new DownloadManager.Request(Uri.parse(url))
                .setTitle("تحديث YoYo "+version)
                .setDescription("جاري تنزيل أحدث نسخة")
                .setMimeType("application/vnd.android.package-archive")
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationInExternalFilesDir(c, Environment.DIRECTORY_DOWNLOADS,"YoYo-"+version+".apk");

        long id=dm.enqueue(r);
        s.edit()
                .putLong("download_id",id)
                .putString("downloading_version",version)
                .remove("ready_version")
                .apply();
    }

    public static void onDownloadComplete(Context c,long id){
        SharedPreferences s=sp(c);
        if(id!=s.getLong("download_id",-1)) return;

        DownloadManager dm=(DownloadManager)c.getSystemService(Context.DOWNLOAD_SERVICE);
        Cursor cur=null;
        try{
            cur=dm.query(new DownloadManager.Query().setFilterById(id));
            if(cur!=null && cur.moveToFirst()){
                int status=cur.getInt(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                if(status==DownloadManager.STATUS_SUCCESSFUL){
                    String version=s.getString("downloading_version","");
                    s.edit()
                            .putString("ready_version",version)
                            .remove("downloading_version")
                            .apply();
                    notifyReady(c,version);
                }else if(status==DownloadManager.STATUS_FAILED){
                    s.edit().remove("downloading_version").remove("download_id").apply();
                }
            }
        }catch(Exception ignored){
        }finally{
            if(cur!=null)cur.close();
        }
    }

    private static void notifyReady(Context c,String version){
        NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT>=26){
            NotificationChannel ch=new NotificationChannel(
                    CHANNEL,
                    "تحديثات YoYo",
                    NotificationManager.IMPORTANCE_HIGH
            );
            ch.setDescription("تنبيه عند جاهزية تحديث جديد");
            nm.createNotificationChannel(ch);
        }

        Intent open=new Intent(c,UpdateInstallActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi=PendingIntent.getActivity(
                c,7701,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder b=new NotificationCompat.Builder(c,CHANNEL)
                .setSmallIcon(R.drawable.ic_heart)
                .setContentTitle("تحديث YoYo جاهز ❤️")
                .setContentText("اضغط لتثبيت النسخة "+version)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("تم تنزيل تحديث YoYo "+version+". اضغط هنا ثم Install لإكمال التحديث."))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pi);

        nm.notify(7701,b.build());
    }

    public static long readyDownloadId(Context c){
        return sp(c).getLong("download_id",-1);
    }

    private static String currentVersion(Context c){
        try{
            android.content.pm.PackageInfo info=c.getPackageManager().getPackageInfo(c.getPackageName(),0);
            return info.versionName==null?"0":info.versionName;
        }catch(Exception e){
            return "0";
        }
    }

    private static int compareVersions(String a,String b){
        String[] aa=a.split("\\.");
        String[] bb=b.split("\\.");
        int n=Math.max(aa.length,bb.length);
        for(int i=0;i<n;i++){
            int x=i<aa.length?num(aa[i]):0;
            int y=i<bb.length?num(bb[i]):0;
            if(x!=y) return Integer.compare(x,y);
        }
        return 0;
    }

    private static int num(String s){
        try{return Integer.parseInt(s.replaceAll("[^0-9]",""));}
        catch(Exception e){return 0;}
    }
}
