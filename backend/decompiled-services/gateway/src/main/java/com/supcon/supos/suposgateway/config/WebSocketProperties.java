/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.boot.context.properties.ConfigurationProperties
 */
package com.supcon.supos.suposgateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(value="spring.cloud.gateway.httpclient.websocket")
public class WebSocketProperties {
    private int maxFramePayloadLength = 262144;

    public int getMaxFramePayloadLength() {
        return this.maxFramePayloadLength;
    }

    public void setMaxFramePayloadLength(int maxFramePayloadLength) {
        this.maxFramePayloadLength = maxFramePayloadLength;
    }
}

