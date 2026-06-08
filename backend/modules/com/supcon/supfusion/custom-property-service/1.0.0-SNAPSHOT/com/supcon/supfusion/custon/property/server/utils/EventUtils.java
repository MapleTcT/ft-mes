/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.custon.property.server.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * 
 * @author zhuyuyin
 * @version $Id$
 */
public class EventUtils {

	/**
	 * 解析事件名称
	 * 
	 * @param funcname
	 * @return
	 */
	public static String[] analysisFuncname(String funcname) {
		List<String> list = new ArrayList<String>();
		if (funcname != null && funcname.length() > 0) {
			addList(funcname, list);
		}
		String[] str = new String[] {};
		return list.toArray(str);
	}

	/**
	 * 解析事件内容
	 * 
	 * @param funcbody
	 * @return
	 */
	public static String[] analysisFuncbody(String funcbody) {
		if (funcbody != null && funcbody.length() > 0) {
			return funcbody.split("@@@@");
		}
		return null;
	}

	private static void addList(String funcname, List<String> list) {
		funcname = funcname.trim();
		int index = funcname.indexOf("'") + 1;
		if (index != 0) {
			String funType = funcname.substring(0, index);
			funcname = funcname.substring(index).trim();
			index = funcname.indexOf("'") + 1;
			String funName = funcname.substring(0, index).trim();
			funcname = funcname.substring(index).trim();
			list.add(funType + funName);
			if (funcname != null && funcname.length() > 0) {
				addList(funcname, list);
			}
		}
	}

	/**
	 * 判断事件是否为callback
	 * 
	 * @param eventName
	 * @return
	 */
	public static boolean isCallBack(String eventName) {
		if (eventName != null && eventName.length() > 0) {
			int index = eventName.indexOf("=");
			if (index != -1) {
				String name = eventName.substring(0, index);
				if (name.equalsIgnoreCase("callback")) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 返回callback事件的名称
	 * 
	 * @param eventName
	 * @return
	 */
	public static String getCallBackName(String eventName) {
		if (isCallBack(eventName)) {
			int index = eventName.indexOf("=");
			if (index != -1) {
				return eventName.substring(index + 1);
			}
		}
		return null;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String name ="aa.bb.vv.nn";
		String f = "gg";
		f += name.replaceAll("\\.", "_");
		System.out.println(f);
	}

}
