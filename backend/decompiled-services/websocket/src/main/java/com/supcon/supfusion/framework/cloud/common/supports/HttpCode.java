/*
 * Decompiled with CFR 0.152.
 */
package com.supcon.supfusion.framework.cloud.common.supports;

import com.supcon.supfusion.framework.cloud.common.supports.IHttpCode;

public enum HttpCode implements IHttpCode
{
    SUCCESS(200, "SUCCESS"),
    FAILURE(400, "BAD_REQUEST"),
    UN_AUTHORIZED(401, "UN_AUTHORIZED"),
    NOT_FOUND(404, "404 NOT_FOUND"),
    MSG_NOT_READABLE(400, "MSG_NOT_READABLE"),
    METHOD_NOT_SUPPORTED(405, "METHOD_NOT_SUPPORTED"),
    MEDIA_TYPE_NOT_SUPPORTED(415, "MEDIA_TYPE_NOT_SUPPORTED"),
    REQ_REJECT(403, "REQ_REJECT"),
    INTERNAL_SERVER_ERROR(500, "INTERNAL_SERVER_ERROR"),
    PARAM_MISS(400, "PARAM_MISS"),
    PARAM_TYPE_ERROR(400, "PARAM_TYPE_ERROR"),
    PARAM_BIND_ERROR(400, "PARAM_BIND_ERROR"),
    PARAM_VALID_ERROR(400, "PARAM_VALID_ERROR"),
    ORM_SQL_ERROR(100001, "SQL_ERROR"),
    ID_NOT_NULL(100002, "ID_NOT_NULL"),
    ID_MUST_NULL(100003, "ID_MUST_NULL"),
    OBJECT_NULL(100004, "OBJECT_NULL"),
    OBJECT_HAVE_BEAN_DELETED(100005, "OBJECT_HAVE_BEAN_DELETED"),
    CODE_NOT_NULL(100006, "CODE_NOT_NULL"),
    URL_NOT_NULL(100007, "URL_NOT_NULL"),
    URL_FORMAT_ERROR(100008, "URL_FORMAT_ERROR");

    final int httpCode;
    final String message;

    @Override
    public int getHttpCode() {
        return this.httpCode;
    }

    @Override
    public String getMessage() {
        return this.message;
    }

    private HttpCode(int httpCode, String message) {
        this.httpCode = httpCode;
        this.message = message;
    }
}

