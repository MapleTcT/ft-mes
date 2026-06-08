package com.supcon.supfusion.signature.dao.enums;

public enum JobStatus {
	ADDNOTSTART, // 初次添加,未选择start。未加入Scheduler
	ADDANDSTART, // 初次添加，选择Start
	SCHEDULED, // 被部署
	WAITNEXTTRIGGER, // 等待下次触发
	JOBPAUSED, // Job被暂停
	JOBOVER, // Job完成所有触发，结束停止状态
	UNSCHEDULED, // 未部署
	SCHEDULERERROR;// 部署失败
	
	public static void main(String[] args){
		System.out.println(JobStatus.ADDNOTSTART.ordinal());
	}
}
