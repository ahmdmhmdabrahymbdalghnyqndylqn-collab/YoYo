package com.yoyo.privatechat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.ViewGroup;
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
    private String mode,phone,name,callId,offerSdp; private TextView status,time;
    private PeerConnectionFactory factory; private PeerConnection pc; private AudioTrack localAudio;
    private final List<IceCandidate> pendingIce=new ArrayList<>(); private boolean remoteSet=false,muted=false,speaker=false,connected=false;
    private final Handler h=new Handler(Looper.getMainLooper()); private long connectedAt=0;
    private final BroadcastReceiver receiver=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){String j=i.getStringExtra(RealtimeService.EXTRA_JSON);if(j!=null)handleSignal(j);}};

    @Override protected void onCreate(Bundle b){super.onCreate(b);mode=getIntent().getStringExtra("mode");phone=getIntent().getStringExtra("phone");name=getIntent().getStringExtra("name");callId=getIntent().getStringExtra("callId");offerSdp=getIntent().getStringExtra("sdp");if(callId==null)callId=UUID.randomUUID().toString();buildUi();if(ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.RECORD_AUDIO},77);else startRtc();}

    private void buildUi(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setGravity(Gravity.CENTER_HORIZONTAL);root.setBackgroundColor(Color.WHITE);root.setPadding(Ui.dp(this,28),Ui.dp(this,70),Ui.dp(this,28),Ui.dp(this,35));
        TextView heart=Ui.text(this,"♥",92,Ui.C_ACCENT);heart.setGravity(Gravity.CENTER);root.addView(heart,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,Ui.dp(this,120)));
        TextView n=Ui.text(this,name==null?phone:name,29,Ui.C_TEXT);n.setGravity(Gravity.CENTER);n.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);root.addView(n);
        status=Ui.text(this,"incoming".equals(mode)?"مكالمة واردة...":"جاري الاتصال...",16,Ui.C_MUTED);status.setGravity(Gravity.CENTER);status.setPadding(0,Ui.dp(this,8),0,0);root.addView(status);
        time=Ui.text(this,"",15,Ui.C_MUTED);time.setGravity(Gravity.CENTER);time.setPadding(0,Ui.dp(this,8),0,0);root.addView(time);
        LinearLayout buttons=new LinearLayout(this);buttons.setGravity(Gravity.CENTER);buttons.setPadding(0,Ui.dp(this,70),0,0);root.addView(buttons,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1));
        TextView mute=round("🎙\nكتم");TextView sp=round("🔊\nسماعة");TextView end=round("✕\nإنهاء");buttons.addView(mute,new LinearLayout.LayoutParams(0,Ui.dp(this,90),1));buttons.addView(sp,new LinearLayout.LayoutParams(0,Ui.dp(this,90),1));buttons.addView(end,new LinearLayout.LayoutParams(0,Ui.dp(this,90),1));
        mute.setOnClickListener(v->{muted=!muted;if(localAudio!=null)localAudio.setEnabled(!muted);mute.setAlpha(muted?.45f:1f);});
        sp.setOnClickListener(v->{speaker=!speaker;AudioManager am=(AudioManager)getSystemService(AUDIO_SERVICE);am.setSpeakerphoneOn(speaker);sp.setAlpha(speaker?1f:.55f);});
        end.setTextColor(Color.WHITE);end.setBackgroundColor(Ui.C_ACCENT);end.setOnClickListener(v->hangup(true));setContentView(root);
    }
    private TextView round(String s){TextView t=Ui.text(this,s,16,Ui.C_TEXT);t.setGravity(Gravity.CENTER);t.setPadding(Ui.dp(this,5),Ui.dp(this,8),Ui.dp(this,5),Ui.dp(this,8));return t;}

    private void startRtc(){
        try{
            PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(getApplicationContext()).createInitializationOptions());
            factory=PeerConnectionFactory.builder().createPeerConnectionFactory();
            MediaConstraints ac=new MediaConstraints();ac.mandatory.add(new MediaConstraints.KeyValuePair("googEchoCancellation","true"));ac.mandatory.add(new MediaConstraints.KeyValuePair("googNoiseSuppression","true"));ac.mandatory.add(new MediaConstraints.KeyValuePair("googAutoGainControl","true"));
            AudioSource src=factory.createAudioSource(ac);localAudio=factory.createAudioTrack("yoyo_audio",src);
            List<PeerConnection.IceServer> servers=new ArrayList<>();servers.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer());servers.add(PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer());
            PeerConnection.RTCConfiguration cfg=new PeerConnection.RTCConfiguration(servers);cfg.sdpSemantics=PeerConnection.SdpSemantics.UNIFIED_PLAN;cfg.continualGatheringPolicy=PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;
            pc=factory.createPeerConnection(cfg,new PeerConnection.Observer(){
                @Override public void onSignalingChange(PeerConnection.SignalingState s){}
                @Override public void onIceConnectionChange(PeerConnection.IceConnectionState s){runOnUiThread(()->{if(s==PeerConnection.IceConnectionState.CONNECTED||s==PeerConnection.IceConnectionState.COMPLETED)onConnected();else if(s==PeerConnection.IceConnectionState.FAILED)status.setText("تعذر الاتصال — جرّب شبكة أخرى");});}
                @Override public void onIceConnectionReceivingChange(boolean b){}
                @Override public void onIceGatheringChange(PeerConnection.IceGatheringState s){}
                @Override public void onIceCandidate(IceCandidate c){sendIce(c);}
                @Override public void onIceCandidatesRemoved(IceCandidate[] c){}
                @Override public void onAddStream(MediaStream s){}
                @Override public void onRemoveStream(MediaStream s){}
                @Override public void onDataChannel(DataChannel d){}
                @Override public void onRenegotiationNeeded(){}
                @Override public void onAddTrack(RtpReceiver r,MediaStream[] ms){}
                @Override public void onConnectionChange(PeerConnection.PeerConnectionState s){runOnUiThread(()->{if(s==PeerConnection.PeerConnectionState.CONNECTED)onConnected();});}
            });
            if(pc==null)throw new IllegalStateException("peer connection");pc.addTrack(localAudio);
            AudioManager am=(AudioManager)getSystemService(AUDIO_SERVICE);am.setMode(AudioManager.MODE_IN_COMMUNICATION);am.setSpeakerphoneOn(false);
            if("incoming".equals(mode)&&offerSdp!=null)acceptOffer(offerSdp);else createOffer();
        }catch(Exception e){status.setText("خطأ في تشغيل المكالمة");Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();}
    }

    private void createOffer(){MediaConstraints c=new MediaConstraints();c.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio","true"));pc.createOffer(new Obs(){@Override public void onCreateSuccess(SessionDescription s){pc.setLocalDescription(new Obs(){@Override public void onSetSuccess(){sendSdp("call_offer",s.description);runOnUiThread(()->status.setText("يرن..."));}},s);}},c);}
    private void acceptOffer(String sdp){SessionDescription s=new SessionDescription(SessionDescription.Type.OFFER,sdp);pc.setRemoteDescription(new Obs(){@Override public void onSetSuccess(){remoteSet=true;flushIce();MediaConstraints c=new MediaConstraints();c.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio","true"));pc.createAnswer(new Obs(){@Override public void onCreateSuccess(SessionDescription a){pc.setLocalDescription(new Obs(){@Override public void onSetSuccess(){sendSdp("call_answer",a.description);runOnUiThread(()->status.setText("جاري توصيل المكالمة..."));}},a);}},c);}},s);}
    private void handleSignal(String json){try{JSONObject o=new JSONObject(json);if(!callId.equals(o.optString("callId")))return;String type=o.optString("type");if("call_answer".equals(type)&&"outgoing".equals(mode)){SessionDescription s=new SessionDescription(SessionDescription.Type.ANSWER,o.optString("sdp"));pc.setRemoteDescription(new Obs(){@Override public void onSetSuccess(){remoteSet=true;flushIce();}},s);}else if("ice".equals(type)){IceCandidate ic=new IceCandidate(o.optString("mid"),o.optInt("mline"),o.optString("candidate"));if(remoteSet&&pc!=null)pc.addIceCandidate(ic);else pendingIce.add(ic);}else if("call_hangup".equals(type)){runOnUiThread(()->{status.setText("انتهت المكالمة");h.postDelayed(this::finish,700);});}}catch(Exception ignored){}}
    private void flushIce(){if(pc==null)return;for(IceCandidate i:pendingIce)pc.addIceCandidate(i);pendingIce.clear();}
    private void sendSdp(String type,String sdp){try{JSONObject o=base(type);o.put("sdp",sdp);RelayClient.publish(o.toString(),null);}catch(Exception ignored){}}
    private void sendIce(IceCandidate c){try{JSONObject o=base("ice");o.put("mid",c.sdpMid);o.put("mline",c.sdpMLineIndex);o.put("candidate",c.sdp);RelayClient.publish(o.toString(),null);}catch(Exception ignored){}}
    private JSONObject base(String type)throws Exception{JSONObject o=new JSONObject();o.put("type",type);o.put("id",UUID.randomUUID().toString());o.put("callId",callId);o.put("from",Prefs.phone(this));o.put("name",Prefs.name(this));o.put("to",phone);o.put("ts",System.currentTimeMillis());return o;}
    private void onConnected(){if(connected)return;connected=true;connectedAt=System.currentTimeMillis();status.setText("متصل");h.post(ticker);}
    private final Runnable ticker=new Runnable(){@Override public void run(){if(!connected)return;long sec=(System.currentTimeMillis()-connectedAt)/1000;time.setText(String.format(java.util.Locale.getDefault(),"%02d:%02d",sec/60,sec%60));h.postDelayed(this,1000);}};
    private void hangup(boolean send){if(send){try{RelayClient.publish(base("call_hangup").toString(),null);}catch(Exception ignored){}}cleanup();finish();}
    private void cleanup(){connected=false;h.removeCallbacksAndMessages(null);try{if(pc!=null)pc.close();}catch(Exception ignored){}try{if(factory!=null)factory.dispose();}catch(Exception ignored){}AudioManager am=(AudioManager)getSystemService(AUDIO_SERVICE);am.setSpeakerphoneOn(false);am.setMode(AudioManager.MODE_NORMAL);}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==77&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED)startRtc();else{Toast.makeText(this,"الميكروفون مطلوب للمكالمة",Toast.LENGTH_LONG).show();finish();}}
    @Override protected void onResume(){super.onResume();try{registerReceiver(receiver,new IntentFilter(RealtimeService.ACTION_EVENT),Build.VERSION.SDK_INT>=33?Context.RECEIVER_NOT_EXPORTED:0);}catch(Exception ignored){}}
    @Override protected void onPause(){try{unregisterReceiver(receiver);}catch(Exception ignored){}super.onPause();}
    @Override protected void onDestroy(){cleanup();super.onDestroy();}
    private static class Obs implements SdpObserver{public void onCreateSuccess(SessionDescription s){}public void onSetSuccess(){}public void onCreateFailure(String e){}public void onSetFailure(String e){}}
}
