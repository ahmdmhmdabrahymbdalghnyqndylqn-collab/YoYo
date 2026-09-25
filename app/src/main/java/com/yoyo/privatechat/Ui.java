package com.yoyo.privatechat;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class Ui {
    private Ui(){}
    public static int dp(Context c,int v){ return (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,v,c.getResources().getDisplayMetrics()); }
    public static TextView text(Context c,String s,float sp,int color){ TextView t=new TextView(c);t.setText(s);t.setTextSize(sp);t.setTextColor(color);return t; }
    public static LinearLayout.LayoutParams lp(int w,int h){ return new LinearLayout.LayoutParams(w,h); }
    public static LinearLayout.LayoutParams lpW(float weight){ return new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,weight); }
    public static int C_ACCENT=Color.rgb(232,93,117), C_TEXT=Color.rgb(22,22,22), C_MUTED=Color.rgb(107,107,107), C_BG=Color.WHITE;
}
