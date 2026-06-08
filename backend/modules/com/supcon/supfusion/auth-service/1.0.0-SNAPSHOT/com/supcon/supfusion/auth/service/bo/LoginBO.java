package com.supcon.supfusion.auth.service.bo;

import lombok.Getter;
import lombok.Setter;


/**
 * @author lifangyuan
 */
@Getter
@Setter
public class LoginBO {

    private String userName;

    private String password;

    private String clientId;

    private String grantType;

    private Long companyId;

    private Boolean ldap;

}
