package com.supcon.supfusion.organization.webapi.vo.position;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.*;

/**
 * 岗位的角色关联
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PositionRoleVO extends VO {


    /**
     * 角色id
     */
    private Long id;

    /**
     * 角色名称
     */
    private String name;
}
