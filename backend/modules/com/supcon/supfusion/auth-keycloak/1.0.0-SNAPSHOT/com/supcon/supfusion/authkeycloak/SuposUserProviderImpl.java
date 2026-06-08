package com.supcon.supfusion.authkeycloak;


import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.supcon.supfusion.authkeycloak.bcrypt.BCryptUtil;
import com.supcon.supfusion.authkeycloak.configure.PropertiesConfigure;
import com.supcon.supfusion.authkeycloak.constant.KeyCloakConstants;
import com.supcon.supfusion.authkeycloak.discovery.Registry;
import com.supcon.supfusion.authkeycloak.entity.UserEntity;
import com.supcon.supfusion.authkeycloak.http.HttpClientTool;
import com.supcon.supfusion.authkeycloak.http.ResponseEntity;
import lombok.extern.jbosslog.JBossLog;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;


@JBossLog
public class SuposUserProviderImpl implements UserStorageProvider, UserLookupProvider, CredentialInputValidator {

    protected KeycloakSession session;
    protected ComponentModel model;

    public SuposUserProviderImpl(KeycloakSession session, ComponentModel model) {
        this.session = session;
        this.model = model;

    }

    /**
     * 通过用户名查用户
     */
    @Override
    public UserModel getUserByUsername(String username, RealmModel realm) {
        try {
            log.info("enter getUserByUsername");
            Long companyId = retrieveCompanyId();
            String group = PropertiesConfigure.getProperties().getProperty("nacos.group", "DEFAULT_GROUP");
            Instance auth = Registry.getNamingService().selectOneHealthyInstance(KeyCloakConstants.AUTH, group);
            String userName = retrieveUserName();
            String url = String.format("http://%s:%d%s", auth.getIp(), auth.getPort(), KeyCloakConstants.USER_INFO_URI);
            HashMap<String, String> header = new HashMap<>();
            header.put(KeyCloakConstants.X_TENANT_IP, realm.getName());
            header.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
            HashMap<String, String> param = new HashMap<>();
            param.put("userName",  StringUtils.isEmpty(userName) ? username : userName);
            if (companyId != null) {
              param.put("companyId", companyId.toString());
            }
            ResponseEntity user = HttpClientTool.doGet(url,header,param);
//            ResponseEntity user = HttpClient.getUser(auth.getIp(), auth.getPort(), KeyCloakConstants.USER_INFO_URI, StringUtils.isEmpty(userName) ? username : userName, companyId, realm.getName());
            return new SuposUserAdapter(session, realm, model, user.getData());
        } catch (Exception e) {
            log.error("getUserByUsername error is", e);
            throw new RuntimeException(e);
        }
    }


    /**
     * 通过用户ID查血用户
     */
    @Override
    public UserModel getUserById(String id, RealmModel realm) {
        log.info("id: " + id);
        StorageId storageId = new StorageId(id);
        String username = storageId.getExternalId();
        return getUserByUsername(username, realm);
    }

    /**
     * 通过邮箱查找用户
     */
    @Override
    public UserModel getUserByEmail(String email, RealmModel realm) {
        return null;
    }

    @Override
    public void close() {
        // nothing to do
    }

    /**
     * 运行时将调用该方法，以确定是否为用户配置了特定的凭据类型。此方法检查是否已为用户设置了密码
     */
    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        return true;
    }


    /**
     * 方法负责验证密码。
     * 该CredentialInput参数实际上只是所有凭证类型的抽象接口。
     * 我们确保我们支持凭证类型，并且它也是的实例UserCredentialModel。
     * 当用户通过登录页面登录时，密码输入的纯文本将放入的实例UserCredentialModel。
     * 该isValid()方法根据存储在属性文件中的纯文本密码检查此值。返回值true表示密码有效。
     */
    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        String password = input.getChallengeResponse();
        if (StringUtils.isEmpty(password)) {
            return false;
        }
        if (!supportsCredentialType(input.getType())) {
            return false;
        }
        if (user instanceof SuposUserAdapter) {
            SuposUserAdapter suposUserAdapter = (SuposUserAdapter) user;
            UserEntity userEntity = suposUserAdapter.getEntity();
            Boolean AD = ldapEnable();
            if (!AD) {
                return validNormalAccount(suposUserAdapter.getPassword(), password);
            } else {
                return validLdapAccount(userEntity.getUserName(), password);
            }
        }
        return false;
    }

    private boolean validLdapAccount(String username, String password) {
        try {
            String group = PropertiesConfigure.getProperties().getProperty("nacos.group", "DEFAULT_GROUP");
            Instance auth = Registry.getNamingService().selectOneHealthyInstance(KeyCloakConstants.AUTH, group);
            String tenantId = retrieveTenantId();
            String url = String.format("http://%s:%d%s", auth.getIp(), auth.getPort(), KeyCloakConstants.AD_AUTHENTICATE_URI);
            HashMap<String, String> header = new HashMap<>();
            header.put(KeyCloakConstants.X_TENANT_IP, tenantId);
            header.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
            HashMap<String, String> param = new HashMap<>();
            param.put("userName", username);
            param.put("password", password);
           return HttpClientTool.doPost(url,header,param);
//            return HttpClient.authenticateUserDirectory(auth.getIp(), auth.getPort(), KeyCloakConstants.AD_AUTHENTICATE_URI, username, password, tenantId);
        } catch (NacosException e) {
            log.error("Connect auth server failed!", e);
        }
        return false;
    }

    private boolean validNormalAccount(String srcPassword, String targetPassword) {
        log.info("====================srcPassword: {}" + srcPassword);
        log.info("+++++++++++++++++++++targetPassword: {}" + targetPassword);
        boolean matches = BCryptUtil.matches(targetPassword, srcPassword);
        if (!matches) {
            String md5Hex = DigestUtils.md5Hex(targetPassword);
            if (md5Hex.equals(srcPassword)) {
                return true;
            } else {
                String sha256Hex = DigestUtils.sha256Hex(targetPassword);
                if (sha256Hex.equals(srcPassword)) {
                    return true;
                } else {
                    if (srcPassword.equals(targetPassword)) {
                        return true;
                    } else {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return credentialType.equals(PasswordCredentialModel.TYPE);
    }

    private Long retrieveCompanyId() {
        HttpRequest request = session.getContext().getContextObject(HttpRequest.class);
        if (request != null) {
            String companyId = request.getDecodedFormParameters().getFirst("companyId");
            if (StringUtils.isNotBlank(companyId)) {
                return Long.valueOf(companyId);
            }
        }
        return null;
    }

    private String retrieveTenantId() {
        HttpRequest request = session.getContext().getContextObject(HttpRequest.class);
        if (request != null) {
            return request.getHttpHeaders().getHeaderString(KeyCloakConstants.X_TENANT_IP);

        }
        return null;
    }

    private String retrieveUserName() {
        HttpRequest request = session.getContext().getContextObject(HttpRequest.class);
        if (request != null) {
            String userName = request.getDecodedFormParameters().getFirst("username");
            if (StringUtils.isNotBlank(userName)) {
                return userName;
            }
        }
        return null;
    }

    private Boolean ldapEnable() {
        HttpRequest request = session.getContext().getContextObject(HttpRequest.class);
        if (request != null) {
            String ldap = request.getDecodedFormParameters().getFirst("ldap");
            if (StringUtils.isNotBlank(ldap)) {
                return Boolean.valueOf(ldap);
            }
        }
        return false;
    }
}
