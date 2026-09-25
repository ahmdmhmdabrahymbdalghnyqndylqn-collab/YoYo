package com.yoyo.privatechat;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class UpdateDownloadReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if(DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())){
            long id=intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID,-1);
            if(id!=-1) UpdateManager.onDownloadComplete(context,id);
        }
    }
}
