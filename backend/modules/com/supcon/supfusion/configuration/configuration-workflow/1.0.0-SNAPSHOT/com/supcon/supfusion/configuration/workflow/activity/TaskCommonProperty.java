package com.supcon.supfusion.configuration.workflow.activity;


import com.supcon.supfusion.configuration.workflow.handlers.AssignmentHandler;

import java.io.Serializable;

public class TaskCommonProperty implements Serializable {

	private static final long serialVersionUID = -5072957158471598315L;

	protected String name;
	protected String openUrl;
	protected String description;
	protected AssignmentHandler assignmentHandler;
	protected Boolean bulkDealFlag = false;
	protected Boolean dealFlag = false;
	protected Integer dealSet=0;
	protected String customParam;
	protected Boolean webSignetFalg= false;
	protected Boolean recallAble= false;
	protected Boolean mobileApprove = false; // 是否支持移动端审批

	public Boolean getMobileApprove() {
		return mobileApprove;
	}

	public void setMobileApprove(Boolean mobileApprove) {
		this.mobileApprove = mobileApprove;
	}

	public Boolean getWebSignetFalg() {
		return webSignetFalg;
	}

	public void setWebSignetFalg(Boolean webSignetFalg) {
		this.webSignetFalg = webSignetFalg;
	}

	public Integer getDealSet() {
		return dealSet;
	}

	public void setDealSet(Integer dealSet) {
		this.dealSet = dealSet;
	}

	public String getCustomParam() {
		return customParam;
	}

	public void setCustomParam(String customParam) {
		this.customParam = customParam;
	}
	
	public Boolean getDealFlag() {
		return dealFlag;
	}

	public void setDealFlag(Boolean dealFlag) {
		this.dealFlag = dealFlag;
	}

	public Boolean getBulkDealFlag() {
		return bulkDealFlag;
	}

	public void setBulkDealFlag(Boolean bulkDealFlag) {
		this.bulkDealFlag = bulkDealFlag;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public AssignmentHandler getAssignmentHandler() {
		return assignmentHandler;
	}

	public void setAssignmentHandler(AssignmentHandler assignmentHandler) {
		this.assignmentHandler = assignmentHandler;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getOpenUrl() {
		return openUrl;
	}

	public void setOpenUrl(String openUrl) {
		this.openUrl = openUrl;
	}

	public Boolean getRecallAble() {
		return recallAble;
	}

	public void setRecallAble(Boolean recallAble) {
		this.recallAble = recallAble;
	}
}