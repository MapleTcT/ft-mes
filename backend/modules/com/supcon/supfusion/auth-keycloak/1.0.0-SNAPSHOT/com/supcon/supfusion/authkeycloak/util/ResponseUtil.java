package com.supcon.supfusion.authkeycloak.util;

import com.supcon.supfusion.authkeycloak.constant.KeyCloakErrorEnum;
import com.supcon.supfusion.authkeycloak.entity.TokenResponse;
import org.keycloak.authentication.AuthenticationFlowContext;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author caokele
 */
public class ResponseUtil {

    public static void sendTokenResponse(AuthenticationFlowContext context, KeyCloakErrorEnum keyCloakErrorEnum) {
        context.getEvent().error(keyCloakErrorEnum.getError());
        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setCode(keyCloakErrorEnum.getCode());
        tokenResponse.setMessage(keyCloakErrorEnum.getMessage());
        Response challengeResponse = Response.status(Response.Status.OK).entity(tokenResponse).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON).build();
        context.failure(keyCloakErrorEnum.getFlowError(), challengeResponse);
    }
}
