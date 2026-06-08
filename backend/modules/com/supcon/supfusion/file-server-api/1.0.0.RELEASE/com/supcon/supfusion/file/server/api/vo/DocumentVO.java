package com.supcon.supfusion.file.server.api.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class DocumentVO implements Serializable  {

	private static final long serialVersionUID = 7097916839851820072L;
    private long id;
	private String path; // 文档路径
	private String name; // 文档名称
	private String type; // 文档类型
	private long size; // 文件大小
	private long linkId;// 关联ID
	private String fileType; // 文件类型 pic：图片字段 attachment:普通附件 office:文档控件
	private Long mainModelId;// 主关联模型的ID，如是表单 则为tableInfoId
	private String sizeDis; // 文件大小(显示用)
	private String memo;// 备注
	private String propertyCode;
	private String showType;
	private String opener;
	private long openTime;
	private Long deploymentId;// 流程ID
	private String activityName;// 活动CODE
	private String taskDescription;// 活动描述
	private String fileIcon;// 文件图标类型
	private Boolean isFileView; // 是否启用附件预览
	private String docContent;
	private String docSummary;
	private String convertStatus; // 文档转换状态
	private String reason; // 不支持转换原因
	private String convertPath; // 转换后路径
	private long downloadTimes = 0; // 下载次数
	private long previewTimes = 0; // 浏览次数
	private Date createTime;
	private Date modifyTime;
	private String createStaffId;
	private String createStaffName;//创建人员名称
	private String modifyStaffId;
	private String modifyStaffName;

}
