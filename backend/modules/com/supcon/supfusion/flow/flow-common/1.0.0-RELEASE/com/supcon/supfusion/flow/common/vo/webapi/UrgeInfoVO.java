/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.vo.webapi;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2020年8月26日 上午9:20:41
 */
@Data
@JsonInclude(value = Include.NON_NULL)
@ApiModel(value = "UrgeInfoVO", description = "催办模型")
public class UrgeInfoVO extends VO {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    /**
     * 单据地址
     */
    @ApiModelProperty(value = "待办任务ID", name = "taskId", dataType = "String", example = "56789222214000")
    private String taskId;
    /**
     * 单据地址
     */
    @ApiModelProperty(value = "单据地址", name = "url", dataType = "String", example = "/page/1234567890")
    private String url;
    
    @ApiModelProperty(value = "待办接收时间", name = "startTime", dataType = "String", example = "2020-09-01T11:11:11.000+0000")
    private String startTime;
    /**
     * 单据编号
     */
    @ApiModelProperty(value = "单据编号", name = "formNo", dataType = "String", example = "1234567890")
    private String formNo;
    /**
     * 表单数据
     */
    @ApiModelProperty(value = "单据数据", name = "formData", example = "{\"a\": 1}")
    private String formData;
    /**
     * 待办ID
     */
    @ApiModelProperty(value = "待办名称", name = "taskName", dataType = "String", example = "审批")
    private String taskName;
    /**
     * 待办执行者列表
     */
    @ApiModelProperty(value = "待办执行者列表", name = "users")
    private List<SimpleUserVO> users;
    
    @ApiModelProperty(value = "通知方式", name = "protocols", example = "[{\"key\": \"email\", \"showName\": \"邮件\"}]")
    private List<ProtocolVO> protocols;
}
