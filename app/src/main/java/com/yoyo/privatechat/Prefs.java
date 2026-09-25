package com.yoyo.privatechat;

import android.content.Context;
import android.content.SharedPreferences;

public final class Prefs {
    private static final String P = "yoyo_prefs";
    private Prefs() {}
    private static SharedPreferences sp(Context c){ return c.getSharedPreferences(P, Context.MODE_PRIVATE); }

    public static boolean hasProfile(Context c){ return !sp(c).getString("phone", "").isEmpty(); }
    public static String phone(Context c){ return sp(c).getString("phone", ""); }
    public static String name(Context c){ return sp(c).getString("name", "YoYo"); }
    public static String pin(Context c){ return sp(c).getString("pin", ""); }
    public static String avatar(Context c){ return sp(c).getString("avatar", ""); }
    public static String token(Context c){ return sp(c).getString("token", ""); }

    public static void saveProfile(Context c, String phone, String name, String pin, String avatar){
        sp(c).edit()
                .putString("phone", phone)
                .putString("name", name)
                .putString("pin", pin)
                .putString("avatar", avatar == null ? "" : avatar)
                .apply();
    }

    public static void saveProfile(Context c, String phone, String name, String pin, String avatar, String token){
        sp(c).edit()
                .putString("phone", phone)
                .putString("name", name)
                .putString("pin", pin)
                .putString("avatar", avatar == null ? "" : avatar)
                .putString("token", token == null ? "" : token)
                .apply();
    }

    public static void saveToken(Context c,String token){
        sp(c).edit().putString("token",token==null?"":token).apply();
    }
}
