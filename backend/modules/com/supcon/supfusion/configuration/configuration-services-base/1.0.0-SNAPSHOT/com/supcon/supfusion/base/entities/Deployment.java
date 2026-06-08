package com.supcon.supfusion.base.entities;

import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueIdEntity;
import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 流程。记录流程信息。
 * 
 * @author songjiawei
 * 
 */
@Entity
@Data
@Table(name = Deployment.TABLE_NAME)
public class Deployment extends AbstractAuditUniqueIdEntity implements Serializable {
	private static final long serialVersionUID = -5425438079328401144L;
	public static final String TABLE_NAME = "wf_deployment";
	private String name;// 流程名称英文
	private String processKey; // 流程key
	private int processVersion;// 版本
	private String description;// 描述
	private String processName;// 引擎中对应名称
	private String deploymentId;// 对应JBPM中DEPLOYMENT的Id
	private Boolean isSuspended;// 是否暂停
	private Boolean isCurrentVersion = false;// 是否当前版本
	private String processDefinitionId;// 对应JBPM中流程定义的ID
	private Long menuInfoId;
	private String menuCode;
	// private Long entityId;//所述实体ID
	private String entityCode;
	private Boolean publishFlag;// 是否发布
	private String operatePowers;
	private String processXml;// 流程XML
	private String entryUrl;// 首次打开地址
	private String tempProcessXml;// 临时保存的流程xml
	private Boolean flowEditFlag;// 是否可以修改发布
	@Transient
	private String keyDescs;// 流程中国际化的key和desc值
	private BigDecimal requiredTime;// 规定完成时间
	private Boolean mobilequery = false;
	private Boolean mobileinitiate = false;
	private Boolean mobileapprove = true;
	private Boolean allowInvalid = false; // 允许管理员作废
	private Boolean graduallyReject = false; // 逐级驳回
	private Boolean recallAble = false;
	private Long recallRemainTime;
	private Date publishTime; // 发布时间
	@Transient
	private StaffVO createStaff;
	@Transient
	private StaffVO modifyStaff;
	private Integer version;
	private Long cid;
	private Boolean crossCompanyFlag = false;
	/**
	 * @author chaibohai
	 * BAP-XA-DBZY
	 * 加上电子签名字段
	 */
	private Boolean signatureEnable;


	private String mainViewViewCode; // 工作流查看视图

	@Override
	protected String _getEntityName() {
		return Deployment.class.getName();
	}

}
