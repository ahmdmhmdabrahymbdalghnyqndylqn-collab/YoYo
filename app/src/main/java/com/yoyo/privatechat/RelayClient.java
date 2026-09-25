package com.yoyo.privatechat;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public final class RelayClient {
    public static final String TOPIC = "yoyo-a2ff119c323eac06be99254633c54462";
    private static final String BASE = "https://ntfy.sh/" + TOPIC;
    private static final OkHttpClient HTTP = new OkHttpClient.Builder().pingInterval(25, TimeUnit.SECONDS).retryOnConnectionFailure(true).build();
    private static final Gson GSON = new Gson();

    public interface Listener { void onEvent(String json); void onState(boolean connected); }
    public interface SendCallback { void done(boolean ok); }

    private RelayClient() {}

    public static WebSocket subscribe(final Listener listener){
        Request req = new Request.Builder().url("wss://ntfy.sh/" + TOPIC + "/ws?since=10m").build();
        return HTTP.newWebSocket(req, new WebSocketListener() {
            @Override public void onOpen(WebSocket webSocket, Response response) { listener.onState(true); }
            @Override public void onMessage(WebSocket webSocket, String text) {
                try {
                    JsonObject wrapper = GSON.fromJson(text, JsonObject.class);
                    if (wrapper != null && wrapper.has("event") && "message".equals(wrapper.get("event").getAsString()) && wrapper.has("message")) {
                        String plain = CryptoBox.decrypt(wrapper.get("message").getAsString());
                        listener.onEvent(plain);
                    }
                } catch (Exception e) { Log.w("YoYoRelay", "Ignoring payload", e); }
            }
            @Override public void onClosed(WebSocket webSocket, int code, String reason) { listener.onState(false); }
            @Override public void onFailure(WebSocket webSocket, Throwable t, Response response) { listener.onState(false); }
        });
    }

    public static void publish(String json, SendCallback cb){
        try {
            String enc = CryptoBox.encrypt(json);
            Request req = new Request.Builder().url(BASE)
                    .post(RequestBody.create(enc, MediaType.parse("text/plain; charset=utf-8")))
                    .header("Priority", "high")
                    .build();
            HTTP.newCall(req).enqueue(new Callback() {
                @Override public void onFailure(Call call, java.io.IOException e) { if(cb != null) cb.done(false); }
                @Override public void onResponse(Call call, Response response) { boolean ok=response.isSuccessful(); response.close(); if(cb != null) cb.done(ok); }
            });
        } catch (Exception e) { if(cb != null) cb.done(false); }
    }
}
