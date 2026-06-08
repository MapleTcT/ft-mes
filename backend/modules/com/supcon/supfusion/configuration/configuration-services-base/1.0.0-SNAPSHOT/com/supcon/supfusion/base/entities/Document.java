package com.supcon.supfusion.base.entities;

import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueIdEntity;
import lombok.Data;
import org.hibernate.annotations.Immutable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.File;
import java.io.Serializable;
import java.util.Date;

@Data
@Entity
@Immutable
@Table(name = "base_document")
public class Document extends AbstractAuditUniqueIdEntity implements Serializable {

	private static final long serialVersionUID = 7097916839851820072L;

	private String name; // 文档名称
	private String path; // 文档路径
	private String type; // 文档类型
	private long linkId;// 关联ID
	private Long mainModelId;//主关联模型的ID，如是表单 则为tableInfoId
	private String memo;// 备注
	private long size; // 文件大小
	@Transient
	private long downloadTimes = 0; // 下载次数
	@Transient
	private String sizeDis; // 文件大小(显示用)
	private String propertyCode;
	private String fileType; //文件类型   pic：图片字段 attachment:普通附件  office:文档控件
	private String showType;
	
	private String opener;
	private Date openTime;
	private Long deploymentId;//流程ID
	private String activityName;//活动CODE
	private String taskDescription;//活动描述

	@Column(nullable = false, length = 512)
	public String getPath() {
		if(null == path) {
			path = String.format("%1$tY%4$s%1$tm%4$s%1$td%4$s%2$s_%1$tY%1$tm%1$td%1$tH%1$tM%1$tS%1$tL%4$s%3$s", getCreateTime(), getLinkId(), getName(), File.separatorChar);
		}
		return path;
	}

	@Transient
	private String docContent;
	@Transient
	private String docSummary;

	@Override
	protected String _getEntityName() {
		return Document.class.getName();
	}
}
