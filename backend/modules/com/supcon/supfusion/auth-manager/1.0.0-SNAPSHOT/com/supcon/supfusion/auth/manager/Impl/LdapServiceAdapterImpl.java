package com.supcon.supfusion.auth.manager.Impl;

import com.supcon.supfusion.auth.common.dto.LdapDTO;
import com.supcon.supfusion.auth.common.exception.UserDirectoryErrorEnum;
import com.supcon.supfusion.auth.common.exception.UserDirectoryException;
import com.supcon.supfusion.auth.manager.LdapServiceAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import java.util.Hashtable;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author caokele
 */
@Slf4j
@Service
public class LdapServiceAdapterImpl implements LdapServiceAdapter {
    private static final String FACTORY = "com.sun.jndi.ldap.LdapCtxFactory";
    private static final String LDAP_CONNECT_TIMEOUT = "com.sun.jndi.ldap.connect.timeout";
    private static final String DEFAULT_TIME = "3000";
    private static final String LDAP_URL = "ldap://%s:%d";
    private static final String LDAP_SSL_URL = "ldaps://%s:%d";
    private static final String SECURITY_AUTHENTICATION = "simple";

    @Override
    public void ldapAuthenticate(LdapDTO ldapDTO) {
        if (StringUtils.isEmpty(ldapDTO.getUserName()) || StringUtils.isEmpty(ldapDTO.getPassword())) {
            throw new UserDirectoryException(UserDirectoryErrorEnum.EMPTY_USERNAME_OR_PWD);
        }
        Hashtable<String, String> env = generateLdapEnv(ldapDTO.getHostname(), ldapDTO.getPort(), Optional.ofNullable(ldapDTO.getEnableSsl()).orElse(false));
        process(ldapDTO);
        // 账号
        env.put(Context.SECURITY_PRINCIPAL, ldapDTO.getUserName());
        // 密码
        env.put(Context.SECURITY_CREDENTIALS, ldapDTO.getPassword());
        LdapContext context = null;
        try {
            context = new InitialLdapContext(env, null);
            log.info("LDAP connect success!");
        } catch (javax.naming.AuthenticationException | javax.naming.InvalidNameException e) {
            log.error("Authentication failed!", e);
            throw new UserDirectoryException(UserDirectoryErrorEnum.ERROR_USERNAME_OR_PWD);
        } catch (Exception e) {
            log.error("LDAP connect failed!", e);
            throw new UserDirectoryException(UserDirectoryErrorEnum.CONNECT_LDAP_FAILED);
        } finally {
            if (context != null) {
                try {
                    context.close();
                } catch (NamingException e) {
                    log.error("Error Disconnect!", e);
                }
            }
        }
    }

    @Override
    public void ldapTestConnectivity(LdapDTO ldapDTO) {
        Hashtable<String, String> env = generateLdapEnv(ldapDTO.getHostname(), ldapDTO.getPort(), Optional.ofNullable(ldapDTO.getEnableSsl()).orElse(false));
        LdapContext context = null;
        try {
            context = new InitialLdapContext(env, null);
            log.info("LDAP connect success!");
        } catch (Exception e) {
            log.error("LDAP connect failed!", e);
            throw new UserDirectoryException(UserDirectoryErrorEnum.CONNECT_LDAP_FAILED);
        } finally {
            if (context != null) {
                try {
                    context.close();
                } catch (NamingException e) {
                    log.error("Error Disconnect!", e);
                }
            }
        }
    }

    /**
     * 生成ldap配置参数
     *
     * @param hostname  主机名
     * @param port      端口号
     * @param enableSsl 是否启用ssl
     */
    private Hashtable<String, String> generateLdapEnv(String hostname, Integer port, Boolean enableSsl) {
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, FACTORY);
        env.put(Context.SECURITY_AUTHENTICATION, SECURITY_AUTHENTICATION);
        // 连接超时时间
        env.put(LDAP_CONNECT_TIMEOUT, DEFAULT_TIME);
        // 请求链接
        String url = String.format(enableSsl ? LDAP_SSL_URL : LDAP_URL, hostname, port);
        env.put(Context.PROVIDER_URL, url);
        return env;
    }

    @Override
    public void ldapTestConnect(LdapDTO ldapDTO) {
        // 如果携带用户名，则需要认证
        if (!StringUtils.isEmpty(ldapDTO.getUserName())) {
            ldapAuthenticate(ldapDTO);
        } else {
            ldapTestConnectivity(ldapDTO);
        }
    }

    @Override
    public LdapContext getLdapContext(LdapDTO ldapDTO) {
        if (StringUtils.isEmpty(ldapDTO.getUserName()) || StringUtils.isEmpty(ldapDTO.getPassword())) {
            throw new UserDirectoryException(UserDirectoryErrorEnum.EMPTY_USERNAME_OR_PWD);
        }
        Hashtable<String, String> env = generateLdapEnv(ldapDTO.getHostname(), ldapDTO.getPort(), Optional.ofNullable(ldapDTO.getEnableSsl()).orElse(false));
        // 账号
        env.put(Context.SECURITY_PRINCIPAL, ldapDTO.getUserName());
        // 密码
        env.put(Context.SECURITY_CREDENTIALS, ldapDTO.getPassword());
        LdapContext context = null;
        try {
            context = new InitialLdapContext(env, null);
            log.info("LDAP connect success!");
        } catch (javax.naming.AuthenticationException | javax.naming.InvalidNameException e) {
            log.error("Authentication failed!", e);
            throw new UserDirectoryException(UserDirectoryErrorEnum.ERROR_USERNAME_OR_PWD);
        } catch (Exception e) {
            log.error("LDAP connect failed!", e);
            throw new UserDirectoryException(UserDirectoryErrorEnum.CONNECT_LDAP_FAILED);
        }
        return context;
    }

    public static void main(String[] args) {
//        String ss= "1234@ci.supos.com";
//        Pattern pattern = Pattern.compile("(^\\w+)@");
//        Matcher matcher = pattern.matcher(ss);
//        String sdsfdsdf = ss.replaceAll("(^\\w+)@", "sdsfdsdf"+"@");
//        System.out.println(sdsfdsdf);
//        if(matcher.find()){
//
//            System.out.println(matcher.group(0) + ", pos: " + matcher.start());
//            System.out.println(matcher.group(1) + ", pos: " + matcher.start(1));
//        }

        String ss = "userid=test03,ou=质量中心,ou=深蓝蓝卓,dc=wimpi,dc=net";
        Pattern pattern = Pattern.compile("(cn=\\w+),");
        Matcher matcher = pattern.matcher(ss);
        String sdsfdsdf = ss.replaceAll("(cn=\\w+),", "cn=" + "sdfsdfsdfsdf" + ",");
        System.out.println(sdsfdsdf);
        if (matcher.find()) {

            System.out.println(matcher.group(0) + ", pos: " + matcher.start());
            System.out.println(matcher.group(1) + ", pos: " + matcher.start(1));
        }
    }

    private void process(LdapDTO ldapDTO) {
        if ("sys_auth_user_directory/msad".equals(ldapDTO.getDirectoryType())) {
            if (!StringUtils.isEmpty(ldapDTO.getAdName())) {
                String[] split = ldapDTO.getUserName().split("@");
                String userName = ldapDTO.getAdName()+"@"+split[1];
                ldapDTO.setUserName(userName);
            }
        } else if ("sys_auth_user_directory/ldap".equals(ldapDTO.getDirectoryType())) {
            if (!StringUtils.isEmpty(ldapDTO.getAdName())) {
                String[] split = ldapDTO.getUserName().split("=");
                StringBuilder str = new StringBuilder();
                for (int i = 0; i < split.length; i++) {
                    if (i == 1) {
                        String[] split1 = split[1].split(",");
                        str.append(ldapDTO.getAdName()).append(",").append(split1[1]).append("=");
                    } else {
                        str.append(split[i]).append("=");
                    }
                }
                ldapDTO.setUserName(str.substring(0, str.length() - 1));
            }
        }
    }

}
