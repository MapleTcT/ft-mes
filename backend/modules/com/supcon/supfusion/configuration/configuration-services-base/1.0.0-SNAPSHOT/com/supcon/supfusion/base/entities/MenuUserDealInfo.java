package com.supcon.supfusion.base.entities;


import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractIdEntity;
import lombok.Data;
import org.hibernate.annotations.Immutable;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Data
@Entity
@Immutable
@Table(name=MenuUserDealInfo.TABLE_NAME)
public class MenuUserDealInfo extends AbstractIdEntity implements Serializable {

	private static final long serialVersionUID = 5914991160306858248L;
	static final String TABLE_NAME="base_menu_user_deal_info";
	@ManyToOne(fetch= FetchType.EAGER,targetEntity=MenuOperate.class)
	@JoinColumn(name="MENU_OPERATE_ID")
	private MenuOperate menuOperate;//权限所对应的操作
	private String memo;//修改权限的备注信息
	@ManyToOne(fetch= FetchType.EAGER,targetEntity=Staff.class)
	@JoinColumn(name="Dealer_ID")
	private Staff dealer;//处理人
	private Integer dealType;//处理类型(0 删除 1修改 2添加)
	private Date dealTime;//处理时间
	private Integer dealReason;//权限更改的理由(1：角色权限2：用户权限 3：人员调动 4：人员岗位调动  5:流程授权)
	@ManyToOne(fetch= FetchType.EAGER,targetEntity=User.class)
	@JoinColumn(name="User_ID")
	private User targetUser;//被处理人
	@ManyToOne(fetch= FetchType.EAGER,targetEntity=Role.class)
	@JoinColumn(name="Role_ID")
	private Role targetRole;//被处理角色
	@ManyToOne(targetEntity=Position.class)
	@JoinColumn(name="Position_ID")
	private Position targetPosition;//被处理岗位
	@ManyToOne(targetEntity=Department.class)
	@JoinColumn(name="Department_ID")
	private Department targetDepartment;//被处理部门
	@ManyToOne(fetch= FetchType.EAGER,targetEntity=MenuInfo.class)
	@JoinColumn(name="MENU_INFO_ID")
	private MenuInfo menuInfo;//权限对应的菜单
	private Integer type;//0表示角色权限的处理日志；1表示用户权限的处理日志；2表示对岗位进行的处理日志；3表示对部门进行的处理日志
	private String dealInfo;//权限的变更的日志信息
	@ManyToOne(targetEntity=Company.class)
	@JoinColumn(name="CID")
	private Company cid;  //公司

	@Override
	protected String _getEntityName() {
		return MenuUserDealInfo.class.getName();
	}
	
}
