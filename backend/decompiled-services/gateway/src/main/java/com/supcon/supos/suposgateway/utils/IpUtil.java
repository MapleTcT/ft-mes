/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.alibaba.fastjson.JSON
 *  com.alibaba.fastjson.JSONObject
 *  org.springframework.http.server.reactive.ServerHttpRequest
 */
package com.supcon.supos.suposgateway.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import java.util.Base64;
import org.springframework.http.server.reactive.ServerHttpRequest;

public class IpUtil {
    public static String getCurrentIp(ServerHttpRequest request) {
        String ip = request.getHeaders().getFirst("X-Real-IP");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeaders().getFirst("x-forwarded-for");
            String[] ars = ip.split(",");
            ip = ars[ars.length - 1].trim();
        }
        if (ip != null && ip.equals("0:0:0:0:0:0:0:1")) {
            ip = "127.0.0.1";
        }
        return ip;
    }

    public static void main(String[] args) {
        String[] split = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJwelhpeXRYVHZVVmx6YTloUC1tZTdEUXNLbXlvRkxLSmFHeE5HMEQzdUpNIn0.eyJleHAiOjE1OTg0ODIxMTIsImlhdCI6MTU5ODQzODkxMiwianRpIjoiYzc5NzgxMDItYTg2NC00ZGM2LThiMjctMzhlZjNkZGU2ZmU1IiwiaXNzIjoiaHR0cDovLzEwLjMwLjQ0LjY5OjkwMTQvYXV0aC9yZWFsbXMvc3Vwb3MiLCJhdWQiOiJhY2NvdW50Iiwic3ViIjoiZjpkMmEzYjUxMy0zODdhLTQ2M2YtYTk5Ni0zM2QxMTgwNWE5OTQ6YWRtaW4iLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJtcy1jb250ZW50LXNhbXBsZSIsInNlc3Npb25fc3RhdGUiOiIxNjAyZTQ5MS1hODQ2LTRhYTAtYTljYi00MTgxNjhiODA4MjUiLCJhY3IiOiIxIiwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbIm9mZmxpbmVfYWNjZXNzIiwidW1hX2F1dGhvcml6YXRpb24iXX0sInJlc291cmNlX2FjY2VzcyI6eyJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50Iiwidmlldy1wcm9maWxlIl19fSwic2NvcGUiOiJlbWFpbCBwcm9maWxlIiwiZW1haWxfdmVyaWZpZWQiOmZhbHNlLCJjb21wYW55X2lkIjoxMDAwLCJ1c2VyX2lkIjoxLCJ1c2VyX25hbWUiOiJhZG1pbiIsImNvbXBhbnlfbmFtZSI6Ium7mOiupOWFrOWPuCIsImNvbXBhbnlfY29kZSI6ImRlZmF1bHRfb3JnX2NvbXBhbnkiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJhZG1pbiJ9.oIDRRMgvvDF2yPWOpcmNOdT7XYzu5IyWvbaMS_Zpi_L4-lojZiRd1DkTjP8JEkPQySc8ckHT8_tgoa4UK4E6FSQv3FvzI44PeLcF2PXhs6AUCvEBognZyOXcb9ZLlHohzeZuxX5iapMmYCGMyuXSU76aEM63R0kVUO8jMPleIdlOPbHch1_Vp2lx5OylrQoZgHGmuGGQMF1G9ZzwDiUrrsvMPX5VcdddTr8g6CojRcIUA1nv3ky3jiX0stSr30gpJBkLT17L3FZnrbWKM5PdbNzAUFItx9ShzPCEnG9uF5lwOGV_8Y98VKyUbmW1jzJDLJSZ6yIlpJQWn9e3Y6TY-w".split("\\.");
        byte[] decode = Base64.getDecoder().decode(split[1]);
        JSONObject jsonObject = JSON.parseObject((String)new String(decode));
        System.out.println(jsonObject.toString());
    }
}

