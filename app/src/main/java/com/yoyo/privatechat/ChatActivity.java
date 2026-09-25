package com.yoyo.privatechat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;

import java.util.UUID;

public class ChatActivity extends AppCompatActivity {
    private String phone,name,avatar;
    private RecyclerView list;
    private MessageAdapter adapter;
    private EditText input;

    private final BroadcastReceiver receiver=new BroadcastReceiver(){
        @Override public void onReceive(Context c,Intent i){refresh();}
    };

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        phone=getIntent().getStringExtra("phone");
        name=getIntent().getStringExtra("name");
        avatar=getIntent().getStringExtra("avatar");
        if(phone==null){finish();return;}
        build();
    }

    private void build(){
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        LinearLayout top=new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(Ui.dp(this,8),Ui.dp(this,10),Ui.dp(this,10),Ui.dp(this,8));

        TextView back=Ui.text(this,"‹",40,Ui.C_ACCENT);
        back.setGravity(Gravity.CENTER);
        top.addView(back,new LinearLayout.LayoutParams(Ui.dp(this,46),Ui.dp(this,48)));
        back.setOnClickListener(v->finish());

        ImageView av=new ImageView(this);
        av.setBackgroundResource(R.drawable.avatar_circle);
        av.setClipToOutline(true);
        Avatar.into(av,avatar);
        top.addView(av,new LinearLayout.LayoutParams(Ui.dp(this,45),Ui.dp(this,45)));

        LinearLayout mid=new LinearLayout(this);
        mid.setOrientation(LinearLayout.VERTICAL);
        mid.setPadding(Ui.dp(this,10),0,0,0);
        top.addView(mid,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));

        TextView n=Ui.text(this,name==null?phone:name,18,Ui.C_TEXT);
        n.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);
        mid.addView(n);
        mid.addView(Ui.text(this,"YoYo • رسائل محفوظة",12,Ui.C_MUTED));

        TextView call=Ui.text(this,"📞",28,Ui.C_ACCENT);
        call.setGravity(Gravity.CENTER);
        top.addView(call,new LinearLayout.LayoutParams(Ui.dp(this,52),Ui.dp(this,52)));
        call.setOnClickListener(v->startActivity(new Intent(this,CallActivity.class)
                .putExtra("mode","outgoing")
                .putExtra("phone",phone)
                .putExtra("name",name)));
        root.addView(top);

        list=new RecyclerView(this);
        LinearLayoutManager lm=new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        list.setLayoutManager(lm);
        adapter=new MessageAdapter(this,Prefs.phone(this));
        list.setAdapter(adapter);
        root.addView(list,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1));

        LinearLayout composer=new LinearLayout(this);
        composer.setGravity(Gravity.BOTTOM|Gravity.CENTER_VERTICAL);
        composer.setPadding(Ui.dp(this,10),Ui.dp(this,8),Ui.dp(this,10),Ui.dp(this,12));

        input=new EditText(this);
        input.setHint("اكتب رسالة...");
        input.setTextSize(16);
        input.setMaxLines(4);
        input.setBackgroundResource(R.drawable.rounded_input);
        input.setPadding(Ui.dp(this,15),Ui.dp(this,9),Ui.dp(this,15),Ui.dp(this,9));
        composer.addView(input,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));

        TextView send=Ui.text(this,"➤",27,Color.WHITE);
        send.setGravity(Gravity.CENTER);
        send.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Ui.C_ACCENT));
        send.setBackgroundResource(R.drawable.ic_heart);
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(Ui.dp(this,54),Ui.dp(this,54));
        sp.setMargins(Ui.dp(this,8),0,0,0);
        composer.addView(send,sp);
        send.setOnClickListener(v->send());

        input.setOnEditorActionListener((v,action,event)->{
            if(event!=null&&event.getKeyCode()==KeyEvent.KEYCODE_ENTER){
                send();
                return true;
            }
            return false;
        });

        root.addView(composer);
        setContentView(root);
        refresh();
    }

    private void send(){
        String txt=input.getText().toString().trim();
        if(txt.isEmpty())return;

        input.setText("");
        String id=UUID.randomUUID().toString();
        long ts=System.currentTimeMillis();
        EventStore.Msg m=new EventStore.Msg(id,Prefs.phone(this),phone,txt,ts,"pending");
        EventStore.addMessage(this,m);
        refresh();

        BackendClient.send(this,m,(ok,error)->runOnUiThread(()->{
            if(ok){
                EventStore.markSent(this,phone,id);
                refresh();
            }else{
                Toast.makeText(this,"محفوظة عندك وستُرسل تلقائيًا عند رجوع النت",Toast.LENGTH_SHORT).show();
            }
        }));

        try{
            JSONObject o=new JSONObject();
            o.put("type","chat");
            o.put("id",id);
            o.put("from",Prefs.phone(this));
            o.put("name",Prefs.name(this));
            o.put("avatar",Prefs.avatar(this));
            o.put("to",phone);
            o.put("text",txt);
            o.put("ts",ts);
            RelayClient.publish(o.toString(),null);
        }catch(Exception ignored){}
    }

    private void refresh(){
        if(adapter==null)return;
        java.util.List<EventStore.Msg> msgs=EventStore.messages(this,phone);
        adapter.setData(msgs);
        if(!msgs.isEmpty())list.scrollToPosition(msgs.size()-1);
    }

    @Override protected void onResume(){
        super.onResume();
        try{
            registerReceiver(receiver,new IntentFilter(RealtimeService.ACTION_EVENT),
                    Build.VERSION.SDK_INT>=33?Context.RECEIVER_NOT_EXPORTED:0);
        }catch(Exception ignored){}
        refresh();
    }

    @Override protected void onPause(){
        try{unregisterReceiver(receiver);}catch(Exception ignored){}
        super.onPause();
    }
}
