package com.supcon.supfusion.rbac.webapi.vo.role;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.*;

import java.util.List;

/**
 * <p>
 * 角色表
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
public class RoleTreeSingleCompanyVO extends VO {

    private static final long serialVersionUID=1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 角色类型
     */
    private String roleType;

    /**
     * 描述
     */
    private String description;

    /**
     * 名称
     */
    private String name;

    /**
     * 编码
     */
    private String code;

}
