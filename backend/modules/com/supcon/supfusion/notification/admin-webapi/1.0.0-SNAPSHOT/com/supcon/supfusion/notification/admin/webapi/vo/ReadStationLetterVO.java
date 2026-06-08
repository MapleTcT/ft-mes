package com.supcon.supfusion.notification.admin.webapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@ApiModel
public class ReadStationLetterVO extends VO {
    @NotNull
    @ApiModelProperty("")
    private Long startTime;

    @NotNull
    @ApiModelProperty("")
    private Long endTime;

    @ApiModelProperty("协议类型")
    private String protocol;

    @ApiModelProperty("消息id集合")
    private List<String> messageIds;
}
