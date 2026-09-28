package com.yoyo.privatechat;
import java.util.Locale;import java.util.regex.*;
/** The APK URL is never treated as an invitation. */
final class Invitation {
 private static final Pattern CODE=Pattern.compile("[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{12}");
 private static final Pattern LABEL=Pattern.compile("(?:رمز الدعوة|invite code)\\s*[:：]\\s*([A-Z2-9]{12})(?![A-Z0-9])",Pattern.CASE_INSENSITIVE);
 static String extract(String text){String s=text.replaceAll("[\\u200e\\u200f\\u202a-\\u202e\\u2066-\\u2069]","").trim().toUpperCase(Locale.ROOT);if(CODE.matcher(s).matches())return s;Matcher m=LABEL.matcher(s);return m.find()&&CODE.matcher(m.group(1)).matches()?m.group(1):"";}
 static String message(String code){return "دعوتك ليويو ♥\nرمز الدعوة: "+code+"\n\nرابط تنزيل التطبيق:\n"+BuildConfig.GUEST_DOWNLOAD_URL+"\n\nبعد التثبيت: اكتب اسمك وكلمة مرور من 8 أحرف أو أرقام، ثم الصق الرمز أو هذه الرسالة كاملة في خانة رمز الدعوة.";}
}
