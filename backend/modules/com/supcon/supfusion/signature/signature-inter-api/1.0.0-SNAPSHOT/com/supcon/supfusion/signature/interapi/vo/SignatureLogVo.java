package com.supcon.supfusion.signature.interapi.vo;

import com.alibaba.fastjson.annotation.JSONField;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Future;
import java.util.Date;

/**
 * @author zhang yafei
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SignatureLogVo extends VO {

    private static final long serialVersionUID = 5283048782587954302L;

    @ApiModelProperty("签名日志id")
    private String uuid;

    @ApiModelProperty("业务主键")
    private String businessKey;

    private Boolean status = true;

    @ApiModelProperty("模型code")
    private String modelCode;

    @ApiModelProperty("模块code")
    private String moduleCode;

    @ApiModelProperty("首签人id")
    private Long firstUserId;

    @ApiModelProperty("首签人名字")
    private String firstUserName;

    @ApiModelProperty("首签人用户名")
    private String firstStaffName;

    @ApiModelProperty("首签人员工id")
    private Long firstStaffId;


    @ApiModelProperty("按钮名字")
    private String buttonName;

    @ApiModelProperty("实体名字")
    private String entityName;

    @ApiModelProperty("模块名字")
    private String moduleName;

    @ApiModelProperty("模型名字")
    private String modelName;

    @ApiModelProperty("签名类型")
    private String signatureType;

    @ApiModelProperty("实体code")
    private String entityCode;

    @ApiModelProperty("表id")
    private Long tableId;

    @ApiModelProperty("按钮code")
    private String buttonCode;

    @ApiModelProperty("地址")
    private String ipAddress;

    @ApiModelProperty("创建时间")
    private Date createTime;

    @ApiModelProperty("流程id")
    private Long processId;

    @ApiModelProperty("任务id")
    private Long taskId;

    @ApiModelProperty("任务没名字")
    private String taskName;

    @ApiModelProperty()
    private Long transitionId;

    @ApiModelProperty("流程名字")
    private String processName;

    @ApiModelProperty()
    private String transitionName;

    @ApiModelProperty("次签人id")
    private Long secondUserId;

    @ApiModelProperty("次签用户名")
    private String secondUserName;

    @ApiModelProperty("次签人员id")
    private Long secondStaffId;

    @ApiModelProperty("次签人员名字")
    private String secondStaffName;

    @ApiModelProperty("次签原因")
    private String secondReason;

    @ApiModelProperty("次签备注")
    private String secondRemark;

    @ApiModelProperty("首签原因")
    private String firstReason;

    @ApiModelProperty("首签备注")
    private String firstRemark;

    @ApiModelProperty("首签时间")
    @Future
    @JSONField(format="yyyy-MM-dd HH:mm:ss")
    private Date firstSignTime;

    @ApiModelProperty("次签时间")
    @Future
    @JSONField(format="yyyy-MM-dd HH:mm:ss")
    private Date secondSignTime;

    @ApiModelProperty()
    private String operateLogUuid;

}
