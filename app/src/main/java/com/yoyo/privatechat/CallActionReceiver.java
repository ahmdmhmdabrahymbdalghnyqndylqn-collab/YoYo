package com.yoyo.privatechat;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.json.JSONObject;

import java.util.UUID;

public class CallActionReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        String action=intent.getAction();
        if(!"com.yoyo.privatechat.REJECT_CALL".equals(action))return;

        String phone=intent.getStringExtra("phone");
        String callId=intent.getStringExtra("callId");

        ((NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE))
                .cancel(9001);

        try{
            JSONObject o=new JSONObject();
            o.put("type","call_hangup");
            o.put("id",UUID.randomUUID().toString());
            o.put("callId",callId==null?"":callId);
            o.put("from",Prefs.phone(context));
            o.put("name",Prefs.name(context));
            o.put("to",phone==null?"":phone);
            o.put("ts",System.currentTimeMillis());

            RelayClient.publish(o.toString(),null);
        }catch(Exception ignored){}
    }
}
