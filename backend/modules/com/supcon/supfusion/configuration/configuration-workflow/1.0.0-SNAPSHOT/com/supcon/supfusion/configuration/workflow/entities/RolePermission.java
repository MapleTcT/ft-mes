package com.supcon.supfusion.configuration.workflow.entities;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlTransient;

import com.supcon.supfusion.base.entities.MenuOperate;
import com.supcon.supfusion.base.entities.Role;
import com.supcon.supfusion.base.entities.Staff;
import com.supcon.supfusion.base.entities.SystemCode;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueIdEntity;
import lombok.Data;
import org.hibernate.annotations.Index;


/**
 * 角色权限表
 *
 * @author rockey
 *
 */
@Entity
@Data
@Table(name = RolePermission.TABLE_NAME)
public class RolePermission extends AbstractAuditUniqueIdEntity implements Serializable {

	private static final long serialVersionUID = -2321013745505416221L;

	public static final String TABLE_NAME = "base_rolepermission";
	@XmlTransient
	@ManyToOne(targetEntity=Role.class)
	@JoinColumn(name="Role_ID")
	@Index(name = "INDEX_RM_ROLE_ID")
//	@XmlJavaTypeAdapter(RoleAdapter.class)
	private Role role;
	@ManyToOne
	@JoinColumn(name="MENUOPERATE_ID")
	@Index(name = "INDEX_RM_MENUOPERATE_ID")
	private MenuOperate menuOperate;
	private String urlPattern;
	private Integer groupFlag;// 组限制：0 1 2
	private Integer positionFlag;// 岗位限制：0 1
	@Column(name = "ASSIGN_POS_FLAG")
	private Integer assignPosFlag;// 指定岗位：0 1
	@Column(name = "ASSIGN_STAFF_FLAG")
	private Integer assignStaffFlag;// 指定人员：0 1
	//其他限制
	@Column(name = "ASSIGN_OTHERRESTRICT_FLAG")
	private Integer assignOtherRestrictFlag;// 指定其他限制：0 1
	//特殊限制
	@Column(name = "ASSIGN_SPECIALPERMISSION_FLAG")
	private Integer assignSpecialPermissionFlag;// 指定特殊限制：0 1
	@Column(name = "NO_RESTRICT_FLAG")
	private Integer noRestrictFlag;// 无限制：0 1
	private Integer dealerPermissionFlag;//处理人权限:0 1
	@Transient
	private Integer typeFlag = 1;// 角色权限与数据权限区分标记：0:数据权限 1：角色权限
	private Date dealTime;// 处理时间
	@ManyToOne(targetEntity= Staff.class)
	@JoinColumn(name="Staff_ID")
	private Staff dealStaff;// 处理人
	@Transient
	@OneToMany(fetch = FetchType.LAZY, mappedBy = "rolePermission", cascade = CascadeType.REFRESH)
	private List<RolePPosition> rolePPositions;
	@Transient
	@OneToMany(fetch = FetchType.LAZY, mappedBy = "rolePermission", cascade = CascadeType.REFRESH)
	private List<RolePStaff> rolePStaffs;
	@Transient
	@OneToMany(fetch = FetchType.LAZY, mappedBy = "rolePermission", cascade = CascadeType.REFRESH)
	private List<RolePOtherRestrict> rolePOtherRestricts;
	@Transient
	@OneToMany(fetch = FetchType.LAZY, mappedBy = "rolePermission", cascade = CascadeType.REFRESH)
	private List<RolePSpecialPermission>  rolePSpecialPermissions;


//	public Role getRole() {
//		return role;
//	}
//
//	public void setRole(Role role) {
//		this.role = role;
//	}
//
//	public void setRolePStaffs(List<RolePStaff> rolePStaffs) {
//		this.rolePStaffs = rolePStaffs;
//	}
//
//
//	public List<RolePStaff> getRolePStaffs() {
//		return rolePStaffs;
//	}
//
//
//	public List<RolePPosition> getRolePPositions() {
//		return rolePPositions;
//	}
//
//	public void setRolePPositions(List<RolePPosition> rolePPositions) {
//		this.rolePPositions = rolePPositions;
//	}
//
//
//	public MenuOperate getMenuOperate() {
//		return menuOperate;
//	}
//
//	public void setMenuOperate(MenuOperate menuOperate) {
//		this.menuOperate = menuOperate;
//	}
//
//	public String getUrlPattern() {
//		return urlPattern;
//	}
//
//	public void setUrlPattern(String urlPattern) {
//		this.urlPattern = urlPattern;
//	}
//
//	public Integer getGroupFlag() {
//		return groupFlag;
//	}
//
//	public void setGroupFlag(Integer groupFlag) {
//		this.groupFlag = groupFlag;
//	}
//
//	public Integer getPositionFlag() {
//		return positionFlag;
//	}
//
//	public void setPositionFlag(Integer positionFlag) {
//		this.positionFlag = positionFlag;
//	}
//
//	public Date getDealTime() {
//		return dealTime;
//	}
//
//	public void setDealTime(Date dealTime) {
//		this.dealTime = dealTime;
//	}
//
//
//	public Integer getAssignPosFlag() {
//		return assignPosFlag;
//	}
//
//	public void setAssignPosFlag(Integer assignPosFlag) {
//		this.assignPosFlag = assignPosFlag;
//	}
//
//
//	public Integer getAssignStaffFlag() {
//		return assignStaffFlag;
//	}
//
//	public void setAssignStaffFlag(Integer assignStaffFlag) {
//		this.assignStaffFlag = assignStaffFlag;
//	}
//
//
//
//	public Integer getNoRestrictFlag() {
//		return noRestrictFlag;
//	}
//
//	public void setNoRestrictFlag(Integer noRestrictFlag) {
//		this.noRestrictFlag = noRestrictFlag;
//	}
//
//
//	public Staff getDealStaff() {
//		return dealStaff;
//	}
//
//	public void setDealStaff(Staff dealStaff) {
//		this.dealStaff = dealStaff;
//	}
//
//
//	public Integer getTypeFlag() {
//		return typeFlag;
//	}
//
//	public void setTypeFlag(Integer typeFlag) {
//		this.typeFlag = typeFlag;
//	}
//
//	public Integer getDealerPermissionFlag() {
//		return dealerPermissionFlag;
//	}
//
//	public void setDealerPermissionFlag(Integer dealerPermissionFlag) {
//		this.dealerPermissionFlag = dealerPermissionFlag;
//	}


	@Override
	protected String _getEntityName() {
		return RolePermission.class.getName();
	}

	/**
	 * S2字段开始
	 */
	private Long stMenuid;

	private Integer stState = 1;

	private Integer stOperatestate = 1;

	private String stMemo;

	private Integer stPowerflag;

	private String stPowercode;

	private String stModulecode;

	@ManyToOne(fetch=FetchType.EAGER, targetEntity = SystemCode.class)
	@JoinColumn(name="ST_FLAG", nullable=true)
	private SystemCode stFlag;

	private Long stOperateid;

	private Integer stPurviewstate;

	private Integer stPositionflag;

//	public Long getStMenuid() {
//		return stMenuid;
//	}
//
//	public void setStMenuid(Long stMenuid) {
//		this.stMenuid = stMenuid;
//	}
//
//	public Integer getStState() {
//		return stState;
//	}
//
//	public void setStState(Integer stState) {
//		this.stState = stState;
//	}
//
//	public Integer getStOperatestate() {
//		return stOperatestate;
//	}
//
//	public void setStOperatestate(Integer stOperatestate) {
//		this.stOperatestate = stOperatestate;
//	}
//
//	public String getStMemo() {
//		return stMemo;
//	}
//
//	public void setStMemo(String stMemo) {
//		this.stMemo = stMemo;
//	}
//
//	public Integer getStPowerflag() {
//		return stPowerflag;
//	}
//
//	public void setStPowerflag(Integer stPowerflag) {
//		this.stPowerflag = stPowerflag;
//	}
//
//	public String getStPowercode() {
//		return stPowercode;
//	}
//
//	public void setStPowercode(String stPowercode) {
//		this.stPowercode = stPowercode;
//	}
//
//	public String getStModulecode() {
//		return stModulecode;
//	}
//
//	public void setStModulecode(String stModulecode) {
//		this.stModulecode = stModulecode;
//	}
//
//
//	public SystemCode getStFlag() {
//		return stFlag;
//	}
//
//	public void setStFlag(SystemCode stFlag) {
//		this.stFlag = stFlag;
//	}
//
//	public Long getStOperateid() {
//		return stOperateid;
//	}
//
//	public void setStOperateid(Long stOperateid) {
//		this.stOperateid = stOperateid;
//	}
//
//	public Integer getStPurviewstate() {
//		return stPurviewstate;
//	}
//
//	public void setStPurviewstate(Integer stPurviewstate) {
//		this.stPurviewstate = stPurviewstate;
//	}
//
//	public Integer getStPositionflag() {
//		return stPositionflag;
//	}
//
//	public void setStPositionflag(Integer stPositionflag) {
//		this.stPositionflag = stPositionflag;
//	}
//
//
//	public Integer getAssignOtherRestrictFlag() {
//		return assignOtherRestrictFlag;
//	}
//
//	public void setAssignOtherRestrictFlag(Integer assignOtherRestrictFlag) {
//		this.assignOtherRestrictFlag = assignOtherRestrictFlag;
//	}
//
//
//
//	public Integer getAssignSpecialPermissionFlag() {
//		return assignSpecialPermissionFlag;
//	}
//
//	public void setAssignSpecialPermissionFlag(Integer assignSpecialPermissionFlag) {
//		this.assignSpecialPermissionFlag = assignSpecialPermissionFlag;
//	}
//
//
//
//	public List<RolePOtherRestrict> getRolePOtherRestricts() {
//		return rolePOtherRestricts;
//	}
//
//	public void setRolePOtherRestricts(List<RolePOtherRestrict> rolePOtherRestricts) {
//		this.rolePOtherRestricts = rolePOtherRestricts;
//	}
//
//
//	public List<RolePSpecialPermission> getRolePSpecialPermissions() {
//		return rolePSpecialPermissions;
//	}
//
//	public void setRolePSpecialPermissions(List<RolePSpecialPermission> rolePSpecialPermissions) {
//		this.rolePSpecialPermissions = rolePSpecialPermissions;
//	}



	/**
	 * S2字段结束
	 */
}
