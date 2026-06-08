package com.supcon.supfusion.configuration.workflow.entities;

import com.supcon.supfusion.configuration.services.utils.ConditionUtil;
import com.supcon.supfusion.configuration.workflow.service.VariablesProvider;

import java.io.Serializable;
import java.util.*;

/**
 * 非持久化,用以流程传值bean.
 * 
 * @author SONGJIAWEI
 * 
 */
public class WorkFlowVar implements Serializable {
	private static final long serialVersionUID = 8265926794742873710L;

	private String outcome;// 所选迁移线
	private String outcomeDes;// 迁移线描述
	private String comment;// 审批意见
	private Map<String, ?> variables;// 变量
	private List<Map<String,?>> outcomeMap;
	private Long modelId;// 模型对象ID
	private Boolean crossCompanyFlag;//是否可以跨公司
	private Long processInitiator;//制单人
	private Long tableInfoId;//单据id
	private Long deploymentId;//流程id
	// private Long entityId;
	private String entityCode;//实体code
	private Long ownerId;//
	private Long ownerPositionId;//所有者岗位id
	private Long initiatorPositionId;//制单人岗位
	private String initiatorGroupIds;//制单人组 采用逗号隔开
	private String tableNo;//表单编号
	private String tableName;//表单名称
	private Boolean groupEnabled;//是否启用组限制
	private String countersignUsers;//普通会签选人，采用逗号隔开
	private String scriptExcuteBeanName;//执行脚本的service
	private Boolean endCountersignFlag;//是否强制结束会签
	private String mainCountersigner;//主会签人，采用逗号隔开
	private IModelStatus status;//单据状态
	private Long currentUserId;//当前用户Id
	private String operateType;//操作类型,save||submit
	private String outcomeType;//迁移线类型
	private Boolean webSignetFlag;//是否使用签名
	private Boolean recallAble;
	private String activityType;//活动类型
	
	public String getOutcomeType() {
		return outcomeType;
	}

	public void setOutcomeType(String outcomeType) {
		this.outcomeType = outcomeType;
	}

	public String getOperateType() {
		return operateType;
	}

	public void setOperateType(String operateType) {
		this.operateType = operateType;
	}

	public String getMainCountersigner() {
		return mainCountersigner;
	}

	public void setMainCountersigner(String mainCountersigner) {
		this.mainCountersigner = mainCountersigner;
	}

	public Long getCurrentUserId() {
		return currentUserId;
	}

	public void setCurrentUserId(Long currentUserId) {
		this.currentUserId = currentUserId;
	}

	public Boolean getEndCountersignFlag() {
		return endCountersignFlag;
	}

	public void setEndCountersignFlag(Boolean endCountersignFlag) {
		this.endCountersignFlag = endCountersignFlag;
	}

	public List<Map<String, ?>> getOutcomeMap() {
		return outcomeMap;
	}

	public void setOutcomeMap(List<Map<String, ?>> outcomeMap) {
		this.outcomeMap = outcomeMap;
	}

	public void setOutcomeMapJson(String outcomeMap) {
		if(null!=outcomeMap&&!"".equals(outcomeMap)){
			this.outcomeMap = (List<Map<String, ?>>) ConditionUtil.generateMapFromJson(outcomeMap);
		}
		
	}

	public String getScriptExcuteBeanName() {
		return scriptExcuteBeanName;
	}

	public void setScriptExcuteBeanName(String scriptExcuteBeanName) {
		this.scriptExcuteBeanName = scriptExcuteBeanName;
	}

	private Set<Long> additionalUsers;//指定人员

	
	public Boolean getCrossCompanyFlag() {
		return crossCompanyFlag;
	}

	public void setCrossCompanyFlag(Boolean crossCompanyFlag) {
		this.crossCompanyFlag = crossCompanyFlag;
	}

	public Boolean getGroupEnabled() {
		return groupEnabled;
	}

	public void setGroupEnabled(Boolean groupEnabled) {
		this.groupEnabled = groupEnabled;
	}

	public Set<Long> getAdditionalUsers() {
		return additionalUsers;
	}

	public void setAdditionalUsersStr(String selectedUsers) {
		if (selectedUsers != null && selectedUsers.length() > 0) {
			this.additionalUsers = new HashSet<Long>();
			StringTokenizer tokenizer = new StringTokenizer(selectedUsers, ",");
			while (tokenizer.hasMoreTokens()) {
				String selectedIdStr = tokenizer.nextToken();
				if (selectedIdStr != null && selectedIdStr.length() > 0) {
					this.additionalUsers.add(Long.valueOf(selectedIdStr));
				}
			}
		}else{
			if(this.additionalUsers!=null){
				this.additionalUsers.clear();
			}
		}
	}

	public void setAdditionalUsers(Set<Long> additionalUsers) {
		this.additionalUsers = additionalUsers;
	}

	public IModelStatus getStatus() {
		return status;
	}

	public void setStatus(IModelStatus status) {
		this.status = status;
	}

	public String getCountersignUsers() {
		return countersignUsers;
	}

	public void setCountersignUsers(String countersignUsers) {
		this.countersignUsers = countersignUsers;
	}

	private VariablesProvider variablesProvider;

	public VariablesProvider getVariablesProvider() {
		return variablesProvider;
	}

	public void setVariablesProvider(VariablesProvider variablesProvider) {
		this.variablesProvider = variablesProvider;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public String getTableNo() {
		return tableNo;
	}

	public void setTableNo(String tableNo) {
		this.tableNo = tableNo;
	}

	public Long getModelId() {
		return modelId;
	}

	public void setModelId(Long modelId) {
		this.modelId = modelId;
	}

	public Long getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(Long ownerId) {
		this.ownerId = ownerId;
	}

	public Long getOwnerPositionId() {
		return ownerPositionId;
	}

	public void setOwnerPositionId(Long ownerPositionId) {
		this.ownerPositionId = ownerPositionId;
	}

	public Long getInitiatorPositionId() {
		return initiatorPositionId;
	}

	public void setInitiatorPositionId(Long initiatorPositionId) {
		this.initiatorPositionId = initiatorPositionId;
	}

	public String getInitiatorGroupIds() {
		return initiatorGroupIds;
	}

	public void setInitiatorGroupIds(String initiatorGroupIds) {
		this.initiatorGroupIds = initiatorGroupIds;
	}

	// public Long getEntityId() {
	// return entityId;
	// }
	//
	// public void setEntityId(Long entityId) {
	// this.entityId = entityId;
	// }

	public Long getDeploymentId() {
		return deploymentId;
	}

	public String getEntityCode() {
		return entityCode;
	}

	public void setEntityCode(String entityCode) {
		this.entityCode = entityCode;
	}

	public void setDeploymentId(Long deploymentId) {
		this.deploymentId = deploymentId;
	}

	public Map<String, ?> getVariables() {
		return variables;
	}

	public void setVariables(Map<String, ?> variables) {
		this.variables = variables;
	}

	public String getOutcome() {
		return outcome;
	}

	public void setOutcome(String outcome) {
		this.outcome = outcome;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public Long getProcessInitiator() {
		return processInitiator;
	}

	public void setProcessInitiator(Long processInitiator) {
		this.processInitiator = processInitiator;
	}

	public Long getTableInfoId() {
		return tableInfoId;
	}

	public void setTableInfoId(Long tableInfoId) {
		this.tableInfoId = tableInfoId;
	}

	public String getOutcomeDes() {
		return outcomeDes;
	}

	public void setOutcomeDes(String outcomeDes) {
		this.outcomeDes = outcomeDes;
	}

	public Boolean getWebSignetFlag() {
		return webSignetFlag;
	}

	public void setWebSignetFlag(Boolean webSignetFlag) {
		this.webSignetFlag = webSignetFlag;
	}

	public Boolean getRecallAble() {
		return recallAble;
	}

	public void setRecallAble(Boolean recallAble) {
		this.recallAble = recallAble;
	}

	public String getActivityType() {
		return activityType;
	}

	public void setActivityType(String activityType) {
		this.activityType = activityType;
	}
	
}
