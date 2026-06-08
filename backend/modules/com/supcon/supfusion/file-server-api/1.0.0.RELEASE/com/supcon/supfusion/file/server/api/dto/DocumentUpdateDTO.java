package com.supcon.supfusion.file.server.api.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(description= "参数数据DTO")
public class DocumentUpdateDTO implements Serializable  {

	private static final long serialVersionUID = 7097916839851820072L;
	@ApiModelProperty(value = "id")
    private Long id;
	@ApiModelProperty(value = "关联ID")
	private Long linkId;// 关联ID
	@ApiModelProperty(value = "文件类型 pic：图片字段 attachment:普通附件 office:文档控件")
	private String fileType; // 文件类型 pic：图片字段 attachment:普通附件 office:文档控件
	@ApiModelProperty(value = "主关联模型的ID，如是表单 则为tableInfoId")
	private Long mainModelId;// 主关联模型的ID，如是表单 则为tableInfoId
	@ApiModelProperty(value = "文件大小(显示用)")
	private String sizeDis; // 文件大小(显示用)
	@ApiModelProperty(value = "备注")
	private String memo;// 备注
	@ApiModelProperty(value = "propertyCode")
	private String propertyCode;
	private String showType;
	private String opener;
	private Long openTime;
	@ApiModelProperty(value = "流程ID")
	private Long deploymentId;// 流程ID
	@ApiModelProperty(value = "活动CODE")
	private String activityName;// 活动CODE
	@ApiModelProperty(value = "活动描述")
	private String taskDescription;// 活动描述
	@ApiModelProperty(value = "文件图标类型")
	private String fileIcon;// 文件图标类型
	@ApiModelProperty(value = "是否启用附件预览")
	private Boolean isFileView; // 是否启用附件预览
	private String docContent;
	private String docSummary;
	@ApiModelProperty(value = "文档转换状态")
	private String convertStatus; // 文档转换状态
	@ApiModelProperty(value = "不支持转换原因")
	private String reason; // 不支持转换原因
	@ApiModelProperty(value = "转换后路径")
	private String convertPath; // 转换后路径
	private Long createStaffId;
	private Long modifyStaffId;

}
