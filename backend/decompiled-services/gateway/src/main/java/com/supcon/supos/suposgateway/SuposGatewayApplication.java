/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.bootstrap.ServerBootstrap
 *  io.netty.channel.EventLoopGroup
 *  io.netty.channel.nio.NioEventLoopGroup
 *  io.netty.channel.socket.nio.NioServerSocketChannel
 *  org.springframework.boot.SpringApplication
 *  org.springframework.boot.autoconfigure.SpringBootApplication
 *  org.springframework.boot.context.properties.EnableConfigurationProperties
 *  org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory
 *  org.springframework.boot.web.embedded.netty.NettyServerCustomizer
 *  org.springframework.cloud.client.discovery.EnableDiscoveryClient
 *  org.springframework.cloud.openfeign.EnableFeignClients
 *  org.springframework.context.annotation.Bean
 *  reactor.netty.http.server.HttpServer
 */
package com.supcon.supos.suposgateway;

import com.supcon.supos.suposgateway.filter.support.PathMatcher;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import java.util.Collections;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.embedded.netty.NettyServerCustomizer;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import reactor.netty.http.server.HttpServer;

@SpringBootApplication(scanBasePackages={"com.supcon.supos.suposgateway"})
@EnableDiscoveryClient
@EnableConfigurationProperties(value={PathMatcher.class})
@EnableFeignClients(basePackages={"com.supcon.supos.suposgateway.feign.client"})
public class SuposGatewayApplication {
    public static void main(String[] args) {
        try {
            SpringApplication.run(SuposGatewayApplication.class, (String[])args);
        }
        catch (Exception e) {
            System.exit(1);
        }
    }

    @Bean
    public NettyReactiveWebServerFactory factory() {
        int processors = Runtime.getRuntime().availableProcessors();
        final NioEventLoopGroup bossGroup = new NioEventLoopGroup(processors);
        final NioEventLoopGroup workerGroup = new NioEventLoopGroup(2 * processors);
        NettyReactiveWebServerFactory factory = new NettyReactiveWebServerFactory();
        factory.setServerCustomizers(Collections.singletonList(new NettyServerCustomizer(){

            public HttpServer apply(HttpServer httpServer) {
                return httpServer.tcpConfiguration(tcpServer -> tcpServer.bootstrap(serverBootstrap -> (ServerBootstrap)serverBootstrap.group((EventLoopGroup)bossGroup, (EventLoopGroup)workerGroup).channel(NioServerSocketChannel.class)));
            }
        }));
        return factory;
    }
}

