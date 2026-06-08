package com.supcon.supfusion.configuration.workflow.entities;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.supcon.supfusion.base.entities.Position;
import com.supcon.supfusion.base.entities.UserPermission;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueIdEntity;
import lombok.Data;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Index;

@Entity
@Data
@Table(name = UserPPosition.TABLE_NAME)
public class UserPPosition extends AbstractAuditUniqueIdEntity implements Serializable {

	private static final long serialVersionUID = -9152983236282373849L;
	public static final String TABLE_NAME = "base_userpposition";
	@XmlTransient
	private UserPermission userPermission;
	private Position position;
	private boolean includeLower;//是否包含下级

	@ManyToOne(targetEntity= UserPermission.class)
	@Index(name="index_UserPPosition_UP_ID")
	@JoinColumn(name="USERPERMISSION_ID")
//	@XmlJavaTypeAdapter(UserPermissionAdapter.class)
	public UserPermission getUserPermission() {
		return userPermission;
	}

	public void setUserPermission(UserPermission userPermission) {
		this.userPermission = userPermission;
	}

	@ManyToOne(targetEntity=Position.class)
	@JoinColumn(name="POSITION_ID")
    @Fetch(FetchMode.SELECT)
	public Position getPosition() {
		return position;
	}

	public void setPosition(Position position) {
		this.position = position;
	}
	@Column(length=1,columnDefinition="INTEGER")
	public boolean isIncludeLower() {
		return includeLower;
	}

	public void setIncludeLower(boolean includeLower) {
		this.includeLower = includeLower;
	}


	@Override
	protected String _getEntityName() {
		return UserPPosition.class.getName();
	}
}
