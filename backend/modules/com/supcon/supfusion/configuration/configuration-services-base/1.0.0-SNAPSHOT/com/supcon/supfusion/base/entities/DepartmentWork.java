package com.supcon.supfusion.base.entities;

import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractIdEntity;
import lombok.Data;
import org.hibernate.annotations.*;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.*;
import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;

/**
 * 
 * @author 曹伟彪
 * 
 */
@Data
@Entity
@Immutable
@Table(name = "base_departmentwork")
public class DepartmentWork extends AbstractIdEntity implements Serializable {

	private static final long serialVersionUID = 359214046234761913L;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name="STAFF_ID")
	@Fetch(FetchMode.SELECT)
	@XmlTransient
	private Staff staff;// 人员
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name="DEPARTMENT_ID")
	@Fetch(FetchMode.SELECT)
	@XmlTransient
	private Department department;// 部门
	@XmlTransient
	@Transient
	private Position position;// 岗位
	private Boolean valid = true;
	
	@Override
	protected String _getEntityName() {
		return DepartmentWork.class.getName();
	}
}
