package com.supcon.supfusion.rbac.api.dto;

import com.supcon.supfusion.framework.cloud.common.pojo.DTO;
import lombok.*;

/**
 * <p>
 * 角色用户表
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-05
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class RoleUserDTO extends DTO {


    private static final long serialVersionUID = 7783187344966180057L;
    private Long roleId;

    private Long userId;

    private String personName;

    private String userName;

    private String personCode;


}
