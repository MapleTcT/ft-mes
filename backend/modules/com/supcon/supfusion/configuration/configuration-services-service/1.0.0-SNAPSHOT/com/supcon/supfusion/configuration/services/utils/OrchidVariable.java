/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.configuration.services.utils;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 存储BAP全局变量
 * 
 * @author zhuyuyin
 * @version 1.0
 */
public class OrchidVariable {
	private static final ReentrantLock lock = new ReentrantLock();
	private static final Set<String> modelArtifacts = new HashSet<String>();
	private static final Set<String> ORCHID_MODULE_SET = new HashSet<String>();
	static {
		ORCHID_MODULE_SET.add("foundation");
		ORCHID_MODULE_SET.add("ui");
		ORCHID_MODULE_SET.add("container");
		ORCHID_MODULE_SET.add("script");
		ORCHID_MODULE_SET.add("entityconf");
		ORCHID_MODULE_SET.add("workflow");
		modelArtifacts.add("foundation");
	}
	
	public static Set<String> getOrchidModules(){
		return ORCHID_MODULE_SET;
	}
	
	/**
	 * 获取BAP所有模块的Artifact
	 * @return
	 */
	public static Set<String> getModelArtifacts() {
		return modelArtifacts;
	}
	/**
	 * 添加模块Artifact
	 * @param artifacts
	 */
	public static void addModelArtifacts(Set<String> artifacts) {
		try{
			lock.lock();
			modelArtifacts.addAll(artifacts);
		} finally {
			lock.unlock();
		}
	}
	/**
	 * 添加模块Artifact
	 * @param modelArtifact
	 */
	public static void addModelArtifact(String modelArtifact) {
		try{
			lock.lock();
			modelArtifacts.add(modelArtifact);
		} finally {
			lock.unlock();
		}
	}
}
