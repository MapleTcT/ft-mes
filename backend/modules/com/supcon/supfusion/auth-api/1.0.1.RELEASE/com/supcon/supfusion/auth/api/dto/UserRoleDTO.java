package com.supcon.supfusion.auth.api.dto;

import com.supcon.supfusion.framework.cloud.common.pojo.DTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author lifangyuan
 */
@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRoleDTO extends DTO {

    /*
    关联用户
     */
    private List<Long> addUserIds;

    /*
    解除关联用户
     */
    private List<Long> deleteUserIds;

    private Long roleId;
}
