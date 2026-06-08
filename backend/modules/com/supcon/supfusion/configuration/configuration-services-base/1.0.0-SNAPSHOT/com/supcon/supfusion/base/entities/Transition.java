package com.supcon.supfusion.base.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import com.supcon.supfusion.base.annotation.BAPInternational;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueIdEntity;
import lombok.Data;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Columns;
import org.hibernate.annotations.Type;

/**
 * 迁移线信息
 * 
 * @author shichenwei
 * 
 */
@Data
@Entity
@Table(name = Transition.TABLE_NAME)
public class Transition extends AbstractAuditUniqueIdEntity {
	private static final long serialVersionUID = 5886041919705534880L;
	public static final String TABLE_NAME = "wf_transition";
	@BAPInternational(replace = true)
	@Type(type="com.supcon.supfusion.base.hibernate.InternationalType")
	@Columns(columns = {
			@Column(name="name"),
			@Column(name="name_zh_cn")
	})
	private String name;//名称
	private String code;//编码
	private Integer type;//1普通迁移线2驳回线
	private String fromNodeCode;//起始活动
	private String toNodeCode;//目标活动
	private Long deploymentId;//流程id
	private String selectStaff;//是否可选人0，否，1是，2跨公司选人、3本部门、4本部门及下级、5自定义
	private Boolean requiredStaff;//选人必填
	private String Expression;//表达式
	private Integer routeSequence;//序号
	@Column(name="DEFAULT_STAFF",columnDefinition="INTEGER")
	private Boolean defaultSelectStaff;//是否默认上次选人
	
	@Override
	protected String _getEntityName() {
		return Transition.class.getName();
	}

}
