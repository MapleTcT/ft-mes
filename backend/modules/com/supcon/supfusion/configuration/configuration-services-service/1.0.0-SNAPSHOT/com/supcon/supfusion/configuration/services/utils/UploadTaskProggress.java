package com.supcon.supfusion.configuration.services.utils;

import lombok.Data;

@Data
public class UploadTaskProggress {
	
	public static final double UPLAODTASK_UNZIP = 0.1;
	public static final double UPLAODTASK_IMPORTMODULEXML = 0.5;
	public static final double UPLAODTASK_IMPORTTEMPLATE = 0.1;
	public static final double UPLAODTASK_IMPORTSYSTEMCODE = 0.1;
	public static final double UPLAODTASK_IMPORTPORTLET = 0.1;
	public static final double UPLAODTASK_IMPORTOTHER = 0.1;
	
	private Double lastProggress; // 上一进度
	
	private String proggress; // 当前进度
	
	private Double nextProggress; // 下一任务进度
	
	private long taskTime; // 预计总时间
	
	private long remanentTtime; // 剩余时间
	
	private Double taskSize; // 总进度

	public void uploadProggress(double type) {
		nextProggress += type;
	}

}
