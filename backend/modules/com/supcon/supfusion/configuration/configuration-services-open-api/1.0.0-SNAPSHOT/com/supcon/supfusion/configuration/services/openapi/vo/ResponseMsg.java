package com.supcon.supfusion.configuration.services.openapi.vo;

import lombok.Data;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Data
public class ResponseMsg {

	private boolean success;
	private Map<String, List<String>> items = Collections.emptyMap();
	List<String> actionErrors = Collections.emptyList();
	private String exceptionMsg;
	private Object data;

	public ResponseMsg() {
	}

	public ResponseMsg(boolean success) {
		this.success = success;
	}
}
