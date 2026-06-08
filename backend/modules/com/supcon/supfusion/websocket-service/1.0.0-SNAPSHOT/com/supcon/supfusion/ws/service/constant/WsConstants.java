package com.supcon.supfusion.ws.service.constant;

import io.netty.util.AttributeKey;

/**
 * @author lifangyuan
 */
public interface WsConstants {
    AttributeKey<String> TENANTID = AttributeKey.valueOf("X-Tenant-Id");
    AttributeKey<String> USERNAME = AttributeKey.valueOf("userName");
    AttributeKey<String> USERID = AttributeKey.valueOf("userId");
    AttributeKey<String> TOPIC = AttributeKey.valueOf("topic");
    String TENANT_ID = "X-Tenant-Id";
    String JWT_HEADER = "jwt.header";
    String AUTHORIZATION = "Authorization";
    String PATH_VARIABLE_REGEX = "\\{.+\\}";
    String EVERY_CHAR_REGEX = ".*";
    String PREFIX_REGEX = "^";
    String SUFFIX_REGEX = "$";
    String SPACE_STRING = " ";
    String QUESTION_MARK = "?";
    String UPGRADE = "Upgrade";
    String HAND = "hand";
    String ADMIN = "admin";
    String CLAIM_KEY_USER_NAME = "user_name";
    String CLAIM_KEY_USER_ID = "user_id";
}
