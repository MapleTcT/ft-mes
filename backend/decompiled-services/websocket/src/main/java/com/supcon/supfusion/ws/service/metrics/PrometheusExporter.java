/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.prometheus.client.Collector$MetricFamilySamples
 *  io.prometheus.client.CollectorRegistry
 *  io.prometheus.client.hotspot.DefaultExports
 */
package com.supcon.supfusion.ws.service.metrics;

import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.hotspot.DefaultExports;
import java.io.IOException;
import java.util.Enumeration;

public class PrometheusExporter {
    private static PrometheusExporter INSTANCE;

    private PrometheusExporter() {
        DefaultExports.initialize();
    }

    public static synchronized PrometheusExporter instance() {
        if (INSTANCE == null) {
            INSTANCE = new PrometheusExporter();
        }
        return INSTANCE;
    }

    public Enumeration<Collector.MetricFamilySamples> metricFamilySamples() throws IOException {
        return CollectorRegistry.defaultRegistry.metricFamilySamples();
    }
}

