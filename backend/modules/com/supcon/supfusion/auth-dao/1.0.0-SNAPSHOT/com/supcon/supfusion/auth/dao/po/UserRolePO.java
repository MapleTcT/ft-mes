package com.supcon.supfusion.auth.dao.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.auth.common.constants.Constants;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import lombok.*;

/**
 * @author lifangyuan
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = Constants.AUTH_ROLE_NAME, autoResultMap = true)
public class UserRolePO extends BaseEntity {
    private Long id;
    private Long userId;
    private Long roleId;
    private Integer roleType;
}
