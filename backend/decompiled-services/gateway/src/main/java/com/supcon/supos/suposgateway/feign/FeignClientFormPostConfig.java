/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  feign.codec.Encoder
 *  feign.form.FormEncoder
 *  org.springframework.beans.factory.ObjectFactory
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.boot.autoconfigure.http.HttpMessageConverters
 *  org.springframework.cloud.openfeign.support.SpringEncoder
 *  org.springframework.context.annotation.Bean
 *  org.springframework.context.annotation.Configuration
 */
package com.supcon.supos.suposgateway.feign;

import feign.codec.Encoder;
import feign.form.FormEncoder;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignClientFormPostConfig {
    @Autowired
    private ObjectFactory<HttpMessageConverters> messageConverters;

    @Bean
    public Encoder feignFormEncoder() {
        return new FormEncoder((Encoder)new SpringEncoder(this.messageConverters));
    }
}

