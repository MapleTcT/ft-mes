package com.supcon.supfusion.ws.service.metrics;

import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import io.prometheus.client.hotspot.DefaultExports;

import java.io.*;
import java.util.Enumeration;

public class PrometheusExporter {

    private static PrometheusExporter INSTANCE;

    private PrometheusExporter(){
        DefaultExports.initialize();
    }

    public static synchronized PrometheusExporter instance() {
        if (INSTANCE == null) {
            INSTANCE = new PrometheusExporter();
        }
        return INSTANCE;
    }
    /**
     * Write the Prometheus formatted values of all counters and
     * gauges to the stream
     *
     * @throws IOException
     */
    public Enumeration<Collector.MetricFamilySamples> metricFamilySamples() throws IOException {
      return CollectorRegistry.defaultRegistry.metricFamilySamples();
    }
}
