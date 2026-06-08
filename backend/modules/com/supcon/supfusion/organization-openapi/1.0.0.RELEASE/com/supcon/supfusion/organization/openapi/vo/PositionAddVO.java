package com.supcon.supfusion.organization.openapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.organization.common.constants.Constants;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 岗位新增的参数VO
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PositionAddVO extends VO {
    /**
     * 岗位编码
     */
    @NotBlank(message = Constants.POSITION_PARAM_CODE_NECESSARY)
    private String code;

    /**
     * 岗位名称
     */
    @NotBlank(message = Constants.POSITION_PARAM_NAME_NECESSARY)
    private String name;

    /**
     * 所属公司id
     */
    @NotNull(message = Constants.POSITION_PARAM_COMPANYID_NECESSARY)
    private Long companyId;

    /**
     * 关联部门id
     */
    @NotNull(message = Constants.POSITION_PARAM_DEPID_NECESSARY)
    private Long depId;

    /**
     * 上级岗位id（如果上级是公司则为空）
     */
    private Long parentId;

    /**
     * 描述
     */
    private String description;
    /**
     * 负责人id
     */
    @ApiModelProperty(value = "负责人id")
    private List<Long> managerIds;
}
