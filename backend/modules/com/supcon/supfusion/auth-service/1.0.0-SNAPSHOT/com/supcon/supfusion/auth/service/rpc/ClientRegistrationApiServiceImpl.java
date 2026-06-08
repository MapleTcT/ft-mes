package com.supcon.supfusion.auth.service.rpc;

import com.alibaba.nacos.api.naming.NamingService;
import com.google.common.collect.Lists;
import com.supcon.supfusion.auth.keycloak.client.api.ClientRegistrationApiService;
import com.supcon.supfusion.auth.keycloak.client.api.dto.ClientDTO;
import com.supcon.supfusion.auth.manager.KeycliandAdminClient;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author lifangyuan
 */
@ServiceApiService
@Slf4j
public class ClientRegistrationApiServiceImpl implements ClientRegistrationApiService {

    @Resource
    private NamingService namingService;
    @Resource
    private KeycliandAdminClient keycloakAdminClient;

    @Override
    public Result<Boolean> create(ClientDTO clientDTO) {
        try {
            ClientRepresentation client = new ClientRepresentation();
            client.setClientId(clientDTO.getClientId());
            client.setSecret(clientDTO.getSecret());
            client.setEnabled(clientDTO.getEnabled());
            client.setDirectAccessGrantsEnabled(clientDTO.getDirectAccessGrantsEnabled());
            client.setServiceAccountsEnabled(true);
            client.setStandardFlowEnabled(clientDTO.getStandardFlowEnabled());
            client.setImplicitFlowEnabled(clientDTO.getImplicitFlowEnabled());
            HashMap<String, String> attributes = new HashMap<>();
            attributes.put("access.token.lifespan", clientDTO.getAccessTokenLifespan());
            attributes.put("client.session.max.lifespan", clientDTO.getAccessTokenLifespan());
            attributes.put("client.session.idle.timeout", clientDTO.getAccessTokenLifespan());
            client.setPublicClient(false);
            client.setBearerOnly(false);
            client.setClientAuthenticatorType("client-secret");
            client.setRedirectUris(clientDTO.getRedirectUris());
            client.setAttributes(attributes);
            client.setDefaultClientScopes(Lists.newArrayList(RpcContext.getContext().getTenantId()));
            keycloakAdminClient.getKeycloak().realm(RpcContext.getContext().getTenantId()).clients().create(client);
            log.info(client.getRegistrationAccessToken());
        } catch (Exception e) {
            log.error("client registry error is", e);
        }
        return Result.data(true);
    }


    @Override
    public Result<Boolean> delete(List<String> clientIds) {
        for (String clientId : clientIds) {
            ClientsResource supos = keycloakAdminClient.getKeycloak().realm(RpcContext.getContext().getTenantId()).clients();
            List<ClientRepresentation> byClientId = supos.findByClientId(clientId);
            supos.get(byClientId.get(0).getId()).remove();
        }
        return Result.data(true);
    }

    @Override
    public Result<Boolean> createRealm(String realmName) {
        keycloakAdminClient.createRealm(realmName);
        return Result.data(true);
    }

    private AuthenticationFlowRepresentation authenticationFlowRepresentation(String realm) {
        AuthenticationFlowRepresentation authenticationFlowRepresentation = new AuthenticationFlowRepresentation();
        authenticationFlowRepresentation.setAlias("lfy grant");
        authenticationFlowRepresentation.setDescription("OpenID Connect Resource Owner Grant");
        authenticationFlowRepresentation.setBuiltIn(false);
        authenticationFlowRepresentation.setTopLevel(true);
        authenticationFlowRepresentation.setProviderId(realm);
        return authenticationFlowRepresentation;
    }

    private ArrayList<ClientScopeRepresentation> buidClientScopeRepresentation(ClientScopeRepresentation clientScopeRepresentation) {
        ArrayList<ClientScopeRepresentation> clientScopeRepresentations = new ArrayList<>();
        List<ProtocolMapperRepresentation> protocolMappers = new ArrayList<>();
        ProtocolMapperRepresentation protocolMapperRepresentation = buildProtocolMapper("userName", "userName", "user_name", "String");
        ProtocolMapperRepresentation protocolMapperRepresentation1 = buildProtocolMapper("userId", "id", "user_id", "long");
        ProtocolMapperRepresentation protocolMapperRepresentation2 = buildProtocolMapper("departmentCode", "departmentCode", "department_code", "String");
        ProtocolMapperRepresentation protocolMapperRepresentation3 = buildProtocolMapper("departmentId", "departmentId", "department_id", "long");
        ProtocolMapperRepresentation protocolMapperRepresentation4 = buildProtocolMapper("positionName", "positionName", "position_name", "String");
        ProtocolMapperRepresentation protocolMapperRepresentation5 = buildProtocolMapper("companyType", "companyType", "company_type", "String");
        ProtocolMapperRepresentation protocolMapperRepresentation6 = buildProtocolMapper("departmentName", "departmentName", "department_name", "String");
        ProtocolMapperRepresentation protocolMapperRepresentation7 = buildProtocolMapper("positionId", "positionId", "position_id", "long");
        ProtocolMapperRepresentation protocolMapperRepresentation8 = buildProtocolMapper("staffCode", "personCode", "staff_code", "String");
        ProtocolMapperRepresentation protocolMapperRepresentation9 = buildProtocolMapper("positionCompanyId", "positionCompanyId", "position_company_id", "long");
        ProtocolMapperRepresentation protocolMapperRepresentation10 = buildProtocolMapper("userType", "userType", "user_type", "int");
        ProtocolMapperRepresentation protocolMapperRepresentation11 = buildProtocolMapper("companyName", "companyName", "company_name", "String");
        ProtocolMapperRepresentation protocolMapperRepresentation12 = buildProtocolMapper("positionCode", "positionCode", "position_code", "String");
        ProtocolMapperRepresentation protocolMapperRepresentation13 = buildProtocolMapper("companyCode", "companyCode", "company_code", "String");
        ProtocolMapperRepresentation protocolMapperRepresentation14 = buildProtocolMapper("staffId", "personId", "staff_id", "long");
        ProtocolMapperRepresentation protocolMapperRepresentation15 = buildProtocolMapper("staffName", "personName", "staff_name", "String");
        ProtocolMapperRepresentation protocolMapperRepresentation16 = buildProtocolMapper("companyId", "companyId", "company_id", "long");
        protocolMappers.add(protocolMapperRepresentation);
        protocolMappers.add(protocolMapperRepresentation1);
        protocolMappers.add(protocolMapperRepresentation2);
        protocolMappers.add(protocolMapperRepresentation3);
        protocolMappers.add(protocolMapperRepresentation4);
        protocolMappers.add(protocolMapperRepresentation5);
        protocolMappers.add(protocolMapperRepresentation6);
        protocolMappers.add(protocolMapperRepresentation7);
        protocolMappers.add(protocolMapperRepresentation8);
        protocolMappers.add(protocolMapperRepresentation9);
        protocolMappers.add(protocolMapperRepresentation10);
        protocolMappers.add(protocolMapperRepresentation11);
        protocolMappers.add(protocolMapperRepresentation12);
        protocolMappers.add(protocolMapperRepresentation13);
        protocolMappers.add(protocolMapperRepresentation14);
        protocolMappers.add(protocolMapperRepresentation15);
        protocolMappers.add(protocolMapperRepresentation16);
        clientScopeRepresentation.setProtocolMappers(protocolMappers);
        clientScopeRepresentations.add(clientScopeRepresentation);
        return clientScopeRepresentations;
    }

    private ProtocolMapperRepresentation buildProtocolMapper(String name, String userAttribute, String claimName, String jsonType) {
        ProtocolMapperRepresentation protocolMapperRepresentation = new ProtocolMapperRepresentation();
        protocolMapperRepresentation.setName(name);
        protocolMapperRepresentation.setProtocol("openid-connect");
        protocolMapperRepresentation.setProtocolMapper("oidc-usermodel-attribute-mapper");
        Map<String, String> config = new HashMap();
        config.put("userinfo.token.claim", "true");
        config.put("user.attribute", userAttribute);
        config.put("id.token.claim", "true");
        config.put("access.token.claim", "true");
        config.put("claim.name", claimName);
        config.put("jsonType.label", jsonType);
        protocolMapperRepresentation.setConfig(config);
        return protocolMapperRepresentation;
    }

    @Override
    public Result<Boolean> deleteRealm(String realmName) {
        keycloakAdminClient.getKeycloak().realm(realmName).remove();
        return Result.data(true);
    }
}
