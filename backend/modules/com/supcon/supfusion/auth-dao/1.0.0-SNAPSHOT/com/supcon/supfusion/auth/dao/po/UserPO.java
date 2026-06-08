package com.supcon.supfusion.auth.dao.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.auth.common.constants.Constants;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.LogicDeleteBaseEntity;
import lombok.*;

/**
 * @author lifangyuan
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = Constants.AUTH_USER, autoResultMap = true)
public class UserPO extends LogicDeleteBaseEntity {

    private Long id;

    private String password;

    private Long companyId;

    private Long currentCompanyId;

    private String userName;

    private Long personId;

    private String personName;

    private String personCode;

    private String timeZone;
    private String faceUrl;

    private Boolean hasLock;
    private Boolean loginFirst ;

    private String description;

    private Integer userType;

    private Long userDirectoryId;

    private Integer lockReason;

    private String ldapUserName;

    private String thirdIdentity;

    private String thirdSource;

}
