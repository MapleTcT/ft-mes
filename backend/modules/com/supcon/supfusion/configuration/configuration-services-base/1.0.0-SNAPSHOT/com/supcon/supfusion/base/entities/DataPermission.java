/**
 * 
 */
package com.supcon.supfusion.base.entities;

import com.supcon.supfusion.base.enums.DataPermissionType;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractIdEntity;
import lombok.Data;
import org.hibernate.annotations.Immutable;

import javax.persistence.*;
import java.io.Serializable;


@Data
@Entity
@Immutable
@Table(name = DataPermission.TABLE_NAME)
public class DataPermission extends AbstractIdEntity implements Serializable {

	private static final long serialVersionUID = 3231984137432906763L;
	public static final String TABLE_NAME = "base_datapermission";

	// flowKey+flowVersion确定一个唯一的流程
	private String flowKey;// 流程KEY
	private String flowVersion;// 流程版本
	private String activityCode;// 活动编码
	private Long typeId;// 根据数据权限类型对应的id：
	@Enumerated(EnumType.STRING)
	private DataPermissionType dataPermissionType;// 数据权限类型
	private Boolean positionPowerFlag;// 岗位限制
	private Boolean groupPowerFlag;// 组限制
	private Boolean unlimitedPower;// 无限制
	private String memo;// 备注
	private Integer purviewState;// 权限的来源：1流程,2开始活动
	private Integer purviewDistribution;	//权限分配来源，3工作流分配的权限
	private String entityCode;
	@Column(name = "ASSIGN_POS_FLAG",columnDefinition = "INTEGER")
	private Boolean assignPosFlag;// 指定岗位：0 1
	@Column(name = "ASSIGN_STAFF_FLAG",columnDefinition = "INTEGER")
	private Boolean assignStaffFlag;// 指定人员：0 1

	@Override
	protected String _getEntityName() {
	
		return DataPermission.class.getName();
	}
}
