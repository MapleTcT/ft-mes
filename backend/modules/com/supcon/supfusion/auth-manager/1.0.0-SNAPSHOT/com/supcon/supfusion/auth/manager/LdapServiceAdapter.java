package com.supcon.supfusion.auth.manager;

import com.supcon.supfusion.auth.common.dto.LdapDTO;

import javax.naming.ldap.LdapContext;

/**
 * 调用LDAP服务方法
 *
 * @author caokele
 */
public interface LdapServiceAdapter {

    /**
     * 认证
     */
    void ldapAuthenticate(LdapDTO ldapDTO);

    /**
     * 测试连通性
     */
    void ldapTestConnectivity(LdapDTO ldapDTO);

    /**
     * 测试连接LDAP
     */
    void ldapTestConnect(LdapDTO ldapDTO);



    LdapContext getLdapContext(LdapDTO ldapDTO);

}
