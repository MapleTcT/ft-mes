package com.supcon.supfusion.i18n.bo;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UploadResourceBO implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String moduleId;
	private String languageCode;
	private String versionCode;
	private String filePath;

	public String toString() {
		return "moduleId=" + moduleId + ", languageCode=" + languageCode + ", versionCode=" + versionCode + ", filePath=" + filePath;
	}

}
