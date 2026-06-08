/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.http.HttpStatus
 *  org.springframework.web.server.ResponseStatusException
 */
package com.supcon.supos.suposgateway.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class GatewayResponseStatusException
extends ResponseStatusException {
    public GatewayResponseStatusException(HttpStatus status) {
        super(status);
    }

    public GatewayResponseStatusException(HttpStatus status, String reason) {
        super(status, reason);
    }

    public GatewayResponseStatusException(HttpStatus status, String reason, Throwable cause) {
        super(status, reason, cause);
    }
}

