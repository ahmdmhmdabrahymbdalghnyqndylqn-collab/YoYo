package com.yoyo.privatechat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public final class Avatar {
    private Avatar(){}
    public static String fromUri(Context c, Uri uri){
        try(InputStream in=c.getContentResolver().openInputStream(uri)){
            Bitmap b=BitmapFactory.decodeStream(in); if(b==null)return "";
            int size=Math.min(b.getWidth(),b.getHeight());
            int x=(b.getWidth()-size)/2,y=(b.getHeight()-size)/2;
            Bitmap crop=Bitmap.createBitmap(b,x,y,size,size);
            Bitmap small=Bitmap.createScaledBitmap(crop,64,64,true);
            ByteArrayOutputStream out=new ByteArrayOutputStream(); small.compress(Bitmap.CompressFormat.JPEG,45,out);
            return Base64.encodeToString(out.toByteArray(),Base64.NO_WRAP);
        }catch(Exception e){return "";}
    }
    public static void into(ImageView v,String base64){
        try{
            if(base64!=null&&!base64.isEmpty()){
                byte[] data=Base64.decode(base64,Base64.DEFAULT); Bitmap b=BitmapFactory.decodeByteArray(data,0,data.length);
                v.setImageBitmap(b); v.setScaleType(ImageView.ScaleType.CENTER_CROP); return;
            }
        }catch(Exception ignored){}
        v.setImageResource(R.drawable.ic_heart); v.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
    }
}
