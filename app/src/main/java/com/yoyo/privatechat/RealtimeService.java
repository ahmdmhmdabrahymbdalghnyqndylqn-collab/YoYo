package com.yoyo.privatechat;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import java.util.List;
import java.util.UUID;

import okhttp3.WebSocket;

public class RealtimeService extends Service {
    public static final String ACTION_EVENT = "com.yoyo.privatechat.EVENT";
    public static final String EXTRA_JSON = "json";
    private final Handler h = new Handler(Looper.getMainLooper());
    private WebSocket ws;
    private boolean stopping=false;

    @Override public void onCreate(){
        super.onCreate();
        channels();
        startForeground(10, new NotificationCompat.Builder(this,"yoyo_service")
                .setSmallIcon(com.yoyo.privatechat.R.drawable.ic_heart)
                .setContentTitle("YoYo")
                .setContentText("متصل للمحادثات والمكالمات")
                .setOngoing(true).setPriority(NotificationCompat.PRIORITY_MIN).build());
        connect();
        h.post(presenceLoop);
        h.postDelayed(retryLoop, 15000);
    }

    private void connect(){
        if(stopping) return;
        if(ws!=null){ try{ws.cancel();}catch(Exception ignored){} }
        ws = RelayClient.subscribe(new RelayClient.Listener() {
            @Override public void onEvent(String json) { h.post(() -> handle(json)); }
            @Override public void onState(boolean connected) {
                if(connected){
                    h.postDelayed(() -> retryOutstanding(), 1200);
                } else if(!stopping) {
                    h.postDelayed(() -> connect(), 3500);
                }
            }
        });
    }

    private final Runnable presenceLoop = new Runnable() {
        @Override public void run() {
            sendPresence();
            if(!stopping) h.postDelayed(this, 60000);
        }
    };

    private final Runnable retryLoop = new Runnable() {
        @Override public void run() {
            retryOutstanding();
            if(!stopping) h.postDelayed(this, 4L * 60L * 60L * 1000L);
        }
    };

    private void sendPresence(){
        if(!Prefs.hasProfile(this)) return;
        try{
            JSONObject o=new JSONObject();
            o.put("type","presence");
            o.put("id",UUID.randomUUID().toString());
            o.put("from",Prefs.phone(this));
            o.put("name",Prefs.name(this));
            o.put("avatar",Prefs.avatar(this));
            o.put("to","all");
            o.put("ts",System.currentTimeMillis());
            RelayClient.publish(o.toString(),null);
        }catch(Exception ignored){}
    }

    private void retryOutstanding(){
        if(!Prefs.hasProfile(this)) return;
        List<EventStore.Msg> pending=EventStore.outstanding(this);
        for(EventStore.Msg m:pending){
            try{
                JSONObject o=new JSONObject();
                o.put("type","chat");
                o.put("id",m.id);
                o.put("from",m.from);
                o.put("name",Prefs.name(this));
                o.put("avatar",Prefs.avatar(this));
                o.put("to",m.to);
                o.put("text",m.text);
                o.put("ts",m.ts);
                RelayClient.publish(o.toString(), ok -> {
                    if(ok) EventStore.markSent(this,m.to,m.id);
                });
            }catch(Exception ignored){}
        }
    }

    private void sendAck(String to,String msgId){
        try{
            JSONObject o=new JSONObject();
            o.put("type","ack");
            o.put("id",UUID.randomUUID().toString());
            o.put("msgId",msgId);
            o.put("from",Prefs.phone(this));
            o.put("name",Prefs.name(this));
            o.put("avatar",Prefs.avatar(this));
            o.put("to",to);
            o.put("ts",System.currentTimeMillis());
            RelayClient.publish(o.toString(),null);
        }catch(Exception ignored){}
    }

    private void handle(String json){
        try{
            JSONObject o=new JSONObject(json);
            String type=o.optString("type");
            String from=o.optString("from");
            String to=o.optString("to","all");

            if(from.equals(Prefs.phone(this))) return;
            if(!"all".equals(to) && !Prefs.phone(this).equals(to)) return;

            String name=o.optString("name", from);
            String avatar=o.optString("avatar","");
            long ts=o.optLong("ts",System.currentTimeMillis());

            if(!from.isEmpty()) EventStore.upsertContact(this,new EventStore.Contact(from,name,avatar,ts));

            if("chat".equals(type)){
                String msgId=o.optString("id");
                boolean fresh=EventStore.addMessage(this,new EventStore.Msg(msgId,from,to,o.optString("text"),ts,"received"));
                sendAck(from,msgId);
                broadcast(json);
                if(fresh) showMessage(from,name,o.optString("text"));
            } else if("ack".equals(type)) {
                String msgId=o.optString("msgId");
                if(!msgId.isEmpty()) EventStore.markDelivered(this,from,msgId);
                broadcast(json);
            } else if("presence".equals(type)) {
                broadcast(json);
            } else if("call_offer".equals(type)) {
                if(System.currentTimeMillis()-ts < 120000L){
                    broadcast(json);
                    showIncomingCall(o);
                }
            } else if(type.startsWith("call_") || "ice".equals(type)) {
                if(System.currentTimeMillis()-ts < 120000L) broadcast(json);
            }
        }catch(Exception ignored){}
    }

    private void broadcast(String json){
        Intent i=new Intent(ACTION_EVENT).setPackage(getPackageName()).putExtra(EXTRA_JSON,json);
        sendBroadcast(i);
    }

    private void showMessage(String phone,String name,String text){
        Intent open=new Intent(this,ChatActivity.class)
                .putExtra("phone",phone)
                .putExtra("name",name)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi=PendingIntent.getActivity(this,phone.hashCode(),open,
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder b=new NotificationCompat.Builder(this,"yoyo_messages")
                .setSmallIcon(R.drawable.ic_heart)
                .setContentTitle(name)
                .setContentText(text)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL);
        ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(phone.hashCode(),b.build());
    }

    private void showIncomingCall(JSONObject o){
        String phone=o.optString("from");
        String name=o.optString("name",phone);
        String callId=o.optString("callId");
        String sdp=o.optString("sdp");
        Intent open=new Intent(this,CallActivity.class)
                .putExtra("mode","incoming")
                .putExtra("phone",phone)
                .putExtra("name",name)
                .putExtra("callId",callId)
                .putExtra("sdp",sdp)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi=PendingIntent.getActivity(this,("call"+callId).hashCode(),open,
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder b=new NotificationCompat.Builder(this,"yoyo_calls")
                .setSmallIcon(R.drawable.ic_heart)
                .setContentTitle("مكالمة YoYo واردة")
                .setContentText(name+" يتصل بك")
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setOngoing(true)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .addAction(0,"رد",pi)
                .setDefaults(NotificationCompat.DEFAULT_ALL);
        ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(9001,b.build());
    }

    private void channels(){
        if(Build.VERSION.SDK_INT>=26){
            NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel s=new NotificationChannel("yoyo_service","اتصال YoYo",NotificationManager.IMPORTANCE_MIN);
            s.setSound(null,null);
            nm.createNotificationChannel(s);
            nm.createNotificationChannel(new NotificationChannel("yoyo_messages","رسائل YoYo",NotificationManager.IMPORTANCE_HIGH));
            nm.createNotificationChannel(new NotificationChannel("yoyo_calls","مكالمات YoYo",NotificationManager.IMPORTANCE_HIGH));
        }
    }

    @Override public int onStartCommand(Intent intent,int flags,int startId){ return START_STICKY; }
    @Override public void onDestroy(){
        stopping=true;
        h.removeCallbacksAndMessages(null);
        if(ws!=null)ws.cancel();
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent){ return null; }

    public static void start(android.content.Context c){
        ContextCompat.startForegroundService(c,new Intent(c,RealtimeService.class));
    }
}
