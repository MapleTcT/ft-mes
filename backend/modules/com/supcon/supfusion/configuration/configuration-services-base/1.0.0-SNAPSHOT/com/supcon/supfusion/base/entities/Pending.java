package com.supcon.supfusion.base.entities;

import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractIdEntity;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.Date;


@Data
@Entity
@Table(name = Pending.TABLE_NAME)
public class Pending extends AbstractIdEntity implements Serializable {

	private static final long serialVersionUID = 3419572566425539054L;
	public static final String TABLE_NAME = "wf_pending";

	public static final int ACTIVE = 88;
	public static final int DONE = 99;
	public static final int SUSPEND = 77;
	public static final String NORMAL_PENDING = "normal";
	public static final String EXPECTED_PENDING = "expectedConsignor";
	public static final String PRO_ID = "id";
	public static final String PRO_TABLEINFO_ID = "tableInfoId";
	public static final String PRO_PROCESS_KEY = "processKey";
	public static final String PRO_PROCESS_VERSION = "processVersion";
	public static final String PRO_TASK_DESCRIPTION = "taskDescription";
	public static final String PRO_OPEN_URL = "openUrl";
	public static final String PRO_ENTITY_ID = "entityId";
	public static final String PRO_USER_ID = "userId";
	public static final String PRO_ACTIVITY_NAME = "activityName";
	public static final String PRO_ACTIVITY_TYPE = "activityType";
	public static final String PRO_DEPLOYMENT_ID = "deploymentId";
	public static final String PRO_PROCESS_ID = "processId";
	public static final String PRO_PROCESS_NAME = "processName";
	public static final String PRO_PROCESS_DESCRIPTION = "processDescription";

	public static final String COL_TABLEINFO_ID = "TABLE_INFO_ID";
	public static final String COL_ID = "ID";
	public static final String COL_STATUS = "STATUS";
	public static final String COL_OPEN_URL = "OPEN_URL";
	public static final String COL_TASK_DESCRIPTION = "TASK_DESCRIPTION";
	public static final String COL_ENTITY_ID = "ENTITY_ID";
	public static final String COL_USER_ID = "USER_ID";
	public static final String COL_PROCESS_KEY = "PROCESS_KEY";
	public static final String COL_PROCESS_VERSION = "PROCESS_VERSION";
	public static final String COL_ACTIVITY_NAME = "ACTIVITY_NAME";
	public static final String COL_ACTIVITY_TYPE = "ACTIVITY_TYPE";
	public static final String COL_DEPLOYMENT_ID = "DEPLOYMENT_ID";
	public static final String COL_PROCESS_ID = "PROCESS_ID";
	public static final String COL_PROCESS_NAME = "PROCESS_NAME";
	public static final String COL_PROCESS_DESCRIPTION = "PROCESS_DESCRIPTION";
	public static final String COL_MODEL_ID = "MODEL_ID";
	public static final String COL_EXECUTION_ID = "EXECUTION_ID";
	private String taskDescription;// 活动描述,用于指示迁移线
	private String activityType;// 对应的活动类型,一般为Activity.TAG

	private String activityName;// 活动名称，这个在一个流程中是惟一的

	private String executionId;// 引擎里的一个东西

	// private String username;

	private Long userId;// 2.0起采用userId记录待办接收者.

	private Integer status;// 状态:ACTIVE,DONE,SUSPEND
	private Date createTime;// 创建时间

	private String openUrl;// 打开对应视图地址

	private String instanceId;// 流程实例ID,索引字段

	private String processKey;// 流程key，索引字段,冗余避免多次查询
	private Integer processVersion;// 流程版本号,冗余避免多次查询
	private String processName;// 流程名称,冗余避免多次查询
	private String processId;// 流程id,冗余避免多次查询
	private String processDescription;// 流程描述,冗余避免多次查询

	private Long modelId; // model id
	@Column(name = COL_TABLEINFO_ID)
	private Long tableInfoId;// TABLEINFO
	// private Long entityId;// 所属实体
	private String tableNo;// 冗余单据编号
	private String entityCode;

	private Long cid;
	@Transient
	private Boolean bulkDealFlag;
	private String systemCalendarId;
	private Boolean overdue;// 是否超期
	private Integer overdueTime;// 超期时间
	private Date statisticsDate;// 统计时间

	// private Deployment deployment;
	private Long deploymentId;
	private Integer taskType = 0;// 任务类型；0：普通待办，2：普通/预期委托待办
	private String proxySource;// 委托人
	private String sourceStaff; // 委托代办最初来源
	private String description;
	@Column(name = "LOOPS")
	private Integer loop;// 0不是循环会签1循环会签2跨公司
	private Boolean mainLoop = false;// 主办人
	private Boolean mobileApprove; // 待办是否支持移动端审批


	@Override
	protected String _getEntityName() {
		return Pending.class.getName();
	}

}