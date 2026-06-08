package com.supcon.supfusion.auth.keycloak.client.api;

import com.supcon.supfusion.auth.keycloak.client.api.dto.ClientDTO;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "auth", contextId = "keycloak")
public interface ClientRegistrationApiService {

    String API_PREFIX = "/service-api/auth/v2";

    @PostMapping(API_PREFIX + "/clients-registrations/default")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    Result<Boolean> create(@Validated @RequestBody ClientDTO clientDTO);


    @PostMapping(API_PREFIX + "/clients-registrations")
    @ResponseBody
    Result<Boolean> delete(@RequestBody List<String> clientIds);

    @PostMapping(API_PREFIX + "/realm/{realmName}")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    Result<Boolean> createRealm(@PathVariable("realmName") String realmName);

    @DeleteMapping(API_PREFIX + "/realm/{realmName}")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    Result<Boolean> deleteRealm(@PathVariable("realmName") String realmName);

}
