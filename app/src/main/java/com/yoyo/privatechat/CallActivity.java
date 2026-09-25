package com.yoyo.privatechat;

import android.Manifest;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CallActivity extends AppCompatActivity {
    private String mode,phone,name,callId,offerSdp;
    private TextView status,time;
    private LinearLayout incomingButtons,inCallButtons;
    private PeerConnectionFactory factory;
    private PeerConnection pc;
    private AudioTrack localAudio;
    private final List<IceCandidate> pendingIce=new ArrayList<>();
    private boolean remoteSet=false,muted=false,speaker=false,connected=false,accepted=false;
    private final Handler h=new Handler(Looper.getMainLooper());
    private long connectedAt=0;
    private Ringtone incomingRingtone;
    private ToneGenerator ringback;
    private AudioManager audioManager;

    private final BroadcastReceiver receiver=new BroadcastReceiver(){
        @Override public void onReceive(Context c,Intent i){
            String j=i.getStringExtra(RealtimeService.EXTRA_JSON);
            if(j!=null)handleSignal(j);
        }
    };

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);

        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O_MR1){
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }else{
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                            | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            );
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mode=getIntent().getStringExtra("mode");
        phone=getIntent().getStringExtra("phone");
        name=getIntent().getStringExtra("name");
        callId=getIntent().getStringExtra("callId");
        offerSdp=getIntent().getStringExtra("sdp");
        if(callId==null)callId=UUID.randomUUID().toString();

        audioManager=(AudioManager)getSystemService(AUDIO_SERVICE);
        ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).cancel(9001);

        buildUi();

        if("incoming".equals(mode)){
            startIncomingRinging();
            if(getIntent().getBooleanExtra("autoAnswer",false)){
                acceptIncomingCall();
            }
        }else{
            requestNeededPermissionsOrStart();
        }
    }

    private void requestNeededPermissionsOrStart(){
        ArrayList<String> req=new ArrayList<>();

        if(ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)
                !=PackageManager.PERMISSION_GRANTED){
            req.add(Manifest.permission.RECORD_AUDIO);
        }

        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S
                && ContextCompat.checkSelfPermission(this,Manifest.permission.BLUETOOTH_CONNECT)
                !=PackageManager.PERMISSION_GRANTED){
            req.add(Manifest.permission.BLUETOOTH_CONNECT);
        }

        if(req.isEmpty()){
            startRtc();
        }else{
            ActivityCompat.requestPermissions(this,req.toArray(new String[0]),77);
        }
    }

    private void buildUi(){
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(Color.WHITE);
        root.setPadding(Ui.dp(this,28),Ui.dp(this,70),Ui.dp(this,28),Ui.dp(this,35));

        TextView heart=Ui.text(this,"♥",92,Ui.C_ACCENT);
        heart.setGravity(Gravity.CENTER);
        root.addView(heart,new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,Ui.dp(this,120)));

        TextView n=Ui.text(this,name==null?phone:name,29,Ui.C_TEXT);
        n.setGravity(Gravity.CENTER);
        n.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);
        root.addView(n);

        status=Ui.text(
                this,
                "incoming".equals(mode)?"مكالمة YoYo واردة":"جاري الاتصال...",
                17,
                Ui.C_MUTED
        );
        status.setGravity(Gravity.CENTER);
        status.setPadding(0,Ui.dp(this,8),0,0);
        root.addView(status);

        time=Ui.text(this,"",15,Ui.C_MUTED);
        time.setGravity(Gravity.CENTER);
        time.setPadding(0,Ui.dp(this,8),0,0);
        root.addView(time);

        incomingButtons=new LinearLayout(this);
        incomingButtons.setGravity(Gravity.CENTER);
        incomingButtons.setPadding(0,Ui.dp(this,70),0,0);

        TextView reject=callButton("✕\nرفض",Color.rgb(220,53,69));
        TextView answer=callButton("☎\nرد",Color.rgb(34,170,76));

        LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,Ui.dp(this,100),1);
        bp.setMargins(Ui.dp(this,8),0,Ui.dp(this,8),0);
        incomingButtons.addView(reject,bp);
        incomingButtons.addView(answer,bp);

        reject.setOnClickListener(v->rejectIncomingCall());
        answer.setOnClickListener(v->acceptIncomingCall());

        root.addView(
                incomingButtons,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        inCallButtons=new LinearLayout(this);
        inCallButtons.setGravity(Gravity.CENTER);
        inCallButtons.setPadding(0,Ui.dp(this,70),0,0);

        TextView mute=round("🎙\nكتم");
        TextView sp=round("🔊\nسماعة");
        TextView end=callButton("✕\nإنهاء",Ui.C_ACCENT);

        inCallButtons.addView(mute,new LinearLayout.LayoutParams(0,Ui.dp(this,90),1));
        inCallButtons.addView(sp,new LinearLayout.LayoutParams(0,Ui.dp(this,90),1));
        inCallButtons.addView(end,new LinearLayout.LayoutParams(0,Ui.dp(this,90),1));

        mute.setOnClickListener(v->{
            muted=!muted;
            if(localAudio!=null)localAudio.setEnabled(!muted);
            mute.setAlpha(muted?.45f:1f);
        });

        sp.setOnClickListener(v->{
            speaker=!speaker;
            if(speaker)routeSpeaker();
            else routePreferredAudio();
            sp.setAlpha(speaker?1f:.55f);
        });

        end.setOnClickListener(v->hangup(true));

        root.addView(
                inCallButtons,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        if("incoming".equals(mode)){
            incomingButtons.setVisibility(View.VISIBLE);
            inCallButtons.setVisibility(View.GONE);
        }else{
            incomingButtons.setVisibility(View.GONE);
            inCallButtons.setVisibility(View.VISIBLE);
        }

        setContentView(root);
    }

    private TextView callButton(String s,int bg){
        TextView t=Ui.text(this,s,18,Color.WHITE);
        t.setGravity(Gravity.CENTER);
        t.setPadding(Ui.dp(this,8),Ui.dp(this,12),Ui.dp(this,8),Ui.dp(this,12));
        t.setBackgroundTintList(android.content.res.ColorStateList.valueOf(bg));
        android.graphics.drawable.GradientDrawable g=new android.graphics.drawable.GradientDrawable();
        g.setColor(bg);
        g.setCornerRadius(Ui.dp(this,32));
        t.setBackground(g);
        return t;
    }

    private TextView round(String s){
        TextView t=Ui.text(this,s,16,Ui.C_TEXT);
        t.setGravity(Gravity.CENTER);
        t.setPadding(Ui.dp(this,5),Ui.dp(this,8),Ui.dp(this,5),Ui.dp(this,8));
        return t;
    }

    private void acceptIncomingCall(){
        if(accepted)return;
        accepted=true;

        stopIncomingRinging();
        ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).cancel(9001);

        if(incomingButtons!=null)incomingButtons.setVisibility(View.GONE);
        if(inCallButtons!=null)inCallButtons.setVisibility(View.VISIBLE);

        status.setText("جاري توصيل المكالمة...");
        requestNeededPermissionsOrStart();
    }

    private void rejectIncomingCall(){
        stopIncomingRinging();
        ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).cancel(9001);

        try{
            RelayClient.publish(base("call_hangup").toString(),null);
        }catch(Exception ignored){}

        cleanup();
        finish();
    }

    private void startIncomingRinging(){
        try{
            Uri uri=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            incomingRingtone=RingtoneManager.getRingtone(this,uri);

            if(incomingRingtone!=null){
                if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.P){
                    incomingRingtone.setLooping(true);
                }

                if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP){
                    incomingRingtone.setAudioAttributes(
                            new AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                    .build()
                    );
                }

                incomingRingtone.play();
            }
        }catch(Exception ignored){}
    }

    private void stopIncomingRinging(){
        try{
            if(incomingRingtone!=null&&incomingRingtone.isPlaying()){
                incomingRingtone.stop();
            }
        }catch(Exception ignored){}

        incomingRingtone=null;
    }

    private void startRingback(){
        if("incoming".equals(mode))return;

        try{
            if(ringback==null){
                ringback=new ToneGenerator(AudioManager.STREAM_VOICE_CALL,70);
            }
            ringback.startTone(ToneGenerator.TONE_SUP_RINGTONE);
        }catch(Exception ignored){}
    }

    private void stopRingback(){
        try{
            if(ringback!=null)ringback.stopTone();
        }catch(Exception ignored){}
    }

    private void startRtc(){
        try{
            PeerConnectionFactory.initialize(
                    PeerConnectionFactory.InitializationOptions
                            .builder(getApplicationContext())
                            .createInitializationOptions()
            );

            factory=PeerConnectionFactory.builder().createPeerConnectionFactory();

            MediaConstraints ac=new MediaConstraints();
            ac.mandatory.add(new MediaConstraints.KeyValuePair("googEchoCancellation","true"));
            ac.mandatory.add(new MediaConstraints.KeyValuePair("googNoiseSuppression","true"));
            ac.mandatory.add(new MediaConstraints.KeyValuePair("googAutoGainControl","true"));

            AudioSource src=factory.createAudioSource(ac);
            localAudio=factory.createAudioTrack("yoyo_audio",src);

            List<PeerConnection.IceServer> servers=new ArrayList<>();
            servers.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer());
            servers.add(PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer());

            PeerConnection.RTCConfiguration cfg=new PeerConnection.RTCConfiguration(servers);
            cfg.sdpSemantics=PeerConnection.SdpSemantics.UNIFIED_PLAN;
            cfg.continualGatheringPolicy=PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;

            pc=factory.createPeerConnection(cfg,new PeerConnection.Observer(){
                @Override public void onSignalingChange(PeerConnection.SignalingState s){}

                @Override public void onIceConnectionChange(PeerConnection.IceConnectionState s){
                    runOnUiThread(()->{
                        if(s==PeerConnection.IceConnectionState.CONNECTED
                                ||s==PeerConnection.IceConnectionState.COMPLETED){
                            onConnected();
                        }else if(s==PeerConnection.IceConnectionState.FAILED){
                            stopRingback();
                            status.setText("تعذر الاتصال — جرّب شبكة أخرى");
                        }
                    });
                }

                @Override public void onIceConnectionReceivingChange(boolean b){}
                @Override public void onIceGatheringChange(PeerConnection.IceGatheringState s){}
                @Override public void onIceCandidate(IceCandidate c){sendIce(c);}
                @Override public void onIceCandidatesRemoved(IceCandidate[] c){}
                @Override public void onAddStream(MediaStream s){}
                @Override public void onRemoveStream(MediaStream s){}
                @Override public void onDataChannel(DataChannel d){}
                @Override public void onRenegotiationNeeded(){}
                @Override public void onAddTrack(RtpReceiver r,MediaStream[] ms){}

                @Override public void onConnectionChange(PeerConnection.PeerConnectionState s){
                    runOnUiThread(()->{
                        if(s==PeerConnection.PeerConnectionState.CONNECTED){
                            onConnected();
                        }
                    });
                }
            });

            if(pc==null)throw new IllegalStateException("peer connection");

            pc.addTrack(localAudio);
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            routePreferredAudio();

            if("incoming".equals(mode)&&offerSdp!=null){
                acceptOffer(offerSdp);
            }else{
                createOffer();
            }

        }catch(Exception e){
            stopIncomingRinging();
            stopRingback();
            status.setText("خطأ في تشغيل المكالمة");
            Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();
        }
    }

    private boolean hasBluetoothPermission(){
        return Build.VERSION.SDK_INT<Build.VERSION_CODES.S
                || ContextCompat.checkSelfPermission(this,Manifest.permission.BLUETOOTH_CONNECT)
                ==PackageManager.PERMISSION_GRANTED;
    }

    private boolean routeBluetooth(){
        try{
            if(!hasBluetoothPermission())return false;

            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S){
                for(AudioDeviceInfo d:audioManager.getAvailableCommunicationDevices()){
                    int t=d.getType();

                    if(t==AudioDeviceInfo.TYPE_BLUETOOTH_SCO
                            ||t==AudioDeviceInfo.TYPE_BLE_HEADSET
                            ||t==AudioDeviceInfo.TYPE_BLE_SPEAKER){
                        return audioManager.setCommunicationDevice(d);
                    }
                }

                return false;
            }

            if(audioManager.isBluetoothScoAvailableOffCall()){
                audioManager.startBluetoothSco();
                audioManager.setBluetoothScoOn(true);
                audioManager.setSpeakerphoneOn(false);
                return true;
            }

        }catch(Exception ignored){}

        return false;
    }

    private void routePreferredAudio(){
        speaker=false;

        if(routeBluetooth())return;

        try{
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S){
                for(AudioDeviceInfo d:audioManager.getAvailableCommunicationDevices()){
                    if(d.getType()==AudioDeviceInfo.TYPE_BUILTIN_EARPIECE){
                        audioManager.setCommunicationDevice(d);
                        return;
                    }
                }

                audioManager.clearCommunicationDevice();

            }else{
                audioManager.setBluetoothScoOn(false);
                audioManager.stopBluetoothSco();
                audioManager.setSpeakerphoneOn(false);
            }
        }catch(Exception ignored){}
    }

    private void routeSpeaker(){
        try{
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S){
                for(AudioDeviceInfo d:audioManager.getAvailableCommunicationDevices()){
                    if(d.getType()==AudioDeviceInfo.TYPE_BUILTIN_SPEAKER){
                        audioManager.setCommunicationDevice(d);
                        return;
                    }
                }

            }else{
                audioManager.setBluetoothScoOn(false);
                audioManager.stopBluetoothSco();
                audioManager.setSpeakerphoneOn(true);
            }

        }catch(Exception ignored){}
    }

    private void createOffer(){
        MediaConstraints c=new MediaConstraints();
        c.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio","true"));

        pc.createOffer(new Obs(){
            @Override public void onCreateSuccess(SessionDescription s){
                pc.setLocalDescription(new Obs(){
                    @Override public void onSetSuccess(){
                        sendSdp("call_offer",s.description);

                        runOnUiThread(()->{
                            status.setText("يرن...");
                            startRingback();
                        });
                    }
                },s);
            }
        },c);
    }

    private void acceptOffer(String sdp){
        SessionDescription s=new SessionDescription(SessionDescription.Type.OFFER,sdp);

        pc.setRemoteDescription(new Obs(){
            @Override public void onSetSuccess(){
                remoteSet=true;
                flushIce();

                MediaConstraints c=new MediaConstraints();
                c.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio","true"));

                pc.createAnswer(new Obs(){
                    @Override public void onCreateSuccess(SessionDescription a){
                        pc.setLocalDescription(new Obs(){
                            @Override public void onSetSuccess(){
                                sendSdp("call_answer",a.description);
                                runOnUiThread(()->status.setText("جاري توصيل المكالمة..."));
                            }
                        },a);
                    }
                },c);
            }
        },s);
    }

    private void handleSignal(String json){
        try{
            JSONObject o=new JSONObject(json);

            if(!callId.equals(o.optString("callId")))return;

            String type=o.optString("type");

            if("call_answer".equals(type)&&"outgoing".equals(mode)){
                stopRingback();

                SessionDescription s=new SessionDescription(
                        SessionDescription.Type.ANSWER,
                        o.optString("sdp")
                );

                if(pc!=null){
                    pc.setRemoteDescription(new Obs(){
                        @Override public void onSetSuccess(){
                            remoteSet=true;
                            flushIce();
                        }
                    },s);
                }

            }else if("ice".equals(type)){
                IceCandidate ic=new IceCandidate(
                        o.optString("mid"),
                        o.optInt("mline"),
                        o.optString("candidate")
                );

                if(remoteSet&&pc!=null){
                    pc.addIceCandidate(ic);
                }else{
                    pendingIce.add(ic);
                }

            }else if("call_hangup".equals(type)){
                runOnUiThread(()->{
                    stopIncomingRinging();
                    stopRingback();
                    status.setText("انتهت المكالمة");
                    h.postDelayed(this::finish,700);
                });
            }

        }catch(Exception ignored){}
    }

    private void flushIce(){
        if(pc==null)return;

        for(IceCandidate i:pendingIce){
            pc.addIceCandidate(i);
        }

        pendingIce.clear();
    }

    private void sendSdp(String type,String sdp){
        try{
            JSONObject o=base(type);
            o.put("sdp",sdp);
            RelayClient.publish(o.toString(),null);
        }catch(Exception ignored){}
    }

    private void sendIce(IceCandidate c){
        try{
            JSONObject o=base("ice");
            o.put("mid",c.sdpMid);
            o.put("mline",c.sdpMLineIndex);
            o.put("candidate",c.sdp);
            RelayClient.publish(o.toString(),null);
        }catch(Exception ignored){}
    }

    private JSONObject base(String type)throws Exception{
        JSONObject o=new JSONObject();
        o.put("type",type);
        o.put("id",UUID.randomUUID().toString());
        o.put("callId",callId);
        o.put("from",Prefs.phone(this));
        o.put("name",Prefs.name(this));
        o.put("to",phone);
        o.put("ts",System.currentTimeMillis());
        return o;
    }

    private void onConnected(){
        if(connected)return;

        connected=true;
        connectedAt=System.currentTimeMillis();

        stopIncomingRinging();
        stopRingback();

        if(incomingButtons!=null)incomingButtons.setVisibility(View.GONE);
        if(inCallButtons!=null)inCallButtons.setVisibility(View.VISIBLE);

        status.setText("متصل");
        h.post(ticker);
    }

    private final Runnable ticker=new Runnable(){
        @Override public void run(){
            if(!connected)return;

            long sec=(System.currentTimeMillis()-connectedAt)/1000;

            time.setText(
                    String.format(
                            java.util.Locale.getDefault(),
                            "%02d:%02d",
                            sec/60,
                            sec%60
                    )
            );

            h.postDelayed(this,1000);
        }
    };

    private void hangup(boolean send){
        if(send){
            try{
                RelayClient.publish(base("call_hangup").toString(),null);
            }catch(Exception ignored){}
        }

        cleanup();
        finish();
    }

    private void cleanup(){
        connected=false;
        h.removeCallbacksAndMessages(null);

        stopIncomingRinging();
        stopRingback();

        try{
            if(ringback!=null)ringback.release();
        }catch(Exception ignored){}

        ringback=null;

        try{
            if(pc!=null)pc.close();
        }catch(Exception ignored){}

        try{
            if(factory!=null)factory.dispose();
        }catch(Exception ignored){}

        try{
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S){
                audioManager.clearCommunicationDevice();
            }else{
                audioManager.setBluetoothScoOn(false);
                audioManager.stopBluetoothSco();
                audioManager.setSpeakerphoneOn(false);
            }

            audioManager.setMode(AudioManager.MODE_NORMAL);

        }catch(Exception ignored){}
    }

    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){
        super.onRequestPermissionsResult(r,p,g);

        if(r==77){
            if(ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)
                    ==PackageManager.PERMISSION_GRANTED){
                startRtc();
            }else{
                stopIncomingRinging();
                Toast.makeText(
                        this,
                        "الميكروفون مطلوب للمكالمة",
                        Toast.LENGTH_LONG
                ).show();
                finish();
            }
        }
    }

    @Override protected void onResume(){
        super.onResume();

        try{
            registerReceiver(
                    receiver,
                    new IntentFilter(RealtimeService.ACTION_EVENT),
                    Build.VERSION.SDK_INT>=33
                            ?Context.RECEIVER_NOT_EXPORTED
                            :0
            );
        }catch(Exception ignored){}
    }

    @Override protected void onPause(){
        try{
            unregisterReceiver(receiver);
        }catch(Exception ignored){}

        super.onPause();
    }

    @Override protected void onDestroy(){
        cleanup();
        super.onDestroy();
    }

    private static class Obs implements SdpObserver{
        public void onCreateSuccess(SessionDescription s){}
        public void onSetSuccess(){}
        public void onCreateFailure(String e){}
        public void onSetFailure(String e){}
    }
}
