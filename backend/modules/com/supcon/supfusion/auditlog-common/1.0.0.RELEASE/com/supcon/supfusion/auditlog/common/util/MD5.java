package com.supcon.supfusion.auditlog.common.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class MD5 {
    private static int DIGITS_SIZE = 16;
    private static char[] digits = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    private static Map<Character, Integer> rDigits = new HashMap(16);
    private static MD5 me;
    private MessageDigest mHasher;
    private ReentrantLock opLock = new ReentrantLock();

    private MD5() {
        try {
            this.mHasher = MessageDigest.getInstance("md5");
        } catch (Exception var2) {
            throw new RuntimeException(var2);
        }
    }

    public static MD5 getInstance() {
        return me;
    }

    public String getMD5String(String content) {
        return this.bytes2string(this.hash(content));
    }

    public String getMD5String(byte[] content) {
        return this.bytes2string(this.hash(content));
    }

    public byte[] getMD5Bytes(byte[] content) {
        return this.hash(content);
    }

    public byte[] hash(String str) {
        this.opLock.lock();

        byte[] var3;
        try {
            byte[] bt = this.mHasher.digest(str.getBytes("UTF-8"));
            if (null == bt || bt.length != DIGITS_SIZE) {
                throw new IllegalArgumentException("md5 need");
            }

            var3 = bt;
        } catch (UnsupportedEncodingException var7) {
            throw new RuntimeException("unsupported utf-8 encoding", var7);
        } finally {
            this.opLock.unlock();
        }

        return var3;
    }

    public byte[] hash(byte[] data) {
        this.opLock.lock();

        byte[] var3;
        try {
            byte[] bt = this.mHasher.digest(data);
            if (null == bt || bt.length != DIGITS_SIZE) {
                throw new IllegalArgumentException("md5 need");
            }

            var3 = bt;
        } finally {
            this.opLock.unlock();
        }

        return var3;
    }

    public String bytes2string(byte[] bt) {
        int l = bt.length;
        char[] out = new char[l << 1];
        int i = 0;

        for(int var5 = 0; i < l; ++i) {
            out[var5++] = digits[(240 & bt[i]) >>> 4];
            out[var5++] = digits[15 & bt[i]];
        }

        return new String(out);
    }

    static {
        for(int i = 0; i < digits.length; ++i) {
            rDigits.put(digits[i], i);
        }

        me = new MD5();
    }
}

