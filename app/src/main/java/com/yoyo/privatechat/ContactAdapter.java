package com.yoyo.privatechat;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.H> {
    public interface Click { void onClick(EventStore.Contact c); }
    private final Context c; private final Click click; private List<EventStore.Contact> data=new ArrayList<>();
    public ContactAdapter(Context c,Click click){this.c=c;this.click=click;}
    public void setData(List<EventStore.Contact> d){data=d;notifyDataSetChanged();}
    @NonNull @Override public H onCreateViewHolder(@NonNull ViewGroup p,int vt){
        LinearLayout row=new LinearLayout(c);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(Ui.dp(c,18),Ui.dp(c,12),Ui.dp(c,18),Ui.dp(c,12));
        ImageView avatar=new ImageView(c);avatar.setBackgroundResource(R.drawable.avatar_circle);avatar.setClipToOutline(true);row.addView(avatar,new LinearLayout.LayoutParams(Ui.dp(c,56),Ui.dp(c,56)));
        LinearLayout mid=new LinearLayout(c);mid.setOrientation(LinearLayout.VERTICAL);mid.setPadding(Ui.dp(c,14),0,0,0);row.addView(mid,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));
        TextView name=Ui.text(c,"",18,Ui.C_TEXT);name.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);mid.addView(name);
        TextView sub=Ui.text(c,"جاهز للدردشة",14,Ui.C_MUTED);mid.addView(sub);
        TextView time=Ui.text(c,"",12,Ui.C_MUTED);row.addView(time);
        return new H(row,avatar,name,sub,time);
    }
    @Override public void onBindViewHolder(@NonNull H h,int pos){EventStore.Contact x=data.get(pos);h.name.setText(x.name==null||x.name.isEmpty()?x.phone:x.name);h.sub.setText(x.phone);Avatar.into(h.avatar,x.avatar);h.time.setText(new SimpleDateFormat("HH:mm",Locale.getDefault()).format(new Date(x.lastSeen)));h.itemView.setOnClickListener(v->click.onClick(x));}
    @Override public int getItemCount(){return data.size();}
    static class H extends RecyclerView.ViewHolder{ImageView avatar;TextView name,sub,time;H(View v,ImageView a,TextView n,TextView s,TextView t){super(v);avatar=a;name=n;sub=s;time=t;}}
}
