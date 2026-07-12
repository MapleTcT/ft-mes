package com.mapletct.ftmes.bpiadapter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AdapterConfiguration {

    @Bean
    public RestTemplate bpiRestTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5_000);
        requestFactory.setReadTimeout(15_000);
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            protected boolean hasError(org.springframework.http.HttpStatus statusCode) {
                return false;
            }
        });
        return restTemplate;
    }
}
