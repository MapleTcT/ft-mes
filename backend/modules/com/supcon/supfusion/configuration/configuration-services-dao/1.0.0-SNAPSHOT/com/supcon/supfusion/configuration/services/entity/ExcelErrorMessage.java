package com.supcon.supfusion.configuration.services.entity;

import lombok.Data;

@Data
public class ExcelErrorMessage {

	private Integer x;
	private Integer y;
	private String message;
	private String sheet;
	private String modelCode;

}
