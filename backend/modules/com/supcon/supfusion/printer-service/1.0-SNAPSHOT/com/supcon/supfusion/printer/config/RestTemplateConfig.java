package com.supcon.supfusion.printer.config;

import java.io.IOException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * @author liyiming
 * @date 2020/11/30 10:25 上午
 */
@Configuration
public class RestTemplateConfig {
    // 设置为静态，保证线程共享，连接池可以复用
    protected static CloseableHttpClient httpClient = null;
    static {
        httpClient = HttpClientFactory.createHttpClient(300, 20, 30000, 0);
        //当然，也可以把CloseableHttpClient定义为Bean，然后在@PreDestroy标记的方法内close这个HttpClient
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                httpClient.close();
            } catch (IOException e) {
            }
        }));
    }

    /**
     * TODO 如果需要租户信息，需要将租户信息设置到请求头
     * @return
     */
    @Bean({"restTemplateClient"})
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate(httpRequestFactory());
    }

    @Bean
    public ClientHttpRequestFactory httpRequestFactory() {
        return new HttpComponentsClientHttpRequestFactory(httpClient);
    }
}
