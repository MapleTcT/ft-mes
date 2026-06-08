package com.supcon.supfusion.organization.webapi.vo.group;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.supcon.supfusion.framework.cloud.common.json.converters.IDJsonDeserializer;
import com.supcon.supfusion.framework.cloud.common.json.converters.IDJsonSerializer;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.organization.common.constants.Constants;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

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
public class GroupPersonVO extends VO {

    /**
     * 岗位ｉｄ
     */
    @NotNull(message = Constants.GROUP_PARAM_ID_NECESSARY)
    private Long groupId;
    /**
     * 关联人员id
     */
    private List<Long> persons;
}
