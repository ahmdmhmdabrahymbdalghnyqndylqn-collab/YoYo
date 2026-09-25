package com.yoyo.privatechat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class MainActivity extends AppCompatActivity {
    private RecyclerView list;
    private ContactAdapter adapter;
    private TextView empty;
    private String pickedAvatar="";

    private final ActivityResultLauncher<String[]> imagePicker=
            registerForActivityResult(new ActivityResultContracts.OpenDocument(),uri->{
                if(uri!=null){
                    pickedAvatar=Avatar.fromUri(this,uri);
                    showRegister();
                }
            });

    private final BroadcastReceiver receiver=new BroadcastReceiver(){
        @Override public void onReceive(Context c, Intent i){refresh();}
    };

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        if(Prefs.hasProfile(this)) showUnlock(); else showRegister();
    }

    private LinearLayout page(){
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);
        root.setPadding(Ui.dp(this,22),Ui.dp(this,20),Ui.dp(this,22),Ui.dp(this,20));
        return root;
    }

    private EditText input(String hint){
        EditText e=new EditText(this);
        e.setHint(hint);
        e.setTextSize(16);
        e.setSingleLine(true);
        e.setBackgroundResource(R.drawable.rounded_input);
        e.setPadding(Ui.dp(this,16),Ui.dp(this,12),Ui.dp(this,16),Ui.dp(this,12));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,Ui.dp(this,52));
        p.topMargin=Ui.dp(this,12);
        e.setLayoutParams(p);
        return e;
    }

    private Button action(String s){
        Button b=new Button(this);
        b.setText(s);
        b.setTextColor(Color.WHITE);
        b.setTextSize(16);
        b.setAllCaps(false);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Ui.C_ACCENT));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,Ui.dp(this,54));
        p.topMargin=Ui.dp(this,18);
        b.setLayoutParams(p);
        return b;
    }

    private void header(LinearLayout root,String title,String sub){
        ImageView logo=new ImageView(this);
        logo.setImageResource(R.drawable.ic_heart);
        root.addView(logo,new LinearLayout.LayoutParams(Ui.dp(this,88),Ui.dp(this,88)));
        TextView t=Ui.text(this,title,30,Ui.C_TEXT);
        t.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);
        root.addView(t);
        TextView s=Ui.text(this,sub,15,Ui.C_MUTED);
        s.setPadding(0,Ui.dp(this,6),0,Ui.dp(this,10));
        root.addView(s);
    }

    private String normalizePhone(String p){
        return p==null?"":p.trim().replaceAll("[\\s\\-()]","");
    }

    private void showRegister(){
        LinearLayout root=page();
        header(root,"YoYo","دردشة خاصة وسريعة بينكم ❤️");

        ImageView av=new ImageView(this);
        av.setBackgroundResource(R.drawable.avatar_circle);
        av.setClipToOutline(true);
        Avatar.into(av,pickedAvatar);
        LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(Ui.dp(this,82),Ui.dp(this,82));
        ap.gravity=Gravity.CENTER_HORIZONTAL;
        ap.topMargin=Ui.dp(this,8);
        root.addView(av,ap);
        av.setOnClickListener(v->imagePicker.launch(new String[]{"image/*"}));

        TextView photo=Ui.text(this,"اضغط على القلب لإضافة صورتك",13,Ui.C_MUTED);
        photo.setGravity(Gravity.CENTER);
        root.addView(photo);

        EditText name=input("اسمك");
        EditText phone=input("رقم الهاتف مع رمز الدولة، مثل +962 أو +20");
        phone.setInputType(InputType.TYPE_CLASS_PHONE);
        EditText pin=input("PIN من 4 أرقام");
        pin.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        root.addView(name);
        root.addView(phone);
        root.addView(pin);

        Button go=action("دخول إلى YoYo");
        root.addView(go);
        go.setOnClickListener(v->{
            String n=name.getText().toString().trim();
            String p=normalizePhone(phone.getText().toString());
            String k=pin.getText().toString().trim();

            if(n.length()<2||p.length()<7||k.length()!=4){
                Toast.makeText(this,"اكتب الاسم ورقم الهاتف وPIN من 4 أرقام",Toast.LENGTH_LONG).show();
                return;
            }

            go.setEnabled(false);
            go.setText("جاري ربط YoYo...");
            BackendClient.auth(this,p,n,k,pickedAvatar,(ok,token,error)->runOnUiThread(()->{
                if(ok){
                    Prefs.saveProfile(this,p,n,k,pickedAvatar,token);
                    enterApp();
                } else {
                    go.setEnabled(true);
                    go.setText("دخول إلى YoYo");
                    Toast.makeText(this,error==null?"تعذر الاتصال بالسيرفر":error,Toast.LENGTH_LONG).show();
                }
            }));
        });
        setContentView(root);
    }

    private void showUnlock(){
        LinearLayout root=page();
        header(root,"أهلاً "+Prefs.name(this),"أدخل PIN لفتح YoYo");
        EditText pin=input("PIN");
        pin.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        root.addView(pin);

        Button go=action("فتح التطبيق");
        root.addView(go);
        go.setOnClickListener(v->{
            String typed=pin.getText().toString();
            if(!Prefs.pin(this).equals(typed)){
                Toast.makeText(this,"PIN غير صحيح",Toast.LENGTH_SHORT).show();
                return;
            }

            if(!Prefs.token(this).isEmpty()){
                enterApp();
                return;
            }

            go.setEnabled(false);
            go.setText("تحديث الحساب...");
            String canonical=normalizePhone(Prefs.phone(this));
            BackendClient.auth(this,canonical,Prefs.name(this),typed,Prefs.avatar(this),(ok,token,error)->runOnUiThread(()->{
                if(ok){
                    Prefs.saveProfile(this,canonical,Prefs.name(this),typed,Prefs.avatar(this),token);
                    enterApp();
                }else{
                    go.setEnabled(true);
                    go.setText("فتح التطبيق");
                    Toast.makeText(this,error==null?"تعذر ربط الحساب":error,Toast.LENGTH_LONG).show();
                }
            }));
        });
        setContentView(root);
    }

    private void enterApp(){
        if(Build.VERSION.SDK_INT>=33 &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.POST_NOTIFICATIONS},5);
        }

        RealtimeService.start(this);

        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        LinearLayout top=new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(Ui.dp(this,18),Ui.dp(this,14),Ui.dp(this,18),Ui.dp(this,10));

        ImageView logo=new ImageView(this);
        logo.setImageResource(R.drawable.ic_heart);
        top.addView(logo,new LinearLayout.LayoutParams(Ui.dp(this,46),Ui.dp(this,46)));

        LinearLayout titles=new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setPadding(Ui.dp(this,10),0,0,0);
        top.addView(titles,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));

        TextView t=Ui.text(this,"YoYo",26,Ui.C_TEXT);
        t.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);
        titles.addView(t);
        titles.addView(Ui.text(this,"خاص • سريع • حتى 6 أشخاص",13,Ui.C_MUTED));
        root.addView(top);

        TextView cap=Ui.text(this,"المحادثات",18,Ui.C_TEXT);
        cap.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);
        cap.setPadding(Ui.dp(this,18),Ui.dp(this,8),0,Ui.dp(this,8));
        root.addView(cap);

        list=new RecyclerView(this);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter=new ContactAdapter(this,c->{
            Intent i=new Intent(this,ChatActivity.class)
                    .putExtra("phone",c.phone)
                    .putExtra("name",c.name)
                    .putExtra("avatar",c.avatar);
            startActivity(i);
        });
        list.setAdapter(adapter);
        root.addView(list,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1));

        empty=Ui.text(this,"أول ما المستخدم الثاني يسجل في نفس نسخة YoYo سيظهر هنا تلقائيًا.",15,Ui.C_MUTED);
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(Ui.dp(this,30),Ui.dp(this,20),Ui.dp(this,30),Ui.dp(this,30));
        root.addView(empty);

        setContentView(root);
        refresh();
    }

    private void refresh(){
        if(adapter==null)return;
        java.util.List<EventStore.Contact> c=EventStore.contacts(this);
        adapter.setData(c);
        if(empty!=null)empty.setVisibility(c.isEmpty()?View.VISIBLE:View.GONE);
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
