/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.util.AttributeKey
 */
package com.supcon.supfusion.ws.service.constant;

import io.netty.util.AttributeKey;

public interface WsConstants {
    public static final AttributeKey<String> TENANTID = AttributeKey.valueOf((String)"X-Tenant-Id");
    public static final AttributeKey<String> USERNAME = AttributeKey.valueOf((String)"userName");
    public static final AttributeKey<String> USERID = AttributeKey.valueOf((String)"userId");
    public static final AttributeKey<String> TOPIC = AttributeKey.valueOf((String)"topic");
    public static final String TENANT_ID = "X-Tenant-Id";
    public static final String JWT_HEADER = "jwt.header";
    public static final String AUTHORIZATION = "Authorization";
    public static final String PATH_VARIABLE_REGEX = "\\{.+\\}";
    public static final String EVERY_CHAR_REGEX = ".*";
    public static final String PREFIX_REGEX = "^";
    public static final String SUFFIX_REGEX = "$";
    public static final String SPACE_STRING = " ";
    public static final String QUESTION_MARK = "?";
    public static final String UPGRADE = "Upgrade";
    public static final String HAND = "hand";
    public static final String ADMIN = "admin";
    public static final String CLAIM_KEY_USER_NAME = "user_name";
    public static final String CLAIM_KEY_USER_ID = "user_id";
}

