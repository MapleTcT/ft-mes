package com.supcon.supfusion.organization.service.bo.position;

import lombok.*;

/**
 * 岗位的角色关联
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PositionRoleBO {


    /**
     * 角色id
     */
    private Long id;

    /**
     * 角色名称
     */
    private String name;
}
