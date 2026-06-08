package com.supcon.supfusion.auth.manager;


import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.representations.idm.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Component
@Slf4j
public class KeycliandAdminClient {

    private static Pattern pattern = Pattern.compile("(?<=\\()(.+?)(?=\\))");

    @Resource
    private NamingService namingService;
    @Value("${supfusion.cloud.registry.group}")
    private String groupName;

    private Keycloak keycloak;


    @PostConstruct
    public void init() throws NacosException {
        try {
            namingService.subscribe("keycloak", event -> {
                log.info("sadsfds==" + event.toString());
                if (event instanceof NamingEvent) {
                    NamingEvent namingEvent = (NamingEvent) event;
                    List<Instance> instances = namingEvent.getInstances();
                    log.info("keycloak host==" + instances.get(0).getIp());
                    log.info("keycloak port ==" + instances.get(0).getPort());
                    if (keycloak!=null) {
                        keycloak.close();
                    }
                    keycloak = KeycloakBuilder.builder().realm("master")
                            .clientId("admin-cli")
                            .username("admin")
                            .password("admin")
                            .serverUrl("http://" + instances.get(0).getIp() + ":" + instances.get(0).getPort() + "/auth")
                            .build();
                }
            });
            Instance instance = namingService.selectOneHealthyInstance("keycloak", groupName, true);
            keycloak = KeycloakBuilder.builder().realm("master")
                    .clientId("admin-cli")
                    .username("admin")
                    .password("admin")
                    .serverUrl("http://" + instance.getIp() + ":" + instance.getPort() + "/auth")
                    .build();
        } catch (Exception e) {
            log.info("keycloak is not ok");
            System.exit(0);
        }
    }

    public Keycloak getKeycloak() {
        return keycloak;
    }

//    public LoginConfigBO getLoginConfig() {
//        RealmRepresentation supos = keycloak.realm(RpcContext.getContext().getTenantId()).partialExport(false, false);
//        String passwordPolicy = supos.getPasswordPolicy();
//        if (StringUtils.isNotEmpty(passwordPolicy)) {
//            Matcher matcher = pattern.matcher(passwordPolicy);
//            if (matcher.find()) {
//                return JSON.parseObject(matcher.group(1), LoginConfigBO.class);
//            }
//            ;
//        }
//        return null;
//    }

    public void createRealm(String realmName) {
            RealmRepresentation realmRepresentation = new RealmRepresentation();
            realmRepresentation.setRealm(realmName);
            realmRepresentation.setSsoSessionIdleTimeout(5184000);
            realmRepresentation.setSsoSessionMaxLifespan(5184000);
            realmRepresentation.setEnabled(true);
            realmRepresentation.setSslRequired("none");
            ClientRepresentation client = new ClientRepresentation();
            client.setClientId("pc" + "_" + realmName);
            client.setEnabled(true);
            client.setDirectAccessGrantsEnabled(true);
            client.setServiceAccountsEnabled(true);
            client.setStandardFlowEnabled(false);
            client.setImplicitFlowEnabled(false);
            HashMap<String, String> attributes = new HashMap<>();
            attributes.put("access.token.lifespan", "1800");
            attributes.put("client.session.max.lifespan", "43200");
            attributes.put("client.session.idle.timeout", "43200");
            client.setPublicClient(true);
            client.setBearerOnly(false);
            client.setAttributes(attributes);
            client.setDefaultClientScopes(Lists.newArrayList("supos"));

            ClientRepresentation client1 = new ClientRepresentation();
            client1.setClientId("mobile" + "_" + realmName);
            client1.setEnabled(true);
            client1.setDirectAccessGrantsEnabled(true);
            client1.setServiceAccountsEnabled(true);
            client1.setStandardFlowEnabled(false);
            client1.setImplicitFlowEnabled(true);
            HashMap<String, String> attributes1 = new HashMap<>();
            attributes1.put("access.token.lifespan", "2592000");
            attributes1.put("client.session.max.lifespan", "5184000");
            attributes1.put("client.session.idle.timeout", "5184000");
            client1.setPublicClient(true);
            client1.setBearerOnly(false);
            client1.setAttributes(attributes1);
            client1.setDefaultClientScopes(Lists.newArrayList("supos"));


            ArrayList<ClientRepresentation> clientRepresentations = new ArrayList<>();
            clientRepresentations.add(client);
            clientRepresentations.add(client1);
            realmRepresentation.setClients(clientRepresentations);

            ClientScopeRepresentation clientScopeRepresentation = new ClientScopeRepresentation();
            clientScopeRepresentation.setName("supos");
            clientScopeRepresentation.setProtocol("openid-connect");
            Map<String, String> clientScopeAttributes = new HashMap<>();
            clientScopeAttributes.put("display.on.consent.screen", "true");
            clientScopeAttributes.put("include.in.token.scope", "true");
            ArrayList<ClientScopeRepresentation> clientScopeRepresentations = buidClientScopeRepresentation(clientScopeRepresentation);
            realmRepresentation.setClientScopes(clientScopeRepresentations);


            MultivaluedHashMap<String, ComponentExportRepresentation> stringComponentExportRepresentationMultivaluedHashMap = new MultivaluedHashMap<>();
            ComponentExportRepresentation componentExportRepresentation = new ComponentExportRepresentation();
            componentExportRepresentation.setName("readonly-property-file");
            componentExportRepresentation.setProviderId("readonly-property-file");
            MultivaluedHashMap<String, String> config = new MultivaluedHashMap();
            config.put("cachePolicy", Lists.newArrayList("NO_CACHE"));
            config.put("enabled", Lists.newArrayList("true"));
            config.put("priority", Lists.newArrayList("0"));
            componentExportRepresentation.setConfig(config);

            ComponentExportRepresentation componentExportRepresentation1 = new ComponentExportRepresentation();
            componentExportRepresentation1.setName("rsa");
            componentExportRepresentation1.setProviderId("rsa");
            MultivaluedHashMap<String, String> config1 = new MultivaluedHashMap();
            config1.put("active", Lists.newArrayList("true"));
            config1.put("algorithm", Lists.newArrayList("RS256"));
            config1.put("certificate", Lists.newArrayList("MIICmTCCAYECBgF0C8a8ojANBgkqhkiG9w0BAQsFADAQMQ4wDAYDVQQDDAVzdXBvczAeFw0yMDA4MjAxMjA4MjdaFw0zMDA4MjAxMjEwMDdaMBAxDjAMBgNVBAMMBXN1cG9zMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtkvoXs7oFBaeUS+I6Ha1SFbz4owQP4YZ0af/ujDAj0BWbHBbGQXbOwSr/Kw6eFPzxz2BfQTvrtaAtAoo5//ZS6S8wsGfXnwSkkXyMYrye+OJfsCGHv0FfSbSvfs6Agp+8E9A/ScB/fL/kMvvVxvr+1LeXr8Kc4R5woydaHto4CjD6ix7Jbfaq+UVS7RT2TE+TGBjpbcUfxceygVaX8lt7s48z0dcwg8gJEk4MwIVCC5iA44tZS3bXBLBUaZsx4VC5dK+4c5PXNDADWHXKF2w+U70MoB5vSR5RzADWO5slQf2h3Vt2Hb7cFPdhGBoWzrTfmdzRM+Kii8bfLxrYpz2WQIDAQABMA0GCSqGSIb3DQEBCwUAA4IBAQCfnbCOLVBfRH4NqxpP6IdYDDFCgH5X4y0VGrOlFwNtycJTbciRFMOj2rzUatEs00WH7uUiPX13LIunQRiqrftg2J+m0yK84D3UB22RUPsMLHtpa6bIJbjMiS1WeezPgyPVgvv4CZZHiZW3aq70IKjW37RkwLgsPgyECQBB9ChPDtEGAckkD+AtLduaTq11mc4mFC597B2dpU1QO3Ogupd/h/KGIu2TiXGKIYLxxkqoXsswAponSFmE8b8h3XrP66t4TbhfaU3NO/lUbv3Dl2KktQkqcwzzTgytby+cQRKoIkn/rpu+7qmFsw+1GkJQnyUA1dtBCdL94Fy2cJrrTzJx"));
            config1.put("enabled", Lists.newArrayList("true"));
            config1.put("priority", Lists.newArrayList("100"));
            config1.put("privateKey", Lists.newArrayList("MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQC2S+hezugUFp5RL4jodrVIVvPijBA/hhnRp/+6MMCPQFZscFsZBds7BKv8rDp4U/PHPYF9BO+u1oC0Cijn/9lLpLzCwZ9efBKSRfIxivJ744l+wIYe/QV9JtK9+zoCCn7wT0D9JwH98v+Qy+9XG+v7Ut5evwpzhHnCjJ1oe2jgKMPqLHslt9qr5RVLtFPZMT5MYGOltxR/Fx7KBVpfyW3uzjzPR1zCDyAkSTgzAhUILmIDji1lLdtcEsFRpmzHhULl0r7hzk9c0MANYdcoXbD5TvQygHm9JHlHMANY7myVB/aHdW3YdvtwU92EYGhbOtN+Z3NEz4qKLxt8vGtinPZZAgMBAAECggEAGyD2xs0x2nN3Qgu56yZnWgmep9NCQ8HYK8w9kV7Z+TeQ5PhfhjzJ4GGUmriixu9vcvIjSBCo/rLrWl+8sLHxi4FLmgwohG4gcWY5YcgUx/0XpCZQj2IGJBbX++/cNRuMwoR43nOkpz83azv+NU9xcehmMWFq08AeQqacckUTWXBXR1X4tKC3C10KaDeU0307R8ZENNJL9+kAMF9c65QBzKni13UVVeLT485V6RCwrft9eEn7MmkXAi6ApSvnAzr9fuHSZfFIC/9LAyLhiJYMqyK3SEiFDlJngHRgNgqIFgzDgb4AqXKCHGCFsXc9Lx3nI99jaPrMlBQZvyVS2NGIRQKBgQDgaOUB87mg6BKxZZwI04P5h06of1H7RN2YeUORxpy+5vZBNe3J7+8jnnC01Ljazc6t6Ec1ek5qIHVzNWYBvzsboGaNm33cxmthiZi/ICNzlFxpHD2fjJVUglVfkaI7ZFoCFoxvxxHxUKGgSacuKStgnU9vYFrksWj9FrBu1HLPwwKBgQDP9V53Q2/qcL5eZ/jR9oev45uj6vtSwiOu9t5BXlPjiTQnWJIrnPfA7CflQ5oa8te5rU+k1MpamYSBRuQLnb9iuh1IWmcS7mXQrwurFtvYGN6i13r2W7m/0lKaecRNGmYLye5c3XiUdJ6GW4kmSjuXebAQe2DKGVO0891epEF7swKBgGIrwTNPaf+IRtUwPEhoL63zkWeI+1ZO1Bolwnd9SYkCBOyWKQZUXmtYnrmc7Zlau3W8zZoJfVBUDs1tqMhO3g7B5ttAEJmKe+NZjGbgKmIfnyWkYxjvKUylD7AVR8FvryiGsL0dey30NiCm1+oLvJwxdVSl9F1jdyhwypJRgkB9AoGBAJyq8QEOiBjp2TFMSRL9FJn48j9afv6JDdL5XtWGV9K6gdUGkBBVT/1CrIe3FzkoLEdQ/whh2xTIRSATpSfvLskVB4yDttV3TrMZvMOnE/bIPaoWhidlURnnPJ3uEGo58hj9hxrlKrtE5Ey6VyfGkwB6B59TI8b9r6dKdUKgokirAoGBAIpKhdZn042kzRDaNthyYCLYA1fW2leI0HH1q9PghnVlmh3FbCprNHsMZmQr/pc2UL5RqHP7uH5pee53mbjwAIQPVCwp1gFSiydUinpMEWveaXjTBdwXlYrcH42FmgvpmH3aXTLYj1Q2/HUbX0zU3wIT8p0VcZ83nW9eQifYX/Nt"));

            componentExportRepresentation1.setConfig(config1);


            stringComponentExportRepresentationMultivaluedHashMap.put("org.keycloak.keys.KeyProvider", Lists.newArrayList(componentExportRepresentation1));
            stringComponentExportRepresentationMultivaluedHashMap.put("org.keycloak.storage.UserStorageProvider", Lists.newArrayList(componentExportRepresentation));
            realmRepresentation.setComponents(stringComponentExportRepresentationMultivaluedHashMap);
            keycloak.realms().create(realmRepresentation);
            AuthenticationFlowRepresentation authenticationFlowRepresentation = authenticationFlowRepresentation(realmName);

            keycloak.realm(realmName).flows().createFlow(authenticationFlowRepresentation);
            Map<String, String> map = new HashMap<>();
            map.put("provider", "lfy-grant-validate-username");
            keycloak.realm(realmName).flows().addExecution("lfy grant", map);
            RealmRepresentation realmRepresentation1 = keycloak.realm(realmName).partialExport(false, true);
            realmRepresentation1.setDirectGrantFlow("lfy grant");
            keycloak.realm(realmName).update(realmRepresentation1);
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

    private AuthenticationFlowRepresentation authenticationFlowRepresentation(String realm) {
        AuthenticationFlowRepresentation authenticationFlowRepresentation = new AuthenticationFlowRepresentation();
        authenticationFlowRepresentation.setAlias("lfy grant");
        authenticationFlowRepresentation.setDescription("OpenID Connect Resource Owner Grant");
        authenticationFlowRepresentation.setBuiltIn(false);
        authenticationFlowRepresentation.setTopLevel(true);
        authenticationFlowRepresentation.setProviderId("basic-flow");
        return authenticationFlowRepresentation;
    }

    public void destroyRealm(String realmName) {
        keycloak.realm(realmName).remove();
    }


}
