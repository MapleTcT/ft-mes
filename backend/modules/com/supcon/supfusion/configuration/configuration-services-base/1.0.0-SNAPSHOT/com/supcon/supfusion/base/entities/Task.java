package com.supcon.supfusion.base.entities;

import com.supcon.supfusion.base.annotation.BAPInternational;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueIdEntity;
import dk.brics.automaton.Transition;
import lombok.Data;
import org.hibernate.annotations.Columns;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 活动信息
 * 
 * @author shichenwei
 * 
 */
@Data
@Entity
@Table(name = Task.TABLE_NAME)
public class Task extends AbstractAuditUniqueIdEntity implements Serializable {
	private static final long serialVersionUID = -5729272254253825254L;
	public static final String TABLE_NAME = "wf_task";
	@BAPInternational(replace = true)
	@Type(type="com.supcon.supfusion.base.hibernate.InternationalType")
	@Columns(columns = {
			@Column(name="name"),
			@Column(name="name_zh_cn")
	})
	private String name;
	private String code;
	private Long deploymentId;
	private int type;//1开始2结束3作废4人工5通知6自由7会签8选择9分发10聚合11子流程
	private String viewCode;
	private String script;
	private String openMode;//_blank _self
	private String candidate;//候选人
	private Boolean batchProcess;//是否可批量处理
	private Boolean forbiddenComment;//禁填意见
	private String reminderType;//提醒类型：email, jabber:聊天工具 sms: 短信 ;app:移动应用
	private Integer loopCountersign;//是否循环会签 1表示本公司，2表示夸公司，3表示本部门,4表示本部门及下级，5自定义
	private String externalComponent;//外部组件
	private String expression;
	private Integer joinCount;//聚合数目
	private String subProcessKey;//子流程的编码
	private Integer routeSequence;
	private Boolean crossCompany;
	private java.math.BigDecimal requiredTime;//规定完成时间
	private Boolean overdueReminders;//超期提醒
	private String customParam;//自定义参数
	
	private String processKey; // 流程key
	private int processVersion;//版本 
	private Boolean recallAble;//是否撤回
	private Boolean mobileApprove;//是否移动端
	@Column(name="SHOW_IN_SIMPLE_DEALINFO")
	private Boolean showInSimpleDealInfo = true; //在简易版处理意见显示，默认勾选
	private Boolean isAllowProxy = true;//是否允许委托，默认是
	
	private Integer dealSet=0;//是否可填写处理意见 ；0表示可空，1必填，2禁填
	private Boolean webSignetFlag= false;//是否启用签名
	private Boolean ignorePermission=false;//是否不需要分配权限

	//出去的迁移线
	@Transient
	private List<Transition> outgoingTransitions = new ArrayList<Transition>();
	//指向本活动的迁移线
	@Transient
	private List<Transition> incomingTransitions = new ArrayList<Transition>();

	@Override
	protected String _getEntityName() {
		return Task.class.getName();
	}
	
}
