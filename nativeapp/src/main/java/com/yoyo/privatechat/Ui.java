package com.yoyo.privatechat;
import android.app.Activity;import android.content.Context;import android.graphics.*;import android.graphics.drawable.GradientDrawable;import android.view.*;import android.widget.*;import androidx.core.view.*;
public final class Ui{
 public static final int C_ACCENT=Color.rgb(171,35,66),C_TEXT=Color.rgb(42,25,32),C_MUTED=Color.rgb(137,120,129),C_BG=Color.rgb(255,252,253),C_SOFT=Color.rgb(251,237,241),C_LINE=Color.rgb(241,232,236);
 public static int dp(Context c,int n){return (int)(n*c.getResources().getDisplayMetrics().density+.5f);}public static TextView text(Context c,String s,float z,int co){TextView t=new TextView(c);t.setText(s);t.setTextSize(z);t.setTextColor(co);t.setTypeface(android.graphics.Typeface.create("sans",android.graphics.Typeface.NORMAL));return t;}public static TextView bold(Context c,String s,float z,int co){TextView t=text(c,s,z,co);t.setTypeface(t.getTypeface(),Typeface.BOLD);return t;}
 public static void round(View v,int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(v.getContext(),radius));v.setBackground(d);}public static LinearLayout col(Context c){LinearLayout v=new LinearLayout(c);v.setOrientation(LinearLayout.VERTICAL);return v;}public static LinearLayout row(Context c){LinearLayout v=new LinearLayout(c);v.setGravity(Gravity.CENTER_VERTICAL);return v;}public static void pad(View v,int a,int b,int c,int d){Context x=v.getContext();v.setPadding(dp(x,a),dp(x,b),dp(x,c),dp(x,d));}public static void space(LinearLayout v,int n){v.addView(new View(v.getContext()),new LinearLayout.LayoutParams(1,dp(v.getContext(),n)));}
 public static TextView button(Context c,String label,Runnable r){TextView b=bold(c,label,17,Color.WHITE);b.setGravity(Gravity.CENTER);round(b,C_ACCENT,18);b.setMinHeight(dp(c,54));b.setOnClickListener(v->r.run());return b;}public static EditText input(Context c,String hint,boolean pass){EditText e=new EditText(c);e.setTextSize(16);e.setTextColor(C_TEXT);e.setHintTextColor(C_MUTED);e.setHint(hint);e.setSingleLine(true);e.setInputType(pass?129:1);e.setTypeface(text(c,"",16,C_TEXT).getTypeface());pad(e,18,10,18,10);round(e,C_SOFT,16);return e;}
 public static Icon icon(Context c,String name,int color,int size,Runnable r){Icon i=new Icon(c,name,color);i.setLayoutParams(new LinearLayout.LayoutParams(dp(c,size),dp(c,size)));i.setContentDescription(name);if(r!=null)i.setOnClickListener(v->r.run());return i;}
 public static void page(Activity a,View v){v.setBackgroundColor(C_BG);v.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);WindowCompat.setDecorFitsSystemWindows(a.getWindow(),true);a.getWindow().setStatusBarColor(C_BG);a.getWindow().setNavigationBarColor(Color.WHITE);a.setContentView(v);WindowCompat.getInsetsController(a.getWindow(),a.getWindow().getDecorView()).setAppearanceLightStatusBars(true);ViewCompat.setOnApplyWindowInsetsListener(v,(w,in)->{androidx.core.graphics.Insets x=in.getInsets(WindowInsetsCompat.Type.systemBars());w.setPadding(w.getPaddingLeft(),x.top,w.getPaddingRight(),x.bottom);return in;});}public static void toast(Context c,String s){Toast.makeText(c,s,Toast.LENGTH_LONG).show();}
 public static class Icon extends View{
  String kind;int color;Paint p=new Paint(3);public Icon(Context c,String k,int co){super(c);kind=k;color=co;}
  protected void onDraw(Canvas canvas){super.onDraw(canvas);canvas.save();float z=Math.min(getWidth(),getHeight())/32f;canvas.translate((getWidth()-24*z)/2,(getHeight()-24*z)/2);canvas.scale(z,z);p.setColor(color);p.setStrokeWidth(1.65f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);p.setStyle(Paint.Style.STROKE);Path x=new Path();switch(kind){
  case "chat":canvas.drawRoundRect(3,3,21,18,5,5,p);x.moveTo(8,18);x.lineTo(5,22);x.lineTo(5,17);canvas.drawPath(x,p);canvas.drawLine(7,8,17,8,p);canvas.drawLine(7,12,14,12,p);break;
  case "story":canvas.drawArc(3,3,21,21,20,105,false,p);canvas.drawArc(3,3,21,21,140,105,false,p);canvas.drawArc(3,3,21,21,260,105,false,p);canvas.drawCircle(12,12,4,p);break;
  case "phone":x.moveTo(6,3);x.cubicTo(3,2,2,5,4,10);x.cubicTo(6,16,13,21,18,21);x.cubicTo(22,21,23,17,20,16);x.lineTo(17,14);x.lineTo(14,16);x.cubicTo(11,14,9,12,8,9);x.lineTo(10,7);x.close();canvas.drawPath(x,p);break;
  case "settings":canvas.drawCircle(12,12,7,p);canvas.drawCircle(12,12,2.6f,p);for(int j=0;j<8;j++){canvas.save();canvas.rotate(j*45,12,12);canvas.drawLine(12,2,12,4,p);canvas.restore();}break;
  case "heart":p.setStyle(Paint.Style.FILL);x.moveTo(12,21);x.cubicTo(-5,10,5,-2,12,6);x.cubicTo(19,-2,29,10,12,21);canvas.drawPath(x,p);break;
  case "back":x.moveTo(9,5);x.lineTo(16,12);x.lineTo(9,19);canvas.drawPath(x,p);break;
  case "plus":canvas.drawLine(12,5,12,19,p);canvas.drawLine(5,12,19,12,p);break;
  case "mic":canvas.drawRoundRect(9,3,15,15,3,3,p);canvas.drawArc(6,8,18,19,0,180,false,p);canvas.drawLine(12,19,12,23,p);break;
  case "send":x.moveTo(3,3);x.lineTo(22,12);x.lineTo(3,21);x.lineTo(7,12);x.close();canvas.drawPath(x,p);canvas.drawLine(7,12,17,12,p);break;
  case "speaker":x.moveTo(3,9);x.lineTo(8,9);x.lineTo(13,4);x.lineTo(13,20);x.lineTo(8,15);x.lineTo(3,15);x.close();canvas.drawPath(x,p);canvas.drawArc(9,5,22,19,-65,130,false,p);break;
  case "bluetooth":x.moveTo(7,6);x.lineTo(18,16);x.lineTo(12,22);x.lineTo(12,2);x.lineTo(18,8);x.lineTo(7,18);canvas.drawPath(x,p);break;
  case "play":x.moveTo(8,4);x.lineTo(20,12);x.lineTo(8,20);x.close();canvas.drawPath(x,p);break;
  default:canvas.drawCircle(12,12,8,p);canvas.drawLine(12,7,12,12,p);canvas.drawLine(12,16,12,17,p);
  }canvas.restore();}
 }
}
