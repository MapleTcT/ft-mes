package com.supcon.supfusion.configuration.services.entity;

import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueCodeEntity;
import lombok.Data;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 业务数据权限
 * 
 * @author zhangbobin
 * @date 2015年10月10日
 */
@Data
@javax.persistence.Entity
@Table(name = SpecialPermission.TABLE_NAME)
public class SpecialPermission extends AbstractAuditUniqueCodeEntity implements Serializable {

	private static final long serialVersionUID = -5993115301868865462L;

	public static final String TABLE_NAME = "ec_special_permission";

	// 关联的模型编码
	private String modelCode;

	private String moduleCode;

	private String entityCode;

	// 等级
	@Column(name = "GRADE")
	private Integer rank;

	// 关系
	private String relation;

	// 类型
	private String type;

	private String targetModelCode;

	private Boolean isTree = false;

	// 关联的参照视图编码
	@ManyToOne
	@JoinColumn(name = "REF_VIEW_CODE")
	@Fetch(FetchMode.SELECT)
	private View refView;
	@ManyToOne
	@JoinColumn(name = "PROPERTY_CODE")
	@Fetch(FetchMode.SELECT)
	private Property property;

	// 顺序
	private Integer orderNo;

	// 关联值,用于显示
	@Transient
	private String propertyName;
	@Transient
	private String associateName;
	@Transient
	private String associateType;
	@Transient
	private String associateCode;
	@Transient
	private String refViewUrl;
	@Transient
	private String targetModelName;
	@Transient
	private List<View> relateRefViews = new ArrayList<View>();

	@Column(name = "MODEL_CODE")
	public String getModelCode() {
		return modelCode;
	}

	public void setModelCode(String modelCode) {
		this.modelCode = modelCode;
	}

	@Column(name = "RELATION")
	public String getRelation() {
		return relation;
	}

	public void setRelation(String relation) {
		this.relation = relation;
	}

	@Column(name = "TYPE")
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Override
	protected String _getEntityName() {
		return SpecialPermission.class.getName();
	}

	@Column(name = "GRADE")
	public Integer getRank() {
		return rank;
	}

	public void setRank(Integer rank) {
		this.rank = rank;
	}

	@Column(name = "TARGET_MODEL_CODE")
	public String getTargetModelCode() {
		return targetModelCode;
	}

	public void setTargetModelCode(String targetModelCode) {
		this.targetModelCode = targetModelCode;
	}

	@Column(name = "IS_TREE", columnDefinition = "INTEGER", length = 1)
	public Boolean getIsTree() {
		return isTree;
	}

	public void setIsTree(Boolean isTree) {
		this.isTree = isTree;
	}

	@Column(name = "ORDER_NO")
	public Integer getOrderNo() {
		return orderNo;
	}

	public void setOrderNo(Integer orderNo) {
		this.orderNo = orderNo;
	}

}
