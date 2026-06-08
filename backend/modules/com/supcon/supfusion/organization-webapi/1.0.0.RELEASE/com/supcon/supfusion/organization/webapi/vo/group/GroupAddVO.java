package com.supcon.supfusion.organization.webapi.vo.group;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.organization.common.constants.Constants;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 组新增的参数VO
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class GroupAddVO extends VO {
    /**
     * 组编码
     */
    @NotBlank(message = Constants.GROUP_PARAM_CODE_NECESSARY)
    @Size(min = 1, max = 50, message = Constants.GROUP_PARAM_CODE_LENGTH_ERROR)
    @Pattern(regexp = "^[0-9a-zA-Z_]{1,}$", message = Constants.ORG_CODE_PATTERN)
    private String code;

    /**
     * 组名称
     */
    @NotBlank(message = Constants.GROUP_PARAM_NAME_NECESSARY)
    @Size(min = 1, max = 50, message = Constants.GROUP_PARAM_NAME_LENGTH_ERROR)
    private String name;


    /**
     * 所属公司id
     */
    @NotNull(message = Constants.GROUP_PARAM_COMPANYID_NECESSARY)
    private Long companyId;

    /**
     * 描述
     */
    @Size(max = 500, message = Constants.GROUP_PARAM_DESC_LENGTH_ERROR)
    private String description;


    private List<Long> managerIds;
}
