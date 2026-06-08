/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.jsonwebtoken.Claims
 *  io.jsonwebtoken.Jwts
 *  io.jsonwebtoken.SignatureAlgorithm
 *  io.jsonwebtoken.impl.TextCodec
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package com.supcon.supfusion.ws.service.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.TextCodec;
import java.io.IOException;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JwtUtil {
    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);
    private static final String secret = "secret";
    private static final String type = "jceks";
    private static final String algorithm = "RS256";
    private static PrivateKey privateKey;
    private static PublicKey publicKey;

    public static Claims parseToken(String token) {
        Claims claims = null;
        try {
            claims = (Claims)Jwts.parserBuilder().setSigningKey((Key)publicKey).build().parseClaimsJws(token).getBody();
        }
        catch (Exception e) {
            log.error("parse token failed: token[{}]", (Object)token);
        }
        return claims;
    }

    public static String genTokenWithPrivateKey(Map<String, Object> claims, String subject) {
        Date date1 = new Date();
        Date date = new Date(date1.getTime() + 6000L);
        return Jwts.builder().setClaims(claims).setSubject(subject).setIssuedAt(date1).setExpiration(date).signWith(SignatureAlgorithm.RS256, (Key)privateKey).compact();
    }

    public static String parsePayload(String jwt) {
        String payload = jwt.split("\\.")[1];
        return TextCodec.BASE64URL.decodeToString(payload);
    }

    static {
        InputStream inputStream = null;
        try {
            inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("keystore.jceks");
            KeyStore keyStore = KeyStore.getInstance(type);
            keyStore.load(inputStream, secret.toCharArray());
            privateKey = (PrivateKey)keyStore.getKey(algorithm, secret.toCharArray());
            publicKey = keyStore.getCertificate(algorithm).getPublicKey();
        }
        catch (Exception e) {
            log.error("init privatekey and publicKey failed");
        }
        finally {
            try {
                if (null != inputStream) {
                    inputStream.close();
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

