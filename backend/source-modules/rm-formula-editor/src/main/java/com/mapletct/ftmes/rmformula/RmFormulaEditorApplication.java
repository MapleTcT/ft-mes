package com.mapletct.ftmes.rmformula;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@EnableDiscoveryClient
@SpringBootApplication
public class RmFormulaEditorApplication {
    public static void main(String[] args) {
        SpringApplication.run(RmFormulaEditorApplication.class, args);
    }

    @Bean
    public RestTemplate rmFormulaRestTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(15000);
        return new RestTemplate(requestFactory);
    }
}
