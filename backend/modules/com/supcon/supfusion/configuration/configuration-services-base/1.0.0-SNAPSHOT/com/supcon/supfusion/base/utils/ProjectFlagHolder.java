
/**
 * Copyright (C) 2015 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.base.utils;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 
 * 
 * @author zhuwei2
 * @version $Id$
 */
@Data
@Slf4j
@Component
public class ProjectFlagHolder {
	private ThreadLocal<Boolean> projFlag=new ThreadLocal<Boolean>();
	private static ProjectFlagHolder projectFlagHolder;
	private ProjectFlagHolder(){
	};
	public static ProjectFlagHolder getInstance(){
		if(projectFlagHolder==null){
			projectFlagHolder=new ProjectFlagHolder();
		}
		return projectFlagHolder;
	}
	public ThreadLocal<Boolean> getProjFlag() {
		return projFlag;
	}
}
