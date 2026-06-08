/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.cloud.gateway.filter.GatewayFilter
 *  org.springframework.core.Ordered
 */
package com.supcon.supos.suposgateway.filter;

import com.supcon.supos.suposgateway.utils.ILogger;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.core.Ordered;

public interface OrderedGatewayFilter
extends GatewayFilter,
Ordered,
ILogger {
}

