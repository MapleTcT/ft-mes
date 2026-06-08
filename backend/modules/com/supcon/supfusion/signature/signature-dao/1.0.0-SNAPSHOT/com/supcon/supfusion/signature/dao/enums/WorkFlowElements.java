package com.supcon.supfusion.signature.dao.enums;

public enum WorkFlowElements {
	SUPERVISION("ec.view.wf.supervision"), // 督办人
	;
	private String value;

	private WorkFlowElements(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}
}
