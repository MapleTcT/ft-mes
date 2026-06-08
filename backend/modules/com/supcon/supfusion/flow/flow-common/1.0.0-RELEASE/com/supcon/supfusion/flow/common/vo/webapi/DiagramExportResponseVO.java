/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.vo.webapi;

import org.apache.ibatis.type.JdbcType;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.framework.scaffold.mybatis.type.handler.UTCToStringTypeHandler;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2020年5月19日 下午2:29:36
 */
@Data
@JsonInclude(value = Include.NON_NULL)
@ApiModel("流程组态导出模型")
public class DiagramExportResponseVO extends VO {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    /**
     * 公司ID
     */
    @ApiModelProperty(value = "公司id", name = "companyId", dataType = "String", example = "580038889177088(String)")
    private String companyId;
    /**
     * App ID
     */
    @ApiModelProperty(value = "app id", name = "appId", dataType = "String", example = "580038889177078(String)")
    private String appId;
    /**
     * 流程编号
     */
    @ApiModelProperty(value = "流程编号", name = "processKey", example = "K2002018123456789")
    private String processKey;
    /**
     * 流程名称
     */
    @ApiModelProperty(value = "流程名称", name = "processName", example = "请假流程")
    private String processName;
    /**
     * 组态数据
     */
    @ApiModelProperty(value = "流程组态JSON数据", name = "json", example = "{}")
    private String json;
    /**
     * 流程版本
     */
    @ApiModelProperty(value = "流程版本", name = "version", example = "1")
    private Integer version;
    
    @ApiModelProperty(value = "是否支持多公司", name = "multiCompany", example = "1")
    private Integer multiCompany;
    
    private String creator;
    /**
     * 创建时间
     */
    private String createTime;
    /**
     * 创建人员ID
     */
    private Long   createStaffId;

}
