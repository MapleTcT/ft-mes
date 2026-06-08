package com.supcon.supfusion.printer.openapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.printer.common.Constants;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PrinterRegisterVO extends VO {
    /**
     * 数据来源
     */
    @NotNull(message = Constants.PARAM_SOURCE_NECESSARY)
    @ApiModelProperty(value = "数据来源")
    private Integer source;

    /**
     * 服务地址
     */
    @NotBlank(message = Constants.PARAM_SERVICEURL_NECESSARY)
    @ApiModelProperty(value = "服务地址")
    private String serviceUrl;

    /**
     * 服务类型
     */
    @NotNull(message = Constants.PARAM_SERVICETYPE_NECESSARY)
    @ApiModelProperty(value = "服务类型")
    private Integer serviceType;

    /**
     * http请求方式
     */
    @NotNull(message = Constants.PARAM_CALLTYPE_NECESSARY)
    @ApiModelProperty(value = "http请求方式")
    private Integer callType;
}
