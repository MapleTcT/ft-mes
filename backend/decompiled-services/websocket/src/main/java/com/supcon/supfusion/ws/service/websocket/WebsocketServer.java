/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.bootstrap.ServerBootstrap
 *  io.netty.channel.ChannelFuture
 *  io.netty.channel.ChannelHandler
 *  io.netty.channel.ChannelInitializer
 *  io.netty.channel.ChannelOption
 *  io.netty.channel.ChannelPipeline
 *  io.netty.channel.EventLoopGroup
 *  io.netty.channel.WriteBufferWaterMark
 *  io.netty.channel.nio.NioEventLoopGroup
 *  io.netty.channel.socket.nio.NioServerSocketChannel
 *  io.netty.channel.socket.nio.NioSocketChannel
 *  io.netty.handler.codec.http.HttpObjectAggregator
 *  io.netty.handler.codec.http.HttpServerCodec
 *  io.netty.handler.timeout.IdleStateHandler
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package com.supcon.supfusion.ws.service.websocket;

import com.supcon.supfusion.ws.service.util.PropertiesManager;
import com.supcon.supfusion.ws.service.websocket.NioWebSocketHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.timeout.IdleStateHandler;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebsocketServer {
    private static final Logger log = LoggerFactory.getLogger(WebsocketServer.class);
    private static final int MAX_CONTENT_LENGTH = 0xA00000;

    public void init(CountDownLatch countDownLatch) {
        int bossTheads = PropertiesManager.getInt("ws.boosThreads", 1);
        int workThreads = PropertiesManager.getInt("ws.workThreads", 200);
        final int size = PropertiesManager.getInt("ws.maxHttpSize", 0xA00000);
        int connectTimeOutMills = PropertiesManager.getInt("ws.connectTimeOutMills", 3000);
        int port = PropertiesManager.getInt("ws.port", 30135);
        final int idleTimeOutSeconds = PropertiesManager.getInt("ws.idleTimeOutSeconds", 60);
        int bufferLowWaterMark = PropertiesManager.getInt("ws.bufferLowWaterMark", 32768);
        int bufferHighWaterMark = PropertiesManager.getInt("ws.bufferHighWaterMark", 131072);
        new Thread(() -> {
            NioEventLoopGroup boss = new NioEventLoopGroup(bossTheads);
            NioEventLoopGroup worker = new NioEventLoopGroup(workThreads);
            ServerBootstrap bootstrap = new ServerBootstrap();
            try {
                ((ServerBootstrap)((ServerBootstrap)((ServerBootstrap)bootstrap.group((EventLoopGroup)boss, (EventLoopGroup)worker).channel(NioServerSocketChannel.class)).option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (Object)connectTimeOutMills)).option(ChannelOption.SO_BACKLOG, (Object)1024)).childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, (Object)new WriteBufferWaterMark(bufferLowWaterMark, bufferHighWaterMark)).childOption(ChannelOption.TCP_NODELAY, (Object)true).childOption(ChannelOption.SO_KEEPALIVE, (Object)true).childOption(ChannelOption.SO_LINGER, (Object)1).childOption(ChannelOption.ALLOW_HALF_CLOSURE, (Object)true).childHandler((ChannelHandler)new ChannelInitializer<NioSocketChannel>(){

                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new ChannelHandler[]{new HttpServerCodec()});
                        pipeline.addLast(new ChannelHandler[]{new HttpObjectAggregator(size)});
                        pipeline.addLast(new ChannelHandler[]{new IdleStateHandler((long)idleTimeOutSeconds, (long)idleTimeOutSeconds, (long)idleTimeOutSeconds, TimeUnit.SECONDS)});
                        pipeline.addLast("ws", (ChannelHandler)new NioWebSocketHandler());
                    }
                });
                ChannelFuture f = bootstrap.bind(port).sync();
                log.info("webSocketServer start OK!");
                f.channel().closeFuture().sync();
            }
            catch (Exception e) {
                log.error("webSocketServer start is", (Throwable)e);
            }
            finally {
                countDownLatch.countDown();
                boss.shutdownGracefully();
                worker.shutdownGracefully();
            }
        }).start();
    }
}

