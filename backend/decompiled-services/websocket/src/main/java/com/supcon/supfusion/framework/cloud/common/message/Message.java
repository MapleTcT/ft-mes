/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.annotation.JsonPropertyOrder
 */
package com.supcon.supfusion.framework.cloud.common.message;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@JsonPropertyOrder(value={"msgId", "header", "body"})
public class Message<T>
implements Serializable {
    private static final long serialVersionUID = -760428162075351161L;
    private Long msgId = IDGenerator.newInstance().generate().longValue();
    private Header header = new Header();
    private T body;

    public Message(T body) {
        this.body = body;
    }

    public Message addHeader(String key, Object value) {
        this.header.getParams().put(key, value);
        return this;
    }

    public Message removeHeader(String key) {
        this.header.getParams().remove(key);
        return this;
    }

    public void setMsgId(Long msgId) {
        this.msgId = msgId;
    }

    public void setHeader(Header header) {
        this.header = header;
    }

    public void setBody(T body) {
        this.body = body;
    }

    public Long getMsgId() {
        return this.msgId;
    }

    public Header getHeader() {
        return this.header;
    }

    public T getBody() {
        return this.body;
    }

    public String toString() {
        return "Message(msgId=" + this.getMsgId() + ", header=" + this.getHeader() + ", body=" + this.getBody() + ")";
    }

    public Message() {
    }

    public static final class Header {
        private Map<String, Object> params = new HashMap<String, Object>();

        public void setParams(Map<String, Object> params) {
            this.params = params;
        }

        public Map<String, Object> getParams() {
            return this.params;
        }

        public String toString() {
            return "Message.Header(params=" + this.getParams() + ")";
        }
    }
}

