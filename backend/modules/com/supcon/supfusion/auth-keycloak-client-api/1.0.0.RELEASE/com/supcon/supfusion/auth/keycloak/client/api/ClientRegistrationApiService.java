package com.supcon.supfusion.auth.keycloak.client.api;

import com.supcon.supfusion.auth.keycloak.client.api.dto.ClientDTO;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@FeignClient(name = "auth", contextId = "keycloak")
public interface ClientRegistrationApiService {

    String API_PREFIX = "/service-api/auth/v2";

    @PostMapping(API_PREFIX + "/clients-registrations/default")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    Result<Boolean> create(@Validated @RequestBody ClientDTO clientDTO);

}
