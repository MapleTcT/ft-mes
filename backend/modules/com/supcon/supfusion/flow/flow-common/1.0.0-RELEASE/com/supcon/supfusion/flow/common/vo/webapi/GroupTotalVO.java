package com.supcon.supfusion.flow.common.vo.webapi;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author zhang yafei
 */
@Data
@ApiModel(value = "分组统计待办数量参数模型")
public class GroupTotalVO implements Serializable {

    @ApiModelProperty("当天待办数量")
    private Integer today;

    @ApiModelProperty("一周内的待办数量")
    private Integer week;

    @ApiModelProperty("所有待办数量")
    private Integer total;
}
