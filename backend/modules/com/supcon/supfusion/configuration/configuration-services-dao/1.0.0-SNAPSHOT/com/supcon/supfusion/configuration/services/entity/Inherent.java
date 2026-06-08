package com.supcon.supfusion.configuration.services.entity;

import com.supcon.supfusion.configuration.services.enums.DbColumnType;

import java.util.HashMap;

public class Inherent {

	public static HashMap<String, String> inherentMainMap = new HashMap<String, String>(){{
	      put("ID","id");
	      put("TABLE_INFO_ID","tableInfoId");
	      put("CREATE_DEPARTMENT_ID","createDepartment");
	      put("CREATE_POSITION_ID","createPosition");
	      put("GROUP_ID","groupId");
	      put("CREATE_STAFF_ID","createStaff");
	      put("MODIFY_STAFF_ID","modifyStaff");
	      put("DELETE_STAFF_ID","deleteStaff");
	      put("EFFECT_STAFF_ID","effectStaff");
	      put("CREATE_TIME","createTime");
	      put("MODIFY_TIME","modifyTime");
	      put("DELETE_TIME","deleteTime");
	      put("EFFECT_TIME","effectTime");
	      put("STATUS","status");
	      put("EXTRA_COL","extraCol");
	      put("TABLE_NO","tableNo");
	      put("POSITION_LAY_REC","positionLayRec");
	      put("OWNER_DEPARTMENT_ID","ownerDepartment");
	      put("OWNER_POSITION_ID","ownerPosition");
	      put("OWNER_STAFF_ID","ownerStaff");
	      put("VERSION","version");
	      put("EFFECTIVE_STATE","effectiveState");
	      put("VALID","valid");
	      put("CID","cid");
	      put("SORT","sort");
	      put("DEPLOYMENT_ID","deploymentId"); 
	      put("PROCESS_KEY","processKey");
	      put("PROCESS_VERSION","processVersion");
	}};
	
	public static HashMap<String, String> inherentMap = new HashMap<String, String>(){{
	      put("ID","id");
	      put("TABLE_INFO_ID","tableInfoId");
	      put("CREATE_STAFF_ID","createStaff");
	      put("MODIFY_STAFF_ID","modifyStaff");
	      put("DELETE_STAFF_ID","deleteStaff");
	      put("CREATE_TIME","createTime");
	      put("MODIFY_TIME","modifyTime");
	      put("DELETE_TIME","deleteTime");
	      put("VERSION","version");
	      put("VALID","valid");
	      put("CID","cid");
	      put("SORT","sort");
	}};
	
	public static HashMap<String, DbColumnType> inherentTypeMap = new HashMap<String, DbColumnType>(){{
	      put("id",DbColumnType.LONG);
	      put("tableInfoId",DbColumnType.LONG);
	      put("createDepartment",DbColumnType.OBJECT);
	      put("createPosition",DbColumnType.OBJECT);
	      put("groupId",DbColumnType.LONG);
	      put("createStaff",DbColumnType.OBJECT);
	      put("modifyStaff",DbColumnType.OBJECT);
	      put("deleteStaff",DbColumnType.OBJECT);
	      put("effectStaff",DbColumnType.OBJECT);
	      put("createTime",DbColumnType.DATETIME);
	      put("modifyTime",DbColumnType.DATETIME);
	      put("deleteTime",DbColumnType.DATETIME);
	      put("effectTime",DbColumnType.DATETIME);
	      put("status",DbColumnType.INTEGER);
	      put("extraCol",DbColumnType.LONGTEXT);
	      put("tableNo",DbColumnType.TEXT);
	      put("positionLayRec",DbColumnType.TEXT);
	      put("ownerDepartment",DbColumnType.OBJECT);
	      put("ownerPosition",DbColumnType.OBJECT);
	      put("ownerStaff",DbColumnType.OBJECT);
	      put("version",DbColumnType.INTEGER);
	      put("effectiveState",DbColumnType.INTEGER);
	      put("valid",DbColumnType.BOOLEAN);
	      put("cid",DbColumnType.LONG);
	      put("sort",DbColumnType.INTEGER);
	      put("deploymentId",DbColumnType.LONG); 
	      put("processKey",DbColumnType.TEXT);
	      put("processVersion",DbColumnType.INTEGER);
	}};
	
	public static HashMap<String, String> assPropertyMap = new HashMap<String, String>(){{
	      put("createDepartment","base_department_id");
	      put("createPosition","base_position_id");
	      put("createStaff","base_staff_id");
	      put("modifyStaff","base_staff_id");
	      put("deleteStaff","base_staff_id");
	      put("effectStaff","base_staff_id");
	      put("ownerDepartment","base_department_id");
	      put("ownerPosition","base_position_id");
	      put("ownerStaff","base_staff_id");
	}};
	
	public static String getInherentByColumn(String column, boolean isMain) {
		String name = null;
		if (isMain) {
			name = inherentMainMap.get(column.toUpperCase());
		} else {
			name = inherentMap.get(column.toUpperCase());
		}
		return name;
	}
	
	public static DbColumnType getInherentType(String name) {
		DbColumnType type = inherentTypeMap.get(name);
		return type;
	}
	
	public static String getAssPropertyCode(String name) {
		String assProperty = assPropertyMap.get(name);
		return assProperty;
	}
}
