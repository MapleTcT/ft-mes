/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.bootstrap.ServerBootstrap
 *  io.netty.channel.ChannelFuture
 *  io.netty.channel.ChannelHandler
 *  io.netty.channel.ChannelInitializer
 *  io.netty.channel.ChannelOption
 *  io.netty.channel.EventLoopGroup
 *  io.netty.channel.nio.NioEventLoopGroup
 *  io.netty.channel.socket.nio.NioServerSocketChannel
 *  io.netty.channel.socket.nio.NioSocketChannel
 *  io.netty.handler.codec.http.HttpObjectAggregator
 *  io.netty.handler.codec.http.HttpServerCodec
 *  io.netty.handler.timeout.IdleStateHandler
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package com.supcon.supfusion.ws.service.http;

import com.supcon.supfusion.ws.service.http.HttpHandler;
import com.supcon.supfusion.ws.service.util.PropertiesManager;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
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

public class HttpServer {
    private static final Logger log = LoggerFactory.getLogger(HttpServer.class);
    private static final int MAX_CONTENT_LENGTH = 0xA00000;

    public void init(CountDownLatch countDownLatch) {
        int bossTheads = PropertiesManager.getInt("http.bossThreads", 1);
        int workThreads = PropertiesManager.getInt("http.workThreads", 200);
        final int size = PropertiesManager.getInt("http.maxHttpSize", 0xA00000);
        int connectTimeOutMills = PropertiesManager.getInt("http.connectTimeOutMills", 1000);
        int port = PropertiesManager.getInt("http.port", 80);
        final int idleTimeOutMills = PropertiesManager.getInt("http.idleTimeOutMills", 2000);
        new Thread(() -> {
            NioEventLoopGroup bossGroup = new NioEventLoopGroup(bossTheads);
            NioEventLoopGroup workerGroup = new NioEventLoopGroup(workThreads);
            try {
                ServerBootstrap b = new ServerBootstrap();
                ((ServerBootstrap)((ServerBootstrap)((ServerBootstrap)((ServerBootstrap)b.group((EventLoopGroup)bossGroup, (EventLoopGroup)workerGroup).channel(NioServerSocketChannel.class)).childHandler((ChannelHandler)new ChannelInitializer<NioSocketChannel>(){

                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new ChannelHandler[]{new HttpServerCodec()}).addLast(new ChannelHandler[]{new HttpObjectAggregator(size)}).addLast(new ChannelHandler[]{new IdleStateHandler((long)idleTimeOutMills, (long)idleTimeOutMills, (long)idleTimeOutMills, TimeUnit.MILLISECONDS)}).addLast(new ChannelHandler[]{new HttpHandler()});
                    }
                }).option(ChannelOption.SO_BACKLOG, (Object)1024)).option(ChannelOption.SO_REUSEADDR, (Object)true)).option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (Object)connectTimeOutMills)).childOption(ChannelOption.SO_KEEPALIVE, (Object)true).childOption(ChannelOption.TCP_NODELAY, (Object)true);
                ChannelFuture f = b.bind(port).sync();
                log.info("HttpServer start OK!");
                f.channel().closeFuture().sync();
            }
            catch (Exception e) {
                log.error("HttpServer start is error", (Throwable)e);
            }
            finally {
                countDownLatch.countDown();
                workerGroup.shutdownGracefully();
                bossGroup.shutdownGracefully();
            }
        }).start();
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof HttpServer)) {
            return false;
        }
        HttpServer other = (HttpServer)o;
        return other.canEqual(this);
    }

    protected boolean canEqual(Object other) {
        return other instanceof HttpServer;
    }

    public int hashCode() {
        boolean result = true;
        return 1;
    }

    public String toString() {
        return "HttpServer()";
    }
}

