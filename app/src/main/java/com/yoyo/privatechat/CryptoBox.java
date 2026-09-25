package com.yoyo.privatechat;

import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class CryptoBox {
    private static final byte[] KEY = Base64.decode("STtV2UliJZtikj5UnoHEwrpDAO+fqVShgpkf7Gs7uTA=", Base64.DEFAULT);
    private static final SecureRandom RNG = new SecureRandom();

    private CryptoBox() {}

    public static String encrypt(String plain) throws Exception {
        byte[] iv = new byte[12];
        RNG.nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(KEY, "AES"), new GCMParameterSpec(128, iv));
        byte[] compressed = gzip(plain.getBytes(StandardCharsets.UTF_8));
        byte[] enc = cipher.doFinal(compressed);
        byte[] out = new byte[iv.length + enc.length];
        System.arraycopy(iv, 0, out, 0, iv.length);
        System.arraycopy(enc, 0, out, iv.length, enc.length);
        return Base64.encodeToString(out, Base64.NO_WRAP | Base64.URL_SAFE);
    }

    public static String decrypt(String encoded) throws Exception {
        byte[] all = Base64.decode(encoded, Base64.NO_WRAP | Base64.URL_SAFE);
        if (all.length < 29) throw new IllegalArgumentException("bad payload");
        byte[] iv = new byte[12];
        byte[] enc = new byte[all.length - 12];
        System.arraycopy(all, 0, iv, 0, 12);
        System.arraycopy(all, 12, enc, 0, enc.length);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(KEY, "AES"), new GCMParameterSpec(128, iv));
        return new String(gunzip(cipher.doFinal(enc)), StandardCharsets.UTF_8);
    }

    private static byte[] gzip(byte[] src) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPOutputStream gz = new GZIPOutputStream(out)) { gz.write(src); }
        return out.toByteArray();
    }

    private static byte[] gunzip(byte[] src) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPInputStream gz = new GZIPInputStream(new java.io.ByteArrayInputStream(src))) {
            byte[] buf = new byte[1024];
            int n;
            while ((n = gz.read(buf)) > 0) out.write(buf, 0, n);
        }
        return out.toByteArray();
    }
}
