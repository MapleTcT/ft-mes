package com.supcon.supfusion.organization.webapi.vo.group;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.organization.common.constants.Constants;
import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 组修改po
 * @author lifangyuan
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class GroupUpdateVO extends VO {

    /**
     * 组id
     */
    @NotNull(message = Constants.GROUP_PARAM_ID_NECESSARY)
    private Long id;

    /**
     * 组名称
     */
    @Size(min = 1, max = 50, message = Constants.GROUP_PARAM_NAME_LENGTH_ERROR)
    private String name;

    /**
     * 描述
     */
    @Size(max = 500, message = Constants.GROUP_PARAM_DESC_LENGTH_ERROR)
    private String description;

    private List<Long> managerIds;
}
