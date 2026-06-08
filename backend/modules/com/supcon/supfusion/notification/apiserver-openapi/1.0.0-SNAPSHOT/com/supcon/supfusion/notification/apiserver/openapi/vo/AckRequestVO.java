package com.supcon.supfusion.notification.apiserver.openapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Data
@ApiModel(value = "消息状态")
public class AckRequestVO extends VO {
    /**
     * 消息协议类型
     */
    @NotNull(message = "消息状态不能为空")
    @Size(min = 1)
    @ApiModelProperty(value = "消息状态类型", required = true)
    List<AckVO> acks;
}
