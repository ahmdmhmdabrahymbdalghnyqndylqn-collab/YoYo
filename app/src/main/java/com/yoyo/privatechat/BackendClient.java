package com.yoyo.privatechat;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public final class BackendClient {
    private static final String URL = "https://ivjqorxkqnipyelnjlqp.supabase.co/functions/v1/yoyo-api";
    private static final String APP_KEY = "sb_publishable_nplKway4Sl6M39aa3jjSkA_oqjlOGBA";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();

    public interface AuthCallback { void done(boolean ok, String token, String error); }
    public interface BoolCallback { void done(boolean ok, String error); }
    public interface SyncCallback { void done(boolean ok, JSONObject data, String error); }

    private BackendClient() {}

    public static void auth(Context c, String phone, String name, String pin, String avatar, AuthCallback cb) {
        try {
            JSONObject o=new JSONObject();
            o.put("action","auth");
            o.put("phone",phone);
            o.put("name",name);
            o.put("pin",pin);
            o.put("avatar",avatar==null?"":avatar);
            post(c,o,false,(ok,data,error)->cb.done(ok,ok?data.optString("token",""):"",error));
        } catch(Exception e){ cb.done(false,"",e.getMessage()); }
    }

    public static void send(Context c, EventStore.Msg m, BoolCallback cb) {
        try {
            JSONObject o=new JSONObject();
            o.put("action","send");
            o.put("id",m.id);
            o.put("recipient",m.to);
            o.put("body",CryptoBox.encrypt(m.text));
            o.put("created_at", Instant.ofEpochMilli(m.ts).toString());
            post(c,o,true,(ok,data,error)->cb.done(ok,error));
        } catch(Exception e){ cb.done(false,e.getMessage()); }
    }

    public static void sync(Context c, SyncCallback cb) {
        try {
            JSONObject o=new JSONObject();
            o.put("action","sync");
            post(c,o,true,cb);
        } catch(Exception e){ cb.done(false,null,e.getMessage()); }
    }

    public static void ack(Context c, List<String> ids) {
        if(ids==null||ids.isEmpty()) return;
        try {
            JSONObject o=new JSONObject();
            o.put("action","ack");
            JSONArray a=new JSONArray();
            for(String id:ids) a.put(id);
            o.put("ids",a);
            post(c,o,true,(ok,data,error)->{});
        } catch(Exception ignored){}
    }

    public static void presence(Context c) {
        try {
            JSONObject o=new JSONObject();
            o.put("action","presence");
            post(c,o,true,(ok,data,error)->{});
        } catch(Exception ignored){}
    }

    private static void post(Context c, JSONObject body, boolean authenticated, SyncCallback cb) {
        Request.Builder b=new Request.Builder()
                .url(URL)
                .header("apikey",APP_KEY)
                .header("Content-Type","application/json")
                .post(RequestBody.create(body.toString(),JSON));
        if(authenticated){
            String token=Prefs.token(c);
            if(token==null||token.isEmpty()){ cb.done(false,null,"not authenticated"); return; }
            b.header("x-yoyo-token",token);
        }
        HTTP.newCall(b.build()).enqueue(new Callback() {
            @Override public void onFailure(Call call, java.io.IOException e) {
                cb.done(false,null,e.getMessage());
            }
            @Override public void onResponse(Call call, Response response) {
                try {
                    String raw=response.body()!=null?response.body().string():"{}";
                    JSONObject data=new JSONObject(raw.isEmpty()?"{}":raw);
                    if(response.isSuccessful()) cb.done(true,data,null);
                    else cb.done(false,data,data.optString("error","server error"));
                } catch(Exception e){ cb.done(false,null,e.getMessage()); }
                finally { response.close(); }
            }
        });
    }
}
