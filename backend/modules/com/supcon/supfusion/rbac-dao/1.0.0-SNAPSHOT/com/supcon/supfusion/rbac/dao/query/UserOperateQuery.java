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
public class UserOperateQuery extends LogicDeleteBaseEntityQuery {


    private String miCode;
    private Long userId;
    private String moCode;
    private String moCid;
}
