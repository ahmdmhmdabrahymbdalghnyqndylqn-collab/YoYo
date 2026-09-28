package com.yoyo.privatechat;
import android.app.Activity;import android.content.*;import android.graphics.Color;import android.os.Bundle;import android.view.View;import android.widget.*;
/** A platform-only recovery view remains available if AppCompat startup fails. */
public final class LaunchActivity extends Activity {
 @Override public void onCreate(Bundle state){super.onCreate(state);String report=CrashReport.get(this);if(report.isEmpty()){openApp();return;}
  ScrollView scroll=new ScrollView(this);LinearLayout p=new LinearLayout(this);p.setOrientation(LinearLayout.VERTICAL);p.setPadding(32,48,32,32);p.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);p.setBackgroundColor(Color.WHITE);scroll.addView(p);
  TextView title=new TextView(this);title.setText("خلّينا نحل مشكلة التشغيل");title.setTextSize(24);title.setTextColor(Color.rgb(171,35,66));p.addView(title);
  TextView intro=new TextView(this);intro.setText("توقّف يويو في المحاولة السابقة. انسخ تقرير الخطأ وأرسله في محادثة الدعم لتحديد السبب. التقرير يحتوي نوع الهاتف وإصدار أندرويد وموقع الخطأ فقط، ولا يحتوي رسائلك أو كلمة مرورك.");intro.setTextSize(17);intro.setTextColor(Color.DKGRAY);intro.setPadding(0,24,0,24);p.addView(intro);
  Button copy=new Button(this);copy.setText("نسخ تقرير الخطأ");copy.setOnClickListener(v->{((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("YOYO diagnostic",report));Toast.makeText(this,"تم نسخ التقرير",Toast.LENGTH_LONG).show();});p.addView(copy);
  Button retry=new Button(this);retry.setText("إعادة المحاولة");retry.setOnClickListener(v->{CrashReport.clear(this);openApp();});p.addView(retry);
  TextView details=new TextView(this);details.setText(report);details.setTextColor(Color.DKGRAY);details.setTextSize(12);details.setTextIsSelectable(true);details.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);p.addView(details);setContentView(scroll);
 }
 void openApp(){startActivity(new Intent(this,MainActivity.class));finish();}
}
