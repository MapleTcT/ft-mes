package com.supcon.supfusion.auth.manager.feign.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(value = "keycloak", path = "/auth/realms")
public interface TokenClient {

    @PostMapping(value = "/{tenant}/protocol/openid-connect/logout", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    String keycloakLogout(@PathVariable("tenant") String tenant, Map<String, ?> formParams, @RequestHeader("Authorization") String contentType);


    @PostMapping(value = "/{tenant}/protocol/openid-connect/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    String keycloakLogin(@PathVariable("tenant") String tenant, Map<String, ?> formParams);

    @PostMapping(value = "/{tenant}/protocol/openid-connect/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    String refreshToken(@PathVariable("tenant") String tenant, Map<String, ?> formParams, @RequestHeader("Authorization") String authorization);

    @GetMapping(value = "/{tenant}/protocol/openid-connect/userinfo")
    String getUserInfo(@PathVariable("tenant") String tenant,  @RequestHeader("Authorization") String authorization);

}
