package com.supcon.supfusion.rbac.dao.query;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 菜单表
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-05
 */
@Data
public class MenuInfoMneCodeQuery implements Serializable {


    private static final long serialVersionUID = 196888854074728248L;
    private Long id;
    private String language;
    private Long menuInfoId;
    private String mneCode;
}
