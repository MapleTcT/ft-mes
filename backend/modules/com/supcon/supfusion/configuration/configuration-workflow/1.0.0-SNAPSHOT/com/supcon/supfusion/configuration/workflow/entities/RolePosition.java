package com.supcon.supfusion.configuration.workflow.entities;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.supcon.supfusion.base.entities.Position;
import com.supcon.supfusion.base.entities.Role;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueIdEntity;
import lombok.Data;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Index;

/**
 * @author 方佳涵
 */
@Entity
@Data
@Table(name = "BASE_ROLEPOSITION")
public class RolePosition extends AbstractAuditUniqueIdEntity implements Serializable {
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 44908648024140858L;

	private boolean valid = true;// 是否有效

	private Position position;
	private Role role;

	
	
	@Column(length=1,columnDefinition="INTEGER")
	public boolean isValid() {
		return valid;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}
	
	@ManyToOne
	@JoinColumn(name = "POSITION_ID")
	@Index(name="INDEX_ROLEPOSITION_POSITION_ID")
    @Fetch(FetchMode.SELECT)
	public Position getPosition() {
		return position;
	}

	public void setPosition(Position position) {
		this.position = position;
	}
	
	@ManyToOne
	@JoinColumn(name = "ROLE_ID")
	@Index(name="INDEX_ROLEPOSITION_ROLE_ID")
    @Fetch(FetchMode.SELECT)
	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}
	

	@Override
	protected String _getEntityName() {
		return RolePosition.class.getName();
	}


}
