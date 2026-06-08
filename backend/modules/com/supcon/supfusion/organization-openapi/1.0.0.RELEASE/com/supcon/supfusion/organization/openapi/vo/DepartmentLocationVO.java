package com.supcon.supfusion.organization.openapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.organization.common.constants.Constants;
import lombok.*;

import javax.validation.constraints.NotNull;

/**
 * 部门修改位置VO
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentLocationVO extends VO {

    /**
     * 部门id
     */
    @NotNull(message = Constants.DEPARTMENT_PARAM_ID_NECESSARY)
    private Long id;

    /**
     * 前续部门id
     */
    private Long upId;

    /**
     * 父级部门id
     */
    private Long parentId;

}
