package com.supcon.supfusion.auth.service.bo;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.auth.common.constants.Constants;
import com.supcon.supfusion.framework.cloud.common.pojo.PO;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = Constants.AUTH_ROLE_NAME, autoResultMap = true)
public class UserRoleBO extends PO {
    private Long id;

    private Long userId;

    private Long roleId;

    private String roleCode;

    private Integer roleType;

    private String roleName;
}
