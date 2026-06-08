package com.supcon.supfusion.ws.service.http.controller;

import com.supcon.supfusion.ws.service.annotation.Controller;
import com.supcon.supfusion.ws.service.constant.WsConstants;
import com.supcon.supfusion.ws.service.manage.ChannelGroupManager;
import com.supcon.supfusion.ws.service.manage.TenantGroup;
import com.supcon.supfusion.ws.service.metrics.PrometheusExporter;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import lombok.extern.slf4j.Slf4j;

import java.io.OutputStreamWriter;
import java.util.Optional;

@Slf4j
@Controller(method = "GET", uriPattern = "/actuator/prometheus")
public class MetricsContriller {
    public void hand(ChannelHandlerContext ctx, FullHttpRequest req, String topic) {
        try {
            ByteBuf buf = Unpooled.buffer();
            ByteBufOutputStream os = new ByteBufOutputStream(buf);
            OutputStreamWriter writer = new OutputStreamWriter(os);
            TextFormat.write004(writer, PrometheusExporter.instance().metricFamilySamples());
            writer.close();
            os.close();
            FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buf);
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, TextFormat.CONTENT_TYPE_004);
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }catch (Exception e){
            FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, TextFormat.CONTENT_TYPE_004);
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }
    }
}
