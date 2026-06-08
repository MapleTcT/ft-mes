package com.supcon.supfusion.file.server.dao.po;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.io.Serializable;
import java.util.Date;

@Data
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "file_server_document", autoResultMap=true)
public class DocumentPO implements Serializable  {

	private static final long serialVersionUID = 7097916839851820072L;
    private long id;
	private String filePath; // 文档路径
	private String fileName; // 文档名称
	private String fileOrgType; // 文档类型
	private long fileSize; // 文件大小
	private long linkId;// 关联ID
	private String fileType; // 文件类型 pic：图片字段 attachment:普通附件 office:文档控件
	private Long mainModelId;// 主关联模型的ID，如是表单 则为tableInfoId
	private String sizeDis; // 文件大小(显示用)
	private String memo;// 备注
	private String propertyCode;
	private String showType;
	private String opener;
	private Date openTime;
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
    private String valid; //是否删除 1 使用 0 删除
    private Long version; //版本
    private String creator;//创建人
    private String modifier; //修改人
    private Date createTime;//创建时间
    private Date modifyTime;//更新时间
    private Long createStaffId; //创建人员id
    private Long modifyStaffId;//修改人员id
	private String tenantId;
}
