package com.supcon.supfusion.ws.service.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.TextCodec;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;
import java.util.Map;


@Slf4j
public class JwtUtil {
    private final static String secret = "secret";
    private final static String type = "jceks";
    private final static String algorithm = "RS256";

    private static PrivateKey privateKey;

    private static PublicKey publicKey;

    static {
        InputStream inputStream = null;
        try {
            // 寻找证书文件
            inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("keystore.jceks");
            // 将证书文件里边的私钥公钥拿出来
            KeyStore keyStore = KeyStore.getInstance(type);
            keyStore.load(inputStream, secret.toCharArray());
            // jwt为命令生成整数文件时的别名
            privateKey = (PrivateKey) keyStore.getKey(algorithm, secret.toCharArray());
            publicKey = keyStore.getCertificate(algorithm).getPublicKey();
        } catch (Exception e) {
            log.error("init privatekey and publicKey failed");
        } finally {
            try {
                if (null != inputStream) {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 通过 公钥解密token
     */
    public static Claims parseToken(String token) {
        Claims claims = null;
        try {
            claims = Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .build()
                    .parseClaimsJws(token).getBody();
        } catch (Exception e) {
            log.error("parse token failed: token[{}]", token);
        }
        return claims;
    }

    /**
     * 使用私钥加密
     *
     * @param claims
     * @param subject
     * @return token
     */
    public static String genTokenWithPrivateKey(Map<String, Object> claims, String subject) {
        Date date1 = new Date();
        Date date = new Date(date1.getTime() + 6000);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(date1)
                .setExpiration(date)
                .signWith(SignatureAlgorithm.RS256, privateKey)
                .compact();
    }

    /**
     * 解析payload部分
     */
    public static String parsePayload(String jwt) {
        String payload = jwt.split("\\.")[1];
        return TextCodec.BASE64URL.decodeToString(payload);
    }
}
