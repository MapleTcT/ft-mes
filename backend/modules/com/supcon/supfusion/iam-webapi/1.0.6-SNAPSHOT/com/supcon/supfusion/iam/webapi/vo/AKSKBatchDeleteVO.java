package com.supcon.supfusion.iam.webapi.vo;


import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Getter
@Setter
@ToString
public class AKSKBatchDeleteVO extends VO {
    @ApiModelProperty(value = "ids")
    @NotNull(message = "ids不能为空")
    @Size(min = 1)
    private List<Long> ids;
}
