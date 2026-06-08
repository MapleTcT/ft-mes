package com.supcon.supfusion.auth.webapi.controller;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.google.common.collect.Lists;
import com.supcon.supfusion.auth.manager.KeycliandAdminClient;
import com.supcon.supfusion.auth.manager.SystemCodeServiceAdapter;
import com.supcon.supfusion.auth.manager.feign.client.TokenClient;
import com.supcon.supfusion.auth.service.AuthCenterService;
import com.supcon.supfusion.auth.webapi.vo.AuthCenterResponseVO;
import com.supcon.supfusion.auth.webapi.vo.AuthClientVO;
import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author caokele
 */
@Slf4j
@RestController
@InternalApi(path = "/inter-api/auth")
@Api(value = "认证中心相关文档", tags = "认证中心")
@Validated
public class AuthCenterController extends BaseController {
    @Autowired
    private AuthCenterService authCenterService;
    @Autowired
    private SystemCodeServiceAdapter systemCodeServiceAdapter;

    @Resource
    private KeycliandAdminClient keycloakAdminClient;

    @Resource
    private TokenClient tokenClient;

    @Resource
    private NamingService namingService;

    @SuppressWarnings("unchecked")
    @GetMapping(value = "/v1/auth-center")
    @ResponseBody
    @ApiOperation(value = "查询认证中心列表V1接口", httpMethod = "GET")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "current", value = "当前页号-默认1", required = false, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "pageSize", value = "每页数量-默认10", required = false, dataType = "String", paramType = "query"),
    })
    public Result<AuthCenterResponseVO> queryAuthCenters(@RequestParam(defaultValue = "1", required = false) Integer current, @RequestParam(defaultValue = "10", required = false) Integer pageSize) {
        RealmResource realm = keycloakAdminClient.getKeycloak().realm(RpcContext.getContext().getTenantId());
        RealmRepresentation realmRepresentation = realm.partialExport(false, true);
        AuthCenterResponseVO authCenter = new AuthCenterResponseVO();
        authCenter.setProtocolType("oauth2");
        authCenter.setAuthUrl("/auth/v2/oauth2/authorize");
        authCenter.setTokenUrl("/auth/v2/oauth2/token");
        authCenter.setSsoSessionMaxLifespan(realmRepresentation.getSsoSessionMaxLifespan());
        List<ClientRepresentation> list = realm.clients().findAll(null, false, true, current, pageSize);
        List<AuthClientVO> collect = list.stream().map(clientRepresentation -> {
            AuthClientVO authClientVO = new AuthClientVO();
            authClientVO.setId(clientRepresentation.getId());
            authClientVO.setClientId(clientRepresentation.getClientId());
            authClientVO.setSecret(clientRepresentation.getSecret());
            authClientVO.setDescription(clientRepresentation.getDescription());
            authClientVO.setEnabled(clientRepresentation.getAuthorizationServicesEnabled());
            authClientVO.setAccessTokenLifespan(clientRepresentation.getAttributes().get("access.token.lifespan"));
            authClientVO.setRedirectUris(clientRepresentation.getRedirectUris());
            authClientVO.setImplicitFlowEnabled(clientRepresentation.isImplicitFlowEnabled());
            authClientVO.setDirectAccessGrantsEnabled(clientRepresentation.isDirectAccessGrantsEnabled());
            authClientVO.setStandardFlowEnabled(clientRepresentation.isStandardFlowEnabled());
            authClientVO.setEnabled(clientRepresentation.isEnabled());
            authClientVO.setPublicClient(clientRepresentation.isPublicClient());
            return authClientVO;
        }).collect(Collectors.toList());
        authCenter.setPageSize(collect.size());
        authCenter.setTotal(realm.clients().findAll().size());
        authCenter.setCurrent(current);
        authCenter.setList(collect);
        return new Result<>(authCenter);
    }

    @PostMapping("/v1/auth-center")
    @ResponseStatus(HttpStatus.OK)
   public void create(@Validated @RequestBody AuthClientVO authClientVO) {
        try {
            if (authClientVO.getPublicClient()) {
                ClientRepresentation clientRepresentation = buildClient(authClientVO);
                clientRepresentation.setPublicClient(true);
                keycloakAdminClient.getKeycloak().realm(RpcContext.getContext().getTenantId()).clients().create(clientRepresentation);
            } else {
                ClientRepresentation clientRepresentation = buildClient(authClientVO);
                clientRepresentation.setPublicClient(false);
                clientRepresentation.setClientAuthenticatorType("client-secret");
                clientRepresentation.setSecret(authClientVO.getSecret());
                keycloakAdminClient.getKeycloak().realm(RpcContext.getContext().getTenantId()).clients().create(clientRepresentation);
            }
        } catch (Exception e) {
            log.error("client registry error is", e);
        }
    }

    private ClientRepresentation buildClient(AuthClientVO authClientVO) {
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(authClientVO.getClientId());
        client.setEnabled(authClientVO.getEnabled());
        client.setDirectAccessGrantsEnabled(authClientVO.getDirectAccessGrantsEnabled());
        client.setServiceAccountsEnabled(authClientVO.getStandardFlowEnabled());
        client.setImplicitFlowEnabled(authClientVO.getImplicitFlowEnabled());
        HashMap<String, String> attributes = new HashMap<>();
        String accessTokenLifespan = authClientVO.getAccessTokenLifespan();
        Integer maxLifespan = Integer.parseInt(accessTokenLifespan) * 2;
        attributes.put("access.token.lifespan", authClientVO.getAccessTokenLifespan());
        attributes.put("client.session.max.lifespan", "2592000");
        attributes.put("client.session.idle.timeout", "2592000");
        client.setAttributes(attributes);
        client.setBearerOnly(false);
        if (authClientVO.getImplicitFlowEnabled() || authClientVO.getStandardFlowEnabled()) {
            client.setRedirectUris(authClientVO.getRedirectUris());
        }
        client.setDefaultClientScopes(Lists.newArrayList("supos"));
        return client;
    }

    @DeleteMapping("/v1/auth-center/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void delete(@PathVariable("id") String clientId) throws NacosException {
        keycloakAdminClient.getKeycloak().realm(RpcContext.getContext().getTenantId()).clients().get(clientId).remove();
    }

    @PutMapping("/v1/auth-center")
    @ResponseStatus(HttpStatus.OK)
    public void updateClient(@Validated @RequestBody AuthClientVO authClientVO) {
        if (authClientVO.getPublicClient()) {
            ClientRepresentation clientRepresentation = buildClient(authClientVO);
            clientRepresentation.setPublicClient(true);
            keycloakAdminClient.getKeycloak().realm(RpcContext.getContext().getTenantId()).clients().get(authClientVO.getId()).update(clientRepresentation);
        } else {
            ClientRepresentation clientRepresentation = buildClient(authClientVO);
            clientRepresentation.setPublicClient(false);
            clientRepresentation.setClientAuthenticatorType("client-secret");
            clientRepresentation.setSecret(authClientVO.getSecret());
            keycloakAdminClient.getKeycloak().realm(RpcContext.getContext().getTenantId()).clients().get(authClientVO.getId()).update(clientRepresentation);
        }
    }

}
