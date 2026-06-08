package com.supcon.supfusion.base.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractIdEntity;
import lombok.Data;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Immutable;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.util.Date;


/**
 * 
 * @author 曹伟彪
 * 
 */
@Data
@Entity
@Immutable
@Table(name = "base_positionwork")
public class PositionWork extends AbstractIdEntity implements Serializable {

	private static final long serialVersionUID = 1398876549164353275L;

	private boolean valid = true;// 是否有效

	@ManyToOne(fetch = FetchType.EAGER)
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name="STAFF_ID")
	@XmlTransient
	private Staff staff;
	@ManyToOne(fetch = FetchType.EAGER)
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name="POSITION_ID")
	@XmlTransient
	private Position position;

	@Override
	protected String _getEntityName() {
		return PositionWork.class.getName();
	}

}
