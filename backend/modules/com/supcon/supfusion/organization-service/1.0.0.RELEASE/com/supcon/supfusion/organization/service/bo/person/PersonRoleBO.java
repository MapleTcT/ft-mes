package com.supcon.supfusion.organization.service.bo.person;

import lombok.*;

/**
 * 人员角色页面的加载
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PersonRoleBO {

    /**
     * 角色id
     */
    private Long id;

    /**
     * 角色名
     */
    private String name;
}
