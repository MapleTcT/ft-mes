package com.supcon.supfusion.custon.property.webapi.vo;

import com.supcon.supfusion.custon.property.server.bo.ViewEnabledStatusCodeBO;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * @author zhang yafei
 */
@Getter
@Setter
@ApiModel
@ToString
public class ViewEnabledStatusVO extends VO {

    @ApiModelProperty("字段code")
    private List<ViewEnabledStatusCodeBO> codes;

    @ApiModelProperty("模型管理id")
    private List<Long> ids;

    @ApiModelProperty("修改状态值")
    private Boolean enabled;

}
