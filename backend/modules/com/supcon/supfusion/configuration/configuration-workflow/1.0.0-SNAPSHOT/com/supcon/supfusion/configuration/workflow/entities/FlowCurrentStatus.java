package com.supcon.supfusion.configuration.workflow.entities;

import com.supcon.supfusion.base.entities.Staff;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractIdEntity;
import lombok.Data;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.util.Date;

/**
 * 工作流状态表.
 * 
 * @author tanzhengyang
 * 
 */
@Data
@Entity
@Table(name = FlowCurrentStatus.TABLE_NAME)
public class FlowCurrentStatus extends AbstractIdEntity implements Cloneable {

	private static final long serialVersionUID = 3419572566425539054L;
	public static final String TABLE_NAME = "wf_flow_current_status";
	private String lastActivityName;// 上一活动
	private String currActivityName;// 当前活动
	private String inTransition;// 当前活动入的迁移线
	@ManyToOne(targetEntity = Staff.class)
	@JoinColumn(name = "DEALER")
	@Fetch(FetchMode.SELECT)
	private Staff dealer;// 处理人
	private String sourceStaff; // 委托代办最初来源
	private String activityType; // 活动类型
	private String autoCreateDealInfos; // 活动类型
	private Date createTime;// 创建时间
	private Long tableInfoId;// tableInfoId
	private Long deploymentId;//

	@Override
	protected String _getEntityName() {
		return FlowCurrentStatus.class.getName();
	}

}