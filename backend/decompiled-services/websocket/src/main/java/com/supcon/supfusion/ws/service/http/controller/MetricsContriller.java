/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 *  io.netty.buffer.ByteBufOutputStream
 *  io.netty.buffer.Unpooled
 *  io.netty.channel.ChannelFutureListener
 *  io.netty.channel.ChannelHandlerContext
 *  io.netty.handler.codec.http.DefaultFullHttpResponse
 *  io.netty.handler.codec.http.FullHttpRequest
 *  io.netty.handler.codec.http.HttpHeaderNames
 *  io.netty.handler.codec.http.HttpResponseStatus
 *  io.netty.handler.codec.http.HttpVersion
 *  io.netty.util.concurrent.GenericFutureListener
 *  io.prometheus.client.exporter.common.TextFormat
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package com.supcon.supfusion.ws.service.http.controller;

import com.supcon.supfusion.ws.service.annotation.Controller;
import com.supcon.supfusion.ws.service.metrics.PrometheusExporter;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.concurrent.GenericFutureListener;
import io.prometheus.client.exporter.common.TextFormat;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller(method="GET", uriPattern="/actuator/prometheus")
public class MetricsContriller {
    private static final Logger log = LoggerFactory.getLogger(MetricsContriller.class);

    public void hand(ChannelHandlerContext ctx, FullHttpRequest req, String topic) {
        try {
            ByteBuf buf = Unpooled.buffer();
            ByteBufOutputStream os = new ByteBufOutputStream(buf);
            OutputStreamWriter writer = new OutputStreamWriter((OutputStream)os);
            TextFormat.write004((Writer)writer, PrometheusExporter.instance().metricFamilySamples());
            writer.close();
            os.close();
            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buf);
            response.headers().set((CharSequence)HttpHeaderNames.CONTENT_TYPE, (Object)"text/plain; version=0.0.4; charset=utf-8");
            ctx.writeAndFlush((Object)response).addListener((GenericFutureListener)ChannelFutureListener.CLOSE);
        }
        catch (Exception e) {
            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            response.headers().set((CharSequence)HttpHeaderNames.CONTENT_TYPE, (Object)"text/plain; version=0.0.4; charset=utf-8");
            ctx.writeAndFlush((Object)response).addListener((GenericFutureListener)ChannelFutureListener.CLOSE);
        }
    }
}

