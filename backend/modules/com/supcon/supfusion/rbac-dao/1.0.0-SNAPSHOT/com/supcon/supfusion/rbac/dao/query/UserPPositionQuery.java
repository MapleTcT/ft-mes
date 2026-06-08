package com.supcon.supfusion.rbac.dao.query;

import com.supcon.supfusion.rbac.dao.enums.FlowPermissionType;
import lombok.Data;

import java.util.List;

/**
 * <p>
 * 工作流数据权限表
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-16
 */
@Data
public class UserPPositionQuery {


    private Long id;

    private Integer version;


    private Boolean includeLower;

    private Long positionId;

    private Long userPermissionId;

    private List<Long> userPermissionIds;
}
