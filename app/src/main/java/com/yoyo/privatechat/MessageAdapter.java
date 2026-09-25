package com.yoyo.privatechat;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.H> {
    private final Context c;
    private final String me;
    private List<EventStore.Msg> data=new ArrayList<>();

    public MessageAdapter(Context c,String me){
        this.c=c;
        this.me=me;
    }

    public void setData(List<EventStore.Msg> d){
        data=d;
        notifyDataSetChanged();
    }

    @NonNull
    @Override public H onCreateViewHolder(@NonNull ViewGroup p,int vt){
        LinearLayout outer=new LinearLayout(c);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setPadding(Ui.dp(c,10),Ui.dp(c,3),Ui.dp(c,10),Ui.dp(c,3));
        outer.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        outer.setLayoutParams(new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout bubble=new LinearLayout(c);
        bubble.setOrientation(LinearLayout.VERTICAL);
        bubble.setPadding(Ui.dp(c,12),Ui.dp(c,8),Ui.dp(c,12),Ui.dp(c,7));

        LinearLayout.LayoutParams bubbleParams=new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        outer.addView(bubble,bubbleParams);

        TextView msg=Ui.text(c,"",16,Ui.C_TEXT);
        msg.setMaxWidth(Ui.dp(c,290));
        msg.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        bubble.addView(msg);

        TextView meta=Ui.text(c,"",11,Ui.C_MUTED);
        meta.setGravity(Gravity.RIGHT);
        bubble.addView(meta);

        return new H(outer,bubble,msg,meta);
    }

    @Override public void onBindViewHolder(@NonNull H h,int pos){
        EventStore.Msg m=data.get(pos);
        boolean mine=me.equals(m.from);

        LinearLayout.LayoutParams bp=(LinearLayout.LayoutParams)h.bubble.getLayoutParams();
        // Physical sides, independent of Arabic/RTL layout:
        // my messages = right, other person = left.
        bp.gravity=mine ? Gravity.RIGHT : Gravity.LEFT;
        h.bubble.setLayoutParams(bp);

        h.bubble.setBackgroundResource(mine ? R.drawable.bubble_out : R.drawable.bubble_in);
        h.msg.setText(m.text);

        String tm=new SimpleDateFormat("HH:mm",Locale.getDefault()).format(new Date(m.ts));
        String tick="";
        if(mine){
            if("delivered".equals(m.status)) tick="  ✓✓";
            else if("sent".equals(m.status)) tick="  ✓";
            else tick="  ⏳";
        }
        h.meta.setText(tm+tick);
    }

    @Override public int getItemCount(){
        return data.size();
    }

    static class H extends RecyclerView.ViewHolder{
        LinearLayout bubble;
        TextView msg,meta;

        H(View v,LinearLayout b,TextView m,TextView t){
            super(v);
            bubble=b;
            msg=m;
            meta=t;
        }
    }
}
