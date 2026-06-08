package com.supcon.supfusion.auth.service.bo;

import lombok.*;

import java.util.List;
import java.util.Set;

/**
 * @author lifangyuan
 */
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBO {

    private Long id;

    private String password;

    private String userName;

    private Long companyId;

    private String companyCode;

    private Long currentCompanyId;

    private Long personId;

    private String personCode;

    private String personName;

    private String timeZone;

    private Boolean hasLock;

    private String faceUrl;

    private String description;

    private Boolean valid;

    private Set<Long> company;

    private Integer userType;

    private Long userDirectoryId;

    private Integer lockReason;

    private String ldapUserName;

    private List<UserRoleBO> roles;

    private Boolean isServiceApi = false;

    private Boolean loginFirst;

    private String modifyTime;

    private String createTime;

    private String email;

    private String thirdIdentity;

    private String thirdSource;

}
