/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  feign.Client
 *  feign.Contract
 *  feign.Feign
 *  feign.Feign$Builder
 *  feign.codec.Decoder
 *  feign.codec.Encoder
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.cloud.openfeign.FeignClientsConfiguration
 *  org.springframework.cloud.openfeign.ribbon.LoadBalancerFeignClient
 *  org.springframework.context.annotation.Import
 *  org.springframework.stereotype.Service
 */
package com.supcon.orchid.entityconf.service.imps;

import com.supcon.orchid.entityconf.services.FeignService;
import feign.Client;
import feign.Contract;
import feign.Feign;
import feign.codec.Decoder;
import feign.codec.Encoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.FeignClientsConfiguration;
import org.springframework.cloud.openfeign.ribbon.LoadBalancerFeignClient;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;

@Service
@Import(value={FeignClientsConfiguration.class})
public class FeignServiceImpl
implements FeignService {
    private final Feign.Builder urlBuilder;
    private final Feign.Builder nameBuilder;

    @Autowired
    public FeignServiceImpl(Decoder decoder, Encoder encoder, Client client, Contract contract) {
        this.nameBuilder = Feign.builder().client(client).encoder(encoder).decoder(decoder).contract(contract);
        if (client instanceof LoadBalancerFeignClient) {
            client = ((LoadBalancerFeignClient)client).getDelegate();
        }
        this.urlBuilder = Feign.builder().client(client).encoder(encoder).decoder(decoder).contract(contract);
    }

    @Override
    public <T> T newInstanceByUrl(Class<T> apiType, String url) {
        return (T)this.urlBuilder.target(apiType, url);
    }

    @Override
    public <T> T newInstanceByName(Class<T> apiType, String name) {
        return (T)this.nameBuilder.target(apiType, name);
    }
}

