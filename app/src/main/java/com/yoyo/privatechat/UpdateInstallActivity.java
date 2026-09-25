package com.yoyo.privatechat;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

public class UpdateInstallActivity extends Activity {
    private boolean requestedPermission=false;

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        installOrRequestPermission();
    }

    @Override protected void onResume(){
        super.onResume();
        if(requestedPermission && (Build.VERSION.SDK_INT<26 || getPackageManager().canRequestPackageInstalls())){
            requestedPermission=false;
            launchInstaller();
        }
    }

    private void installOrRequestPermission(){
        if(Build.VERSION.SDK_INT>=26 && !getPackageManager().canRequestPackageInstalls()){
            requestedPermission=true;
            Intent i=new Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:"+getPackageName())
            );
            startActivity(i);
            return;
        }
        launchInstaller();
    }

    private void launchInstaller(){
        long id=UpdateManager.readyDownloadId(this);
        if(id<0){
            Toast.makeText(this,"ما في تحديث جاهز الآن",Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        DownloadManager dm=(DownloadManager)getSystemService(DOWNLOAD_SERVICE);
        Uri uri=dm.getUriForDownloadedFile(id);
        if(uri==null){
            Toast.makeText(this,"تعذر فتح ملف التحديث",Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Intent install=new Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri,"application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(install);
        finish();
    }
}
