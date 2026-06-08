/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 

 * All rights reserved.
 */
package com.supcon.supfusion.configuration.services.service.impl;

import com.supcon.supfusion.configuration.services.service.EcDataSynchronizeService;
import com.supcon.supfusion.configuration.services.utils.DBColumnNames;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;



@Slf4j
@ServiceApiService
@Transactional
public class EcDataSynchronizeServiceImpl implements EcDataSynchronizeService {

	private static final String TABLENAME_PREFIX_RUNTIME = "RUNTIME_";
	private static final String TABLENAME_PREFIX_DEV = "EC_";
	private static final String TABLENAME_PREFIX_PROJECT = "PROJECT_";
	private enum EcSynEnum {
		// need synchronize model
		MODULE_RELATION, // 关联关系
		DATA_CLASSIFIC, // 数据分类
		DATA_GROUP, // 数据分组
		SQL, // SQL
		VIEW, // 视图
		EXTRA_VIEW, // EXTRA_VIEW
		PROPERTY, // 字段
		MODEL, // 模型
		ENTITY, // 实体
		MODULE, // 模块
		EXTRA_QUERY_JSON, // 视图配置信息
		CUSTOMER_CONDITION, // 自定义条件
		BUTTON, // 按钮
		DATA_GRID, // datagrid
		FIELD, // 视图字段
		EVENT, // 事件
		VALIDATE, // 验证
		FAST_QUERY_JSON, // 快速查询
		ADV_QUERY_JSON, // 高级查询
		IMPORT_TEMPLATE, //导入导出模板
		ECHARTS, // 图表属性
		ECHARTS_MODEL // 图表数据源属性
	}

	private enum ViewSynEnum {
		// need synchronize model
		DATA_CLASSIFIC, // 数据分类
		DATA_GROUP, // 数据分组
		SQL, // SQL
		VIEW, // 视图
		EXTRA_VIEW, // EXTRA_VIEW
		EXTRA_QUERY_JSON, // 视图配置信息
		CUSTOMER_CONDITION, // 自定义条件
		BUTTON, // 按钮
		DATA_GRID, // datagrid
		FAST_QUERY_JSON,	//快速查询
		ADV_QUERY_JSON,	//高级查询
		FIELD, // 视图字段
		EVENT, // 事件
		VALIDATE, // 验证
		ECHARTS,
		ECHARTS_MODEL
	}

	public static final int DIRECTION_OTHER = 0;
	public static final int DIRECTION_DEV_TO_RUNTIME = 1;
	public static final int DIRECTION_DEV_TO_PROJ = 2;
	public static final int DIRECTION_PROJ_TO_RUNTIME = 3;
	public static final int DIRECTION_NO_TO_RUNTIME = 4;
	public static final int DIRECTION_DEV_TO_RUNTIME_DEV_TO_PROJ = 5;

	public static final int SCOPE_MODULE = 1;
	public static final int SCOPE_VIEW = 2;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private List<Map<String, String>> ecRelations = null;

	@Override
	public synchronized void synchronizeEcDataFromDevToRumtime(String moduleCode) {
		if (StringUtils.isEmpty(moduleCode)) {
			return;
		}
		long timestamp = System.currentTimeMillis();
		log.info("===============开始进行同步EC-->RUNTIME=========");
		synchronizeObjInfo(moduleCode, SCOPE_MODULE, DIRECTION_DEV_TO_RUNTIME);
		log.info("===============同步EC-->RUNTIME耗时" + (System.currentTimeMillis() - timestamp) + " ms=========");
	}

	private boolean isDevToRuntime(int direction) {
		return DIRECTION_DEV_TO_RUNTIME == direction;
	}
	private boolean isDevToProj(int direction) {
		return DIRECTION_DEV_TO_PROJ == direction;
	}
	private boolean isModuleType(int type) {
		return SCOPE_MODULE == type;
	}

	@SuppressWarnings("rawtypes")
	private void synchronizeObjInfo(String syncCode, int type, int direction) {
		synchronized (EcDataSynchronizeServiceImpl.class) {
			EnumSet items = null;
			String sourceTableNamePrefix ;
			String targetTableNamePrefix ;
			if (isDevToRuntime(direction)) {
				sourceTableNamePrefix = TABLENAME_PREFIX_DEV;
				targetTableNamePrefix = TABLENAME_PREFIX_RUNTIME;
			} else if (isDevToProj(direction)) {
				sourceTableNamePrefix = TABLENAME_PREFIX_DEV;
				targetTableNamePrefix = TABLENAME_PREFIX_PROJECT;
			} else {
				sourceTableNamePrefix = TABLENAME_PREFIX_PROJECT;
				targetTableNamePrefix = TABLENAME_PREFIX_RUNTIME;
			}

			if (isModuleType(type)) {
				items = EnumSet.allOf(EcSynEnum.class);
			} else {
				items = EnumSet.allOf(ViewSynEnum.class);
			}
			ArrayList<String> als=new ArrayList<String>();
			for (Object item : items) {
				String sTable=sourceTableNamePrefix+item.toString();
				String tTable=targetTableNamePrefix+item.toString();
				if (isDevToRuntime(direction)) {
					als.add("delete from "+tTable+" where (PROJ_FLAG is null or PROJ_FLAG <> 1) and code like '"+syncCode+"%'");
					als.add("insert into "+tTable+"(" + DBColumnNames.COLUMN_NAMES.get(item.toString())+") select " + DBColumnNames.COLUMN_NAMES.get(item.toString())
							+ " from "+sTable+" where code like '"+syncCode+"%' and code not in (select code from "+tTable+" where code like '"+syncCode+"%')");
				}else{
					als.add("delete from "+tTable+" where code like '"+syncCode+"%'");
					als.add("insert into "+tTable+"(" + DBColumnNames.COLUMN_NAMES.get(item.toString())+") select " + DBColumnNames.COLUMN_NAMES.get(item.toString()) 
							+ " from "+sTable+" where code like '"+syncCode+"%'");
				}
			}
			String[] sa=new String[1];
			jdbcTemplate.batchUpdate(als.toArray(sa));
		}
	}

	@Override
	public void synchronizeViewDataFromEC(String viewCode, int syncFlag) {
		if (StringUtils.isEmpty(viewCode)) {
			return;
		}
		if(syncFlag != DIRECTION_NO_TO_RUNTIME){
			if(syncFlag == DIRECTION_DEV_TO_RUNTIME){
				synchronizeObjInfo(viewCode, SCOPE_VIEW, DIRECTION_DEV_TO_RUNTIME);
			}else if(syncFlag == DIRECTION_DEV_TO_PROJ){
				synchronizeObjInfo(viewCode, SCOPE_VIEW, DIRECTION_DEV_TO_PROJ);
			}else if(syncFlag == DIRECTION_PROJ_TO_RUNTIME){
				synchronizeObjInfo(viewCode, SCOPE_VIEW, DIRECTION_DEV_TO_PROJ);
				synchronizeObjInfo(viewCode, SCOPE_VIEW, DIRECTION_PROJ_TO_RUNTIME);
			}else if(syncFlag == DIRECTION_DEV_TO_RUNTIME_DEV_TO_PROJ){
				synchronizeObjInfo(viewCode, SCOPE_VIEW, DIRECTION_DEV_TO_PROJ);
				synchronizeObjInfo(viewCode, SCOPE_VIEW, DIRECTION_DEV_TO_RUNTIME);
			}else{
				synchronizeObjInfo(viewCode, SCOPE_VIEW, DIRECTION_DEV_TO_PROJ);
				synchronizeObjInfo(viewCode, SCOPE_VIEW, DIRECTION_DEV_TO_RUNTIME);
				synchronizeObjInfo(viewCode, SCOPE_VIEW, DIRECTION_PROJ_TO_RUNTIME);
			}
		}
	}
	/*
	 * (non-Javadoc)
	 *
	 * @see com.supcon.orchid.entityconf.services.EcDataSynchronizeService#synchronizeViewDataFromProj(java.lang.String)
	 */
	@Override
	public void synchronizeViewDataFromProj(String viewCode) {
		if (StringUtils.isEmpty(viewCode)) {
			return;
		}
		synchronizeObjInfo(viewCode, SCOPE_VIEW, DIRECTION_PROJ_TO_RUNTIME);
//		clearViewCache();
	}

	@Override
	public void forceSynchronizeECViewDataToRuntime(String viewCode) {
		EnumSet items = EnumSet.allOf(ViewSynEnum.class);
		String sourceTableNamePrefix = TABLENAME_PREFIX_DEV;
		String targetTableNamePrefix = TABLENAME_PREFIX_RUNTIME;
		ArrayList<String> als=new ArrayList<String>();
		for (Object item : items) {
			String sTable=sourceTableNamePrefix+item.toString();
			String tTable=targetTableNamePrefix+item.toString();
			als.add("delete from "+tTable+" where code like '"+viewCode+"%'");
			als.add("insert into "+tTable+"(" + DBColumnNames.COLUMN_NAMES.get(item.toString())+") select " + DBColumnNames.COLUMN_NAMES.get(item.toString())
					+ " from "+sTable+" where code like '"+viewCode+"%'");
		}
		String[] sa=new String[1];
		jdbcTemplate.batchUpdate(als.toArray(sa));
//		postEvent(viewCode, 2, 1);
	}

}
