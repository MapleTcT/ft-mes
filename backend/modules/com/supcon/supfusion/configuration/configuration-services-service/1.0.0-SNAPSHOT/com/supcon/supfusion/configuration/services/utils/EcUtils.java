/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.configuration.services.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;


/**
 * 
 * 
 * @author fangzhibin
 * @version $Id$
 */
public class EcUtils {

	private transient static final Logger logger = LoggerFactory.getLogger("bap.ec.generator.EcUtils");
	public static final Map<String, Boolean> deployMap = new ConcurrentHashMap<String, Boolean>();
	public static final ConcurrentMap<String, String> deployTask = new ConcurrentHashMap<String, String>();
	
	public static final ConcurrentMap<String, String> uploadTask = new ConcurrentHashMap<String, String>();
	
	public static final ConcurrentMap<String, Object> uploadTaskBatch = new ConcurrentHashMap<String, Object>();
	
	public static final Logger uploadLogger = LoggerFactory.getLogger("configuration.upload");
	
	public static final Logger uploadFullLogger = LoggerFactory.getLogger("configuration.upload.full");
	
	public static final Map<String, Boolean> migMap = new ConcurrentHashMap<String, Boolean>();
	
	public static final Map<String, Queue<String>> deployLogMap = new ConcurrentHashMap<String, Queue<String>>();

	public static final ThreadLocal<String> generateInfoMap = new ThreadLocal<String>();

	public static final ThreadLocal<PrintWriter> fos = new ThreadLocal<>();
	
	public static final LinkedBlockingQueue<Map<String, Map<String, Map<String, Object>>>> metaInfoTasksQueue = new LinkedBlockingQueue<Map<String, Map<String, Map<String, Object>>>>();
	
	public static final LinkedBlockingQueue<Map<String, String>> dealFileTasksQueue = new LinkedBlockingQueue<Map<String, String>>();

	public static void log_info(String msg, Writer... writers) {
		try {
			if (writers != null && writers.length > 0 && null != msg) {
				if(writers.length==1&&writers[0]==null){
					
				}else{
					writers[0].append("<br/>").append(msg);
					writers[0].flush();
				}
				logger.info(msg);
			}
		} catch (IOException e) {
			logger.warn(e.getMessage(), e);
		}
	}
	
	/**
	 * 输出错误信息
	 * @param msg
	 * @param exception
	 * @param writer
	 */
	public static void log_error(String msg, Exception exception, Writer writer) {
		try {
			writer.append("<br/><span style=\"color:red\">").append(msg).append("</span><br/>").flush();
			writer.append(exception.getMessage()).flush();
			logger.error(msg);
			logger.error(exception.getMessage(), exception);
		} catch (IOException e) {
			logger.warn(e.getMessage(), e);
		}
	}
	
	public static void alert(String msg, Writer writer) {
		try {
			if(writer!=null){
				writer.append("<script type=\"text/javascript\">alert('").append(msg).append("');</script>").flush();
			}
			logger.info(msg);
		} catch (IOException e) {
			logger.warn(e.getMessage(), e);
		}
	
		
	}

	public static void close() {
		PrintWriter writer = fos.get();
		if (writer != null) {
			writer.flush();
			writer.close();
		}
		fos.set(null);
	}
}
