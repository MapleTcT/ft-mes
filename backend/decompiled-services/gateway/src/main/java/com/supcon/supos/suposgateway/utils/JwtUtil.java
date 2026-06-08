/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.jsonwebtoken.Claims
 *  io.jsonwebtoken.Jwts
 *  io.jsonwebtoken.MalformedJwtException
 *  io.jsonwebtoken.impl.TextCodec
 *  org.apache.commons.lang3.StringUtils
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.http.HttpCookie
 *  org.springframework.http.server.reactive.ServerHttpRequest
 */
package com.supcon.supos.suposgateway.utils;

import com.supcon.supos.suposgateway.utils.HttpUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.impl.TextCodec;
import java.security.Key;
import java.security.PublicKey;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;

public class JwtUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(JwtUtil.class);

    public static String parsePayload(String jwt) {
        String payload = jwt.split("\\.")[1];
        return TextCodec.BASE64URL.decodeToString(payload);
    }

    public static String parseTokenTicket(ServerHttpRequest request) {
        HttpCookie cookie;
        String token = request.getHeaders().getFirst("Authorization");
        if (StringUtils.isBlank((CharSequence)token) && (request.getURI().getRawPath().startsWith("/msService") || request.getURI().getRawPath().startsWith("/inter-api/file-server/web/") || request.getURI().getRawPath().startsWith("/inter-api/file-server/build/") || request.getURI().getRawPath().startsWith("/inter-api/file-server/v1/file/pdfStreamHandeler") || request.getURI().getRawPath().startsWith("/inter-api/file-server/v1/file/auth/overview/image")) && (cookie = (HttpCookie)request.getCookies().getFirst((Object)"suposTicket")) != null) {
            token = cookie.getValue();
        }
        if (StringUtils.isBlank((CharSequence)token) && HttpUtils.isWS(request)) {
            token = (String)request.getQueryParams().getFirst((Object)"token");
        }
        if (StringUtils.isBlank((CharSequence)token)) {
            return null;
        }
        if (token.startsWith("Bearer")) {
            String[] arr = token.split(" ");
            if (arr.length != 2) {
                return null;
            }
            return arr[1];
        }
        return token;
    }

    public static void main(String[] args) {
        String jwt = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJwelhpeXRYVHZVVmx6YTloUC1tZTdEUXNLbXlvRkxLSmFHeE5HMEQzdUpNIn0.eyJleHAiOjE2MDQ2NzMwOTQsImlhdCI6MTYwNDYyOTg5NCwianRpIjoiOTJhNmNlNzgtOTNjNS00ZjZjLTg5YWEtZWRjMGYzZGQ1NzAxIiwiaXNzIjoiaHR0cDovLzEwLjQyLjAuMTg6ODA4MC9hdXRoL3JlYWxtcy9zdXBvcyIsImF1ZCI6ImFjY291bnQiLCJzdWIiOiJhNzYxYTQxMy1hZDQ1LTQ3YzAtOWFjNS0wNDAxNTY5ODM0MWEiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJzZGZkc2YiLCJzZXNzaW9uX3N0YXRlIjoiMDBjNTc3ZTMtNmNmNS00Y2Y3LWE4NjgtMmM3YzAzODg5ZTQ4IiwiYWNyIjoiMSIsInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJvZmZsaW5lX2FjY2VzcyIsInVtYV9hdXRob3JpemF0aW9uIl19LCJyZXNvdXJjZV9hY2Nlc3MiOnsiYWNjb3VudCI6eyJyb2xlcyI6WyJtYW5hZ2UtYWNjb3VudCIsInZpZXctcHJvZmlsZSJdfX0sInNjb3BlIjoiZW1haWwgcHJvZmlsZSIsImNsaWVudEhvc3QiOiIxMC40Mi4wLjc2IiwiZW1haWxfdmVyaWZpZWQiOmZhbHNlLCJjbGllbnRJZCI6InNkZmRzZiIsInByZWZlcnJlZF91c2VybmFtZSI6InNlcnZpY2UtYWNjb3VudC1zZGZkc2YiLCJjbGllbnRBZGRyZXNzIjoiMTAuNDIuMC43NiJ9.EGn1KiLiBRvlWIGOfCy2L5G-aACKqf80WRVtXDlIZigvGqThaVgHROKUuLTMT7200okjTDsAz8he8GC4Ky4nyc_E6-gEzMa2KEjvmh-eVYAK8WRCQiGBqU7rVcSGG4MdVFpZZCjA7WE6Y5nbRoVtEnhWKR-XkuXlKN3dbLMAEVPEXLEvTGwrSiMmnMtEZhuTsifWs4KUqZRVHlnmbYtSkm-rzBoym4cJY0Tp_-zfMupHR33hEFIX7mKl-VjCLrMdP8HR08L-B3_0pNfjPWC7_KmWADXoxejvs5yAKI_8lDY503w7_WytVJrBiEEKOI3_oUQ0KEKG0zcOpnw86FHbsw";
        String payload = JwtUtil.parsePayload(jwt);
        System.out.println(payload);
    }

    public static Claims validateJwt(String jwt, PublicKey publicKey) {
        return (Claims)Jwts.parser().setSigningKey((Key)publicKey).parseClaimsJws(jwt).getBody();
    }

    public static boolean isJwtFormat(String str, PublicKey publicKey) {
        boolean result = true;
        try {
            JwtUtil.validateJwt(str, publicKey);
        }
        catch (MalformedJwtException e) {
            result = false;
        }
        catch (Exception e) {
            LOGGER.error("isJwtFormat method error", (Throwable)e);
        }
        return result;
    }
}

