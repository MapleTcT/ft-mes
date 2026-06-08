/**
 * 
 */
package com.supcon.supfusion.configuration.services.entity;

import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueIdEntity;
import lombok.Data;

import javax.persistence.Transient;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 业务模块启动、发布任务。
 * 
 * 由于BAP的业务模块启动、发布较慢，为了更好的管理这些任务，提供统一的发布启动逻辑进行管理。
 * 
 * @author yaowei
 *
 */
@Data
public class DeploymentTask extends AbstractAuditUniqueIdEntity implements Serializable {
	
	private static final long serialVersionUID = 3537965692171860637L;
	
	public static final short TASK_PACKAGE = 4;
	
	public static final short TASK_DEPLOY = 8;

	public static final short TASK_GENERATE_SOURCE = 16;

	public static final short TASK_GENERATE_FULL = TASK_GENERATE_SOURCE + 1;
	
	/**
	 * 同步EC数据到RUNTIME。
	 */
	public static final short TASK_SYNC_EC_TO_RUNTIME = 32;
	
	public enum TaskStatus {
		WAITTING, // 等待中
		RUNNING, // 执行中
		FINISHED, // 已完成
		FAILED, // 已失败
		CANCELED, // 已取消
	}

	/**
	 * 任务所对应的模块
	 */
	private String moduleCode;
	
	/**
	 * 任务所对应的所有模块名称
	 */
	private String moduleName;
	
	/**
	 * 任务内容
	 */
	private int tasks;
	
	private TaskStatus status;
	
	// 以下的属性由后台服务所负责set
	@Transient
	private Module module;
	@Transient
	private List<Module> modules;
	@Transient
	private String locale = "zh_CN";
	@Transient
	private boolean needUpdateDatabaseTables = false;
	@Transient
	private boolean needUpdateStaticResources = false;
	@Transient
	private boolean autoDeploy = false;
	@Transient
	private Integer level;		//模块依赖层级
	private Map<String,String> taskMap;
	private String deployUser;

	Date createTime;
	
	// default constractor.
	public DeploymentTask() {
	}
	
	public DeploymentTask(String moduleCode, int taskType) {
		super();
		this.moduleCode = moduleCode;
		this.tasks = taskType;
		this.createTime = new Date();
		this.status = TaskStatus.WAITTING;
		this.locale = "zh_CN"; // 默认为简体中文
	}
	
	public DeploymentTask(String moduleCode, int taskType, boolean needUpdateDatabaseTables,
                          boolean needUpdateStaticResources) {
		this(moduleCode, taskType);
		this.needUpdateDatabaseTables = needUpdateDatabaseTables;
		this.needUpdateStaticResources = needUpdateStaticResources;
	}

	// 以下均为 Transient 
	public String getTaskName() {
		return getModuleCode() + "_" + getId();
	}
	
	@Transient
	public boolean isWaitting() {
		return DeploymentTask.TaskStatus.WAITTING == this.status;
	}

	@Transient
	public boolean isRunning() {
		return DeploymentTask.TaskStatus.RUNNING == this.status;
	}

	@Transient
	public boolean isCanceled() {
		return DeploymentTask.TaskStatus.CANCELED == this.status;
	}

	@Transient
	public boolean hasGenerateTask() {
		return (tasks & DeploymentTask.TASK_GENERATE_SOURCE) == DeploymentTask.TASK_GENERATE_SOURCE
				|| hasGenerateFull();
	}

	@Transient
	public boolean hasGenerateFull() {
		return (tasks & DeploymentTask.TASK_GENERATE_FULL) == DeploymentTask.TASK_GENERATE_FULL;
	}

	@Transient
	public boolean hasBuildTask() {
		return (tasks & DeploymentTask.TASK_PACKAGE) == DeploymentTask.TASK_PACKAGE;
	}
	
	@Transient
	public boolean hasSyncRuntime() {
		return (tasks & DeploymentTask.TASK_SYNC_EC_TO_RUNTIME) == DeploymentTask.TASK_SYNC_EC_TO_RUNTIME;
	}

	@Transient
	public boolean hasDeployTask() {
		return (tasks & DeploymentTask.TASK_DEPLOY) == DeploymentTask.TASK_DEPLOY;
	}

	@Override
	protected String _getEntityName() {
		return null;
	}

	public DeploymentTask clone() {
		DeploymentTask clone = new DeploymentTask(moduleCode, tasks);
		if(null != getId()) clone.setId(getId());
		clone.setCreateTime(getCreateTime());
		clone.setStatus(getStatus());
		clone.setLocale(getLocale());
		clone.setNeedUpdateDatabaseTables(needUpdateDatabaseTables);
		clone.setNeedUpdateStaticResources(needUpdateStaticResources);
		clone.setAutoDeploy(autoDeploy);
		clone.setDeployUser(deployUser);
		clone.setLevel(level);
		return clone;
	}
	

	/**
	 * 
	 * @return
	 */
	public String getTaskNames() {
		StringBuilder sb = new StringBuilder();
		if(hasGenerateTask()) sb.append(",generate");
		else if(hasGenerateFull()) sb.append(",generate_full");
		
		if(hasBuildTask()) sb.append(",package");
		if(hasDeployTask()) sb.append(",deploy");
		if(hasSyncRuntime()) sb.append(",sync_runtime");

		if(sb.length() > 0) return sb.substring(1);
		return "";
	}

}
