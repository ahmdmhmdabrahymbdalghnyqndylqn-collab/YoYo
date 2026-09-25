package com.yoyo.privatechat;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class EventStore {
    public static class Contact {
        public String phone, name, avatar;
        public long lastSeen;
        public Contact() {}
        public Contact(String p,String n,String a,long l){phone=p;name=n;avatar=a;lastSeen=l;}
    }
    public static class Msg {
        public String id, from, to, text, status;
        public long ts;
        public Msg() {}
        public Msg(String id,String from,String to,String text,long ts,String status){this.id=id;this.from=from;this.to=to;this.text=text;this.ts=ts;this.status=status;}
    }

    private static final Gson G = new Gson();
    private static SharedPreferences sp(Context c){ return c.getSharedPreferences("yoyo_store", Context.MODE_PRIVATE); }

    public static synchronized List<Contact> contacts(Context c){
        Type t = new TypeToken<ArrayList<Contact>>(){}.getType();
        List<Contact> list = G.fromJson(sp(c).getString("contacts","[]"), t);
        if(list==null) list=new ArrayList<>();
        Collections.sort(list, (a,b)->Long.compare(b.lastSeen,a.lastSeen));
        return list;
    }

    public static synchronized void upsertContact(Context c, Contact nc){
        if(nc.phone == null || nc.phone.equals(Prefs.phone(c))) return;
        List<Contact> list=contacts(c); boolean found=false;
        for(Contact x:list) if(x.phone.equals(nc.phone)){x.name=nc.name;x.avatar=nc.avatar;x.lastSeen=Math.max(x.lastSeen,nc.lastSeen);found=true;break;}
        if(!found && list.size()<5) list.add(nc);
        sp(c).edit().putString("contacts",G.toJson(list)).apply();
    }

    public static synchronized List<Msg> messages(Context c,String other){
        Type t = new TypeToken<ArrayList<Msg>>(){}.getType();
        List<Msg> list=G.fromJson(sp(c).getString("m_"+safe(other),"[]"),t);
        if(list==null) list=new ArrayList<>();
        Collections.sort(list, Comparator.comparingLong(m->m.ts));
        return list;
    }

    public static synchronized void addMessage(Context c,Msg m){
        String me=Prefs.phone(c); String other=me.equals(m.from)?m.to:m.from;
        if(other==null || other.isEmpty()) return;
        List<Msg> list=messages(c,other);
        for(Msg x:list) if(x.id!=null && x.id.equals(m.id)) return;
        list.add(m); if(list.size()>500) list=new ArrayList<>(list.subList(list.size()-500,list.size()));
        sp(c).edit().putString("m_"+safe(other),G.toJson(list)).apply();
    }

    public static synchronized void markSent(Context c,String other,String id){
        List<Msg> list=messages(c,other);
        for(Msg x:list) if(id.equals(x.id)) x.status="sent";
        sp(c).edit().putString("m_"+safe(other),G.toJson(list)).apply();
    }

    private static String safe(String s){ return s.replaceAll("[^0-9A-Za-z]", "_"); }
}
