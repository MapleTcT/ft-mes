package com.supcon.supfusion.systemcode.common.constants;

/**
 * 常量类
 * @author root
 *
 */
public class Constants {

	public static final String PATTERN_CODE = "^[A-Za-z0-9_]{0,100}$";

//	//entityCode系统字典项编码参数为必填，不可以为空
//	public static final String SYSTEM_ENTITY_CODE_PARAM_NECESSARY = "编码参数为必填，不可以为空";
//
//	//value系统编码值不可以为空
//	public static final String SYSTEM_CODE_VALUE_NECESSARY = "值参数为必填, 不可以为空";
//
//	//system code系统编码不可以为空
//	public static final String SYSTEM_CODE_NECESSARY = "编码参数为必填, 不可以为空";
//
//	//所属公司id不可以为空
//	public static final String COMPANY_ID_NECESSARY = "所属公司ID参数为必填, 不可以为空";
//
//	//所属公司名称不可以为空
//	public static final String COMPANY_NAME_NECESSARY = "所属公司名称参数为必填, 不可以为空";
//
//	//page分页页码不可以小于1
//	public static final String CURRENT_PAGE_MIN_1 = "分页页码不可以小于1";
//
//	//per_page每页记录数不可以小于1
//	public static final String CURRENT_PAGE_SIZE_MIN_1 = "每页记录数不可以小于1";
//
//	//system code请先选择系统编码
//	public static final String SYSTEM_CODE_IS_NOT_EMPTY = "请先选择系统编码";
//
//	//-------------------------------------------系统字典相关信息----------------------------------------//
//	// entityCode系统字典项名称参数为必填，不可以为空
//	public static final String SYSTEM_ENTITY_NAME_PARAM_NECESSARY = "名称参数为必填，不可以为空";
//
//	// entityCode系统字典项类型参数为必填，不可以为空
//	public static final String SYSTEM_ENTITY_TYPE_PARAM_NECESSARY = "类型参数为必填，不可以为空";
//
//	// entityCode系统字典项所属模块参数为必填，不可以为空
//	public static final String SYSTEM_ENTITY_MODULE_ID_PARAM_NECESSARY = "所属模块ID参数为必填，不可以为空";
//
//	// entityCode系统字典项所属公司id参数为必填，不可以为空
//	public static final String SYSTEM_ENTITY_COMPANY_ID_PARAM_NECESSARY = "所属公司ID参数为必填，不可以为空";
//
//	// entityCode系统字典项所属公司名称参数为必填，不可以为空
//	public static final String SYSTEM_ENTITY_COMPANY_NAME_PARAM_NECESSARY = "所属公司名称参数为必填，不可以为空";
//
//	// parentId上级编码的id为必填，不可以为空
//	public static final String SYSTEM_CODE_PARENTID_PARAM_NECESSARY = "父级节点ID参数为必填，不可以为空";
//
//	// codeId不可以为空
//	public static final String SYSTEM_CODE_ID_PARAM_NECESSARY = "编码ID参数为必填，不可以为空";

	//数据库类型常量字符串
	public static final String DB_TYPE_MARIADB = "mariadb";
	public static final String DB_TYPE_MYSQL = "mysql";
	public static final String DB_TYPE_ORACLE = "oracle";
	public static final String DB_TYPE_SQLSERVER = "sqlserver";

	/**
	 * 数据库通配符
	 */
	public static final String SQL_WILD_CARD = "\\%";
	public static final String SQL_ESCAPE_CHAR = "\\&";

	public static final String SQL_LIKE_CHAR = "%";

	public static final String SUPOS = "supos";

	public static final String SUPIDE = "supide";

}
