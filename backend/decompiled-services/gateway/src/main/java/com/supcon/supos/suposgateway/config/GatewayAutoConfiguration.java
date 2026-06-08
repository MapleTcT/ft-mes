/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.boot.autoconfigure.AutoConfigureAfter
 *  org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
 *  org.springframework.boot.context.properties.EnableConfigurationProperties
 *  org.springframework.cloud.gateway.config.GatewayAutoConfiguration
 *  org.springframework.context.annotation.Bean
 *  org.springframework.context.annotation.Configuration
 *  org.springframework.context.annotation.Primary
 *  org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
 *  reactor.netty.http.client.HttpClient
 */
package com.supcon.supos.suposgateway.config;

import com.supcon.supos.suposgateway.config.WebSocketProperties;
import com.supcon.supos.suposgateway.filter.CacheRequestBodyGlobalFilter;
import com.supcon.supos.suposgateway.filter.CacheResponseBodyGlobalFilter;
import com.supcon.supos.suposgateway.filter.CookieGlobalFilter;
import com.supcon.supos.suposgateway.filter.InitGlobalFilter;
import com.supcon.supos.suposgateway.filter.IpStoreGlobalFilter;
import com.supcon.supos.suposgateway.filter.IpVerifyGlobalFilter;
import com.supcon.supos.suposgateway.filter.JwtAuthenticationTokenFilter;
import com.supcon.supos.suposgateway.filter.LicenseFilter;
import com.supcon.supos.suposgateway.filter.LogGlobalFilter;
import com.supcon.supos.suposgateway.filter.LoginNumFilter;
import com.supcon.supos.suposgateway.filter.RabcFilter;
import com.supcon.supos.suposgateway.filter.WSResolverGlobalFilter;
import com.supcon.supos.suposgateway.filter.factory.AddTenantAppUriHeaderGatewayFilterFactory;
import com.supcon.supos.suposgateway.filter.factory.DynamicRedirectGatewayFilterFactory;
import com.supcon.supos.suposgateway.filter.factory.ForwardAppUrlGatewayFilterFactory;
import com.supcon.supos.suposgateway.filter.factory.SetPortAfterLoadBalancerGatewayFilterFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.netty.http.client.HttpClient;

@Configuration
@EnableConfigurationProperties(value={WebSocketProperties.class})
@AutoConfigureAfter(value={org.springframework.cloud.gateway.config.GatewayAutoConfiguration.class})
public class GatewayAutoConfiguration {
    @Autowired
    private WebSocketProperties webSocketProperties;

    @Primary
    @Bean
    public ReactorNettyWebSocketClient reactNettyWebSocketClient(HttpClient httpClient) {
        return new com.supcon.supos.suposgateway.websocket.ReactorNettyWebSocketClient(httpClient, this.webSocketProperties.getMaxFramePayloadLength());
    }

    @Bean
    @ConditionalOnProperty(name={"filter.cacheRequestBodyGlobalFilter.enabled"}, havingValue="true")
    public CacheRequestBodyGlobalFilter cacheRequestBodyGlobalFilter() {
        return new CacheRequestBodyGlobalFilter();
    }

    @Bean
    @ConditionalOnProperty(name={"filter.cacheResponseBodyGlobalFilter.enabled"}, havingValue="true")
    public CacheResponseBodyGlobalFilter cacheResponseBodyGlobalFilter() {
        return new CacheResponseBodyGlobalFilter();
    }

    @Bean
    public InitGlobalFilter initGlobalFilter() {
        return new InitGlobalFilter();
    }

    @Bean
    @ConditionalOnProperty(name={"license.enabled"}, havingValue="false")
    public LicenseFilter licenseFilter() {
        return new LicenseFilter();
    }

    @Bean
    @ConditionalOnProperty(name={"filter.logGlobalFilter.enabled"}, havingValue="true", matchIfMissing=true)
    public LogGlobalFilter logGlobalFilter() {
        return new LogGlobalFilter();
    }

    @Bean
    @ConditionalOnProperty(name={"integration.supos.enabled"}, havingValue="false")
    public LoginNumFilter loginNumFilter() {
        return new LoginNumFilter();
    }

    @Bean
    public WSResolverGlobalFilter wsResolverGlobalFilter() {
        return new WSResolverGlobalFilter();
    }

    @Bean
    public RabcFilter rabcFilter() {
        return new RabcFilter();
    }

    @Bean
    public AddTenantAppUriHeaderGatewayFilterFactory addTenantAppUriHeaderGatewayFilterFactory() {
        return new AddTenantAppUriHeaderGatewayFilterFactory();
    }

    @Bean
    public DynamicRedirectGatewayFilterFactory dynamicRedirectGatewayFilterFactory() {
        return new DynamicRedirectGatewayFilterFactory();
    }

    @Bean
    public SetPortAfterLoadBalancerGatewayFilterFactory setPortAfterLoadBalancerGatewayFilterFactory() {
        return new SetPortAfterLoadBalancerGatewayFilterFactory();
    }

    @Bean
    public JwtAuthenticationTokenFilter jwtAuthenticationTokenFilter() {
        return new JwtAuthenticationTokenFilter();
    }

    @Bean
    public IpVerifyGlobalFilter ipVerifyGlobalFilter() {
        return new IpVerifyGlobalFilter();
    }

    @Bean
    @ConditionalOnProperty(value={"custom.dev.enable"})
    public IpStoreGlobalFilter customCacheIpGlobalFilter() {
        return new IpStoreGlobalFilter();
    }

    @Bean
    public CookieGlobalFilter cookieGlobalFilter() {
        return new CookieGlobalFilter();
    }

    @Bean
    public ForwardAppUrlGatewayFilterFactory forwardAppUrlGatewayFilterFactory() {
        return new ForwardAppUrlGatewayFilterFactory();
    }
}

