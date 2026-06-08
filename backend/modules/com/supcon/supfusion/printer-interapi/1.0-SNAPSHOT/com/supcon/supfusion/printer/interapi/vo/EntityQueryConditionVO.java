package com.supcon.supfusion.printer.interapi.vo;

import com.supcon.supfusion.printer.common.Constants;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import javax.validation.constraints.NotBlank;
import java.util.List;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class EntityQueryConditionVO {

    /**
     * 数据来源
     */
    @NotBlank(message = Constants.PARAM_SOURCE_NECESSARY)
    @ApiModelProperty(value = "数据源来源", required = true)
    private String source;


    /**
     * app或模块编码
     */
    @NotBlank(message = Constants.PARAM_APPID_NECESSARY)
    @ApiModelProperty(value = "app或模块编码", required = true)
    private String appCode;

    /**
     * 查询条件
     */

    private ParamConditionVO condition;

    /**
     * 需要的数据
     */
    private List<EntityConditionVO> resultData;




}
