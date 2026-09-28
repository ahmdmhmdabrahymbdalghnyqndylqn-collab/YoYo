package com.yoyo.privatechat;
import android.app.AlertDialog;import android.os.Build;import android.text.InputType;import android.text.method.PasswordTransformationMethod;import android.view.*;import android.view.inputmethod.*;import android.widget.*;
final class EntryScreen {
 final MainActivity a; EditText name,password; TextView error,go,nameErrorLabel,passwordErrorLabel; boolean busy=false;String invitation=BuildConfig.OWNER_ENROLLMENT;
 EntryScreen(MainActivity activity){a=activity;}
 void show(){
  ScrollView scroll=new ScrollView(a);scroll.setFillViewport(true);LinearLayout p=Ui.col(a);Ui.pad(p,28,24,28,24);scroll.addView(p);Ui.space(p,22);
  LinearLayout mark=Ui.row(a);mark.setGravity(Gravity.CENTER);mark.addView(Ui.icon(a,"heart",Ui.C_ACCENT,64,null));p.addView(mark);
  TextView logo=Ui.bold(a,"YOYO",42,Ui.C_TEXT);logo.setGravity(Gravity.CENTER);logo.setLetterSpacing(.07f);p.addView(logo);
  TextView tagline=Ui.text(a,"مسافة أقرب. حكاية إلنا.",16,Ui.C_MUTED);tagline.setGravity(Gravity.CENTER);p.addView(tagline);Ui.space(p,32);
  p.addView(Ui.bold(a,"أهلاً فيك",30,Ui.C_TEXT));p.addView(Ui.text(a,"اسمك وكلمة مرورك، وبس.",17,Ui.C_MUTED));Ui.space(p,24);
  p.addView(Ui.bold(a,"الاسم",16,Ui.C_TEXT));Ui.space(p,7);name=Ui.input(a,"مثال: أحمد قنديل",false);name.setId(View.generateViewId());name.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_WORDS);name.setTypeface(Ui.text(a,"",16,Ui.C_TEXT).getTypeface());name.setImeOptions(EditorInfo.IME_ACTION_NEXT);name.setText(Prefs.get(a,"login_name"));p.addView(name,new LinearLayout.LayoutParams(-1,Ui.dp(a,56)));
  nameErrorLabel=fieldError(p);
  p.addView(Ui.text(a,"العربي والإنجليزي والمسافات مسموحة",12,Ui.C_MUTED));Ui.space(p,18);
  p.addView(Ui.bold(a,"كلمة المرور",16,Ui.C_TEXT));Ui.space(p,7);password=Ui.input(a,"8 أحرف أو أرقام على الأقل",true);password.setId(View.generateViewId());password.setImeOptions(EditorInfo.IME_ACTION_DONE);p.addView(password,new LinearLayout.LayoutParams(-1,Ui.dp(a,56)));
  passwordErrorLabel=fieldError(p);
  if(Build.VERSION.SDK_INT>=26){name.setAutofillHints(View.AUTOFILL_HINT_USERNAME);password.setAutofillHints(View.AUTOFILL_HINT_PASSWORD);}
  CheckBox eye=new CheckBox(a);eye.setText("إظهار كلمة المرور");eye.setTextColor(Ui.C_MUTED);eye.setOnCheckedChangeListener((v,show)->{int at=password.getSelectionStart();password.setTransformationMethod(show?null:PasswordTransformationMethod.getInstance());password.setSelection(Math.max(0,at));});p.addView(eye);Ui.space(p,8);
  error=Ui.text(a,"",14,Ui.C_ACCENT);error.setVisibility(View.GONE);p.addView(error);Ui.space(p,8);
  go=Ui.button(a,"دخول",this::submit);p.addView(go,new LinearLayout.LayoutParams(-1,Ui.dp(a,56)));password.setOnEditorActionListener((v,action,event)->{if(action==EditorInfo.IME_ACTION_DONE){submit();return true;}return false;});Ui.space(p,18);
  TextView help=Ui.text(a,BuildConfig.OWNER_ENROLLMENT.isEmpty()?"أول مرة؟ بعد كتابة اسمك وكلمة مرورك، الصق رمز الدعوة الذي وصلك.":"أول مرة؟ سننشئ حسابك تلقائيًا. بعد الدخول، تقدر تدعو الشخص الثاني.",13,Ui.C_MUTED);help.setGravity(Gravity.CENTER);p.addView(help);Ui.page(a,scroll);
 }
 TextView fieldError(LinearLayout p){TextView t=Ui.text(a,"",14,Ui.C_ACCENT);t.setVisibility(View.GONE);t.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);p.addView(t);return t;}
 void showFieldError(TextView label,EditText field,String message){label.setText(message);label.setVisibility(View.VISIBLE);field.requestFocus();}
 static String nameError(String s){int n=s.trim().length();if(n<2)return "اكتب اسمك من حرفين على الأقل";if(n>60)return "الاسم طويل؛ الحد 60 حرفًا";return null;}
 static String passwordError(String s){if(s.length()<8)return "كلمة المرور قصيرة؛ اكتب 8 أحرف أو أرقام على الأقل";if(s.length()>128)return "كلمة المرور طويلة؛ الحد 128 حرفًا";return null;}
 void submit(){submit(false);}
 void submit(boolean recover){if(busy)return;nameErrorLabel.setVisibility(View.GONE);passwordErrorLabel.setVisibility(View.GONE);error.setVisibility(View.GONE);String ne=nameError(name.getText().toString()),pe=passwordError(password.getText().toString());if(ne!=null){showFieldError(nameErrorLabel,name,ne);return;}if(pe!=null){showFieldError(passwordErrorLabel,password,pe);return;}
  busy=true;go.setEnabled(false);go.setText("لحظة، عم ندخّلك…");final String n=name.getText().toString(),p=password.getText().toString();
  Api.entry(a,n,p,invitation,recover,(field,message)->{if(a.isFinishing()||a.isDestroyed())return;busy=false;go.setEnabled(true);go.setText("دخول");if(message==null){((InputMethodManager)a.getSystemService(android.content.Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(password.getWindowToken(),0);a.enter();return;}
   if("recovery".equals(field)){askRecovery();}else if("name".equals(field)){showFieldError(nameErrorLabel,name,message);}else if("password".equals(field)){showFieldError(passwordErrorLabel,password,message);}else if("invite".equals(field)&&BuildConfig.OWNER_ENROLLMENT.isEmpty())askInvite(message);else{error.setText("invite".equals(field)?"نسخة المالك تم تفعيلها مسبقًا. استخدم اسم حسابك وكلمة مروره على جهازك الأصلي.":message);error.setVisibility(View.VISIBLE);}
  });
 }
 void askRecovery(){new AlertDialog.Builder(a).setTitle("استرجاع حسابك")
  .setMessage("كلمة المرور صحيحة. هل تريد استخدام حسابك على هذا التثبيت؟\n\nسيبقى اسمك وصورتك وصلاحياتك كما هي. تتوقف الجلسات السابقة عن التجدد. الرسائل القديمة المشفّرة لا يمكن استرجاعها بدون مفتاح التثبيت القديم. قد يحتاج الشخص الآخر لتأكيد تغيير جهازك.\n\nإذا كان حسابك يعمل على جهاز آخر، لا تسترجعه إلا إذا أردت نقله إلى هنا.")
  .setPositiveButton("استرجاع حسابي",(d,w)->submit(true)).setNegativeButton("إلغاء",null).show();}
 void askInvite(String message){EditText code=Ui.input(a,"الصق الرمز أو رسالة الدعوة كاملة",false);code.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);code.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);code.setText(invitation);LinearLayout box=Ui.col(a);Ui.pad(box,24,6,24,8);box.addView(Ui.text(a,message,15,Ui.C_MUTED));Ui.space(box,12);box.addView(code,new LinearLayout.LayoutParams(-1,Ui.dp(a,56)));AlertDialog d=new AlertDialog.Builder(a).setTitle("تفعيل الحساب — مرة واحدة").setView(box).setPositiveButton("تفعيل ودخول",null).setNegativeButton("رجوع",null).create();d.setOnShowListener(x->d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String s=Invitation.extract(code.getText().toString());if(s.isEmpty()){code.setError("هذا ليس رمز دعوة. الصق الرمز المؤلف من 12 حرفًا ورقمًا أو رسالة الدعوة كاملة");return;}invitation=s;d.dismiss();submit();}));d.show();}
}
