package com.supcon.supfusion.signature.dao.entity;


import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.cloud.common.util.DateUtil;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 电子签名日志
 */


@Data
@TableName(value = "bap_signature_logs", autoResultMap=true)
public class SignatureLog implements Serializable {

	private static final long serialVersionUID = 8718334521164274222L;
	@TableId
	private String uuid;
	private String businessKey;

	/*
	 *   1表示成功  0表示失败
	 */
	private Boolean status=true;

	private String modelCode;
	private String moduleCode;

	private Long firstUserId;
	
	private String firstUserName;
	private String firstStaffName;
	private String buttonName;

	private String entityName;
	private String moduleName;
	private String modelName;
	private Long firstStaffId;

	private String signatureType;
	private String entityCode;

	private String firstReason;
	private Long tableId;
	private String buttonCode;
	private String ipAddress;

	private Date createTime;

	private Long processId;
	private Long taskId;
	private Long  transitionId;
	
	private String processName;
	private String taskName;
	private String transitionName;
	
	private Long secondUserId;
	private String secondUserName;


	private Long secondStaffId;
	private String secondStaffName;
	private String secondReason;
	
	private String firstRemark;
	private String secondRemark;

	private Date firstSignTime;
	private Date secondSignTime;
	private String operateLogUuid;
	private Long cid;


	public String getFirstSignTimeStr() {
		if (firstSignTime == null){
			return null;
		}
		String firstSignTimeStr = DateUtil.format(firstSignTime, DateUtil.PATTERN_DATETIME);
		return firstSignTimeStr;
	}

	public String getSecondSignTimeStr() {
		if (secondSignTime == null){
			return null;
		}
		String secondSignTimeStr = DateUtil.format(secondSignTime, DateUtil.PATTERN_DATETIME);
		return secondSignTimeStr;
	}
}
