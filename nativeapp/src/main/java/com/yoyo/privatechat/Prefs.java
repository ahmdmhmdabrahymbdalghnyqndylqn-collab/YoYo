package com.yoyo.privatechat;
import android.content.*;import org.json.*;
public final class Prefs{
 static SharedPreferences sp(Context c){return c.getSharedPreferences("yoyo_native_v2",0);}
 public static String get(Context c,String k){return sp(c).getString(k,"");}public static void put(Context c,String k,String v){sp(c).edit().putString(k,v).apply();}
 public static String phone(Context c){return get(c,"id");}public static String name(Context c){return get(c,"name");}public static String avatar(Context c){return get(c,"avatar");}public static boolean hasProfile(Context c){return !get(c,"refresh").isEmpty();}
 public static void session(Context c,JSONObject s)throws Exception{put(c,"token",s.getString("access_token"));put(c,"refresh",s.getString("refresh_token"));put(c,"id",s.getJSONObject("user").getString("id"));put(c,"expires",String.valueOf(System.currentTimeMillis()+s.optLong("expires_in",3600)*1000));}public static void clear(Context c){sp(c).edit().clear().apply();}
}
