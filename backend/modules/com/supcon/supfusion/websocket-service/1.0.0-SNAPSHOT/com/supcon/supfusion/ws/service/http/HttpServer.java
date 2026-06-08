package com.supcon.supfusion.ws.service.http;

import com.supcon.supfusion.ws.service.util.PropertiesManager;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Data
@Slf4j
public class HttpServer {
    private final static int MAX_CONTENT_LENGTH = 10 * 1024 * 1024;
    public void init(final CountDownLatch countDownLatch) {
        int bossTheads = PropertiesManager.getInt("http.bossThreads", 1);
        int workThreads = PropertiesManager.getInt("http.workThreads", 200);
        int size = PropertiesManager.getInt("http.maxHttpSize", MAX_CONTENT_LENGTH);
        int connectTimeOutMills = PropertiesManager.getInt("http.connectTimeOutMills", 1000);
        int port = PropertiesManager.getInt("http.port", 80);
        int idleTimeOutMills = PropertiesManager.getInt("http.idleTimeOutMills", 2000);
        new Thread(() -> {
            EventLoopGroup bossGroup = new NioEventLoopGroup(bossTheads);
            EventLoopGroup workerGroup = new NioEventLoopGroup(workThreads);
            try {
                ServerBootstrap b = new ServerBootstrap();
                b.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .childHandler(new ChannelInitializer<NioSocketChannel>() {
                            protected void initChannel(NioSocketChannel ch) throws Exception {
                                ch.pipeline().addLast(new HttpServerCodec())
                                        .addLast(new HttpObjectAggregator(size))
                                        .addLast(new IdleStateHandler(idleTimeOutMills, idleTimeOutMills, idleTimeOutMills, TimeUnit.MILLISECONDS))
                                        .addLast(new HttpHandler());
                            }
                        })
                        .option(ChannelOption.SO_BACKLOG, 1024)
                        .option(ChannelOption.SO_REUSEADDR, true)
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeOutMills)
                        .childOption(ChannelOption.SO_KEEPALIVE, true)
                        .childOption(ChannelOption.TCP_NODELAY, true);
                ChannelFuture f = b.bind(port).sync();
                log.info("HttpServer start OK!");
                f.channel().closeFuture().sync();
            } catch (Exception e) {
                log.error("HttpServer start is error", e);
            } finally {
                countDownLatch.countDown();
                workerGroup.shutdownGracefully();
                bossGroup.shutdownGracefully();
            }
        }).start();

    }
}
