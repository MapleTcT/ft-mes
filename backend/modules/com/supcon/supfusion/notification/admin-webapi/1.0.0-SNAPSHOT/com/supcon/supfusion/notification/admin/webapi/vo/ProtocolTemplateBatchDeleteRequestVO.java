package com.supcon.supfusion.notification.admin.webapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.List;

@Getter
@Setter
@ApiModel
@ToString
public class ProtocolTemplateBatchDeleteRequestVO extends VO {
    @ApiModelProperty("模板ID")
    @NotEmpty(message = "模板ID不能为空")
    @Size(min = 1, message = "模板ID不能为空")
    private List<String> ids;
}
