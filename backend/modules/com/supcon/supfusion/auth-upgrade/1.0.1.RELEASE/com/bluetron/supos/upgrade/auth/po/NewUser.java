package com.bluetron.supos.upgrade.auth.po;

import lombok.Data;

@Data
public class NewUser {

    private Long id;

    private String password;

    private Long companyId;

    private Long currentCompanyId;

    private String userName;

    private Long personId;

    private String timeZone;

    private String faceUrl;

    private Boolean hasLock;

    private Boolean loginFirst;

    private String description;

    private Integer userType;

}
