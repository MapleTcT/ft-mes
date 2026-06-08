package com.supcon.supfusion.rbac.dao.query;

import lombok.Data;

/**
 * <p>
 * 角色表
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-05
 */
@Data
public class TagQuery extends LogicDeleteBaseEntityQuery {


    private String name;
    private Long roleCid;
    private String roleCode;
    private String roleName;
}
