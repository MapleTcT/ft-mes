package com.supcon.supfusion.ws.service.websocket;

import com.supcon.supfusion.ws.service.util.PropertiesManager;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
public class WebsocketServer {
    private final static int MAX_CONTENT_LENGTH = 10 * 1024 * 1024;

    public void init(final CountDownLatch countDownLatch) {

        int bossTheads = PropertiesManager.getInt("ws.boosThreads", 1);
        int workThreads = PropertiesManager.getInt("ws.workThreads", 200);
        int size = PropertiesManager.getInt("ws.maxHttpSize", MAX_CONTENT_LENGTH);
        int connectTimeOutMills = PropertiesManager.getInt("ws.connectTimeOutMills", 3000);
        int port = PropertiesManager.getInt("ws.port", 30135);
        int idleTimeOutSeconds = PropertiesManager.getInt("ws.idleTimeOutSeconds", 60);
        int bufferLowWaterMark = PropertiesManager.getInt("ws.bufferLowWaterMark", 32 * 1024);
        int bufferHighWaterMark = PropertiesManager.getInt("ws.bufferHighWaterMark", 128 * 1024);
        new Thread(() -> {
            EventLoopGroup boss = new NioEventLoopGroup(bossTheads);
            EventLoopGroup worker = new NioEventLoopGroup(workThreads);
            ServerBootstrap bootstrap = new ServerBootstrap();
            try {
                bootstrap.group(boss, worker)
                        .channel(NioServerSocketChannel.class)
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeOutMills)
                        .option(ChannelOption.SO_BACKLOG, 1024)
                        .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(bufferLowWaterMark, bufferHighWaterMark))
                        .childOption(ChannelOption.TCP_NODELAY, true)
                        .childOption(ChannelOption.SO_KEEPALIVE, true)
                        .childOption(ChannelOption.SO_LINGER, 1)
                        .childOption(ChannelOption.ALLOW_HALF_CLOSURE, true)
                        .childHandler(new ChannelInitializer<NioSocketChannel>() {
                            @Override
                            protected void initChannel(NioSocketChannel ch) throws Exception {
                                ChannelPipeline pipeline = ch.pipeline();
                                pipeline.addLast(new HttpServerCodec());
                                pipeline.addLast(new HttpObjectAggregator(size));
                                pipeline.addLast(new IdleStateHandler(idleTimeOutSeconds, idleTimeOutSeconds, idleTimeOutSeconds, TimeUnit.SECONDS));
                                pipeline.addLast("ws", new NioWebSocketHandler());
                            }
                        });
                ChannelFuture f = bootstrap.bind(port).sync();
                log.info("webSocketServer start OK!");
                f.channel().closeFuture().sync();
            } catch (Exception e) {
                log.error("webSocketServer start is", e);
            } finally {
                countDownLatch.countDown();
                boss.shutdownGracefully();
                worker.shutdownGracefully();
            }
        }).start();
    }
}
