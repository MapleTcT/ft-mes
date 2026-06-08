/**
 * 
 */
package com.supcon.supfusion.configuration.services.utils;

import lombok.Data;

import java.io.Serializable;

/**
 * @author zhushizhang
 *
 */
@Data
public class UploadLog implements Serializable {
	
	private final long position;

	private final String status;

	private final String content;

	private transient UploadTaskProggress taskProggress;
	
	public UploadLog(long length, String content, UploadTaskProggress taskProggress) {
		super();
		this.position = length;
		this.content = content;
		this.status = "RUNNING";
		this.taskProggress = taskProggress;
	}
	
	public UploadLog(long position, String content, String status, UploadTaskProggress taskProggress) {
		super();
		this.position = position;
		this.content = content;
		this.status = status;
		this.taskProggress = taskProggress;
	}

}
