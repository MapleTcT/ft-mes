package com.supcon.supfusion.configuration.services.utils;

import com.supcon.supfusion.configuration.services.i18n.InternationalResource;
import com.supcon.supfusion.configuration.services.utils.DateUtils;
import flexjson.JSONDeserializer;


import java.util.*;
import java.util.Map.Entry;

public class SqlParser {
	
	public static final String ESCAPE = "escape '/'";
	
	/** SQL中like查询中特殊字符转换，key是原来字符，value是转换后的字符. 其他方式是先在页面中进行特殊字符检验 */
	private static final Map<String, String> specialSymbolInLikeQuery = new HashMap<>();

	static {
		//目前只处理这两个特殊字符
		specialSymbolInLikeQuery.put("_", "/_");
		specialSymbolInLikeQuery.put("%", "/%");
	}
	
	public static String afterEscape(String originalStr) {
		for (Entry<String, String> entry : specialSymbolInLikeQuery.entrySet()) {
			if (originalStr.indexOf(entry.getKey()) > 0) {
				originalStr = originalStr.replace(entry.getKey(),
						entry.getValue());
			}
		}
		return originalStr;
	}

	public static String filtrateSQLLike(String likeStr) {
		String str="";

		String s1 = likeStr.replaceAll("&", "&&");
		String s2 = s1.replaceAll("%", "&%");
		String s3 = s2.replaceAll("_", "&_");
//		str = StringUtils.replace(likeStr, "&", "&&");
//		str = StringUtils.replace(str, "%", "&%");
//		// str = StringUtils.replace(str, "#", "&#");
//		str = StringUtils.replace(str, "_", "&_");
		return s3;
	}

	// public static void main(String[] args) {
	//	String likeStr="#";
	//	System.out.println(filtrateSQLLike(likeStr));
	//}
	 public static Map<String, String> parserAdvConditions(String queryConditionJson, List<Object> parameters) throws IllegalArgumentException {
		 JSONDeserializer<List<Map<String, String>>> jsonDeserializer = new JSONDeserializer<List<Map<String, String>>>();
			List<Map<String, String>> queryMapList = jsonDeserializer.deserialize(queryConditionJson);
			Object[] objects=parameters.toArray();
			Map<String, String> sqlMap = new LinkedHashMap<String, String>();
			String oldTable="";
			List<Object> paramList=new LinkedList<>();
			for (Map<String, String> map : queryMapList) {
				String tail = null;
				String tableName = map.get("table");
				
				String column = map.get("column");
				String operator = map.get("operator");
				String connector=map.get("connector");
				String type = map.get("displayType");
				// TODO 逐一对 operator, tableName, column 做有效性验证
				if(tableName==null||"".equals(tableName)){
					throw new IllegalArgumentException(InternationalResource.get("container.sqlparser.tableisnull"));
				}
				if(column==null||"".equals(column)){
					throw new IllegalArgumentException(InternationalResource.get("container.sqlparser.columnisnull"));
				}
				if(operator==null||"".equals(operator)){
					throw new IllegalArgumentException(InternationalResource.get("container.sqlparser.operator"));
				}
				
				/*String regEx="^[a-zA-Z][a-zA-Z0-9_]{0,30}$";
				Pattern pat = Pattern.compile(regEx);   
				Matcher tableMat = pat.matcher(tableName);  
				Matcher colMat = pat.matcher(tableName);
				boolean tableRs = tableMat.find();
				boolean colRs=colMat.find();
				if(!tableRs){
					throw new IllegalArgumentException("表名必须是以字母开头，且有字母，数字和下划线组成,长度不能超过30字符！");
				}
				if(!colRs){
					throw new IllegalArgumentException("字段名称必须是以字母开头，且有字母，数字和下划线组成,长度不能超过30字符！");
				}*/
				/*if(!operator.toUpperCase().equals("ilike")&&!operator.toUpperCase().equals("LIKE")&&!operator.equals("<")&&!operator.equals("<=")&&!operator.equals("=")&&!operator.equals(">")&&!operator.equals(">=")){
					throw new IllegalArgumentException("操作符不符合要求，只能为=、>、>=、<、<=！");
				}
				if(connector!=null&&!connector.trim().equals("")){
					if(!connector.toUpperCase().equals("AND")&&!connector.toUpperCase().equals("OR")){
						throw new IllegalArgumentException("连接符不符合要求，只能为AND、OR！");
					}
				}*/
				if(tableName.equals("BASE_STAFF")&&(column.equals("ADVCOMPANYNAME")||column.equals("ASVDEPARTMENTNAME")||column.equals("ADVPOSITIONNAME"))){
					paramList.add("BASE_STAFF");
					sqlMap.put(column, operator+"&&"+map.get("id"));
					continue;
				}
				
				String value = map.get("value");
				if (operator.equals("like")) {
					value = "%" + value + "%";
				} 
				if(sqlMap.get(tableName)!=null&&!oldTable.equals(tableName)){
					int index=paramList.lastIndexOf(tableName);
					if(type.equals("DATE")){
						parameters.add(index, DateUtils.parseDate(value));
					}else{
						parameters.add(index,value);
					}
					
				}else{
					if(type.equals("DATE")){
						parameters.add(DateUtils.parseDate(value));
					}else{
						parameters.add(value);
					}	
				}
				if (sqlMap.get(tableName) == null) {
					sqlMap.put(tableName, "");
					tail = " ";
				} else {
					if(connector==null||connector.equals("")){
						tail = " AND ";
					}else{
						tail = " "+connector+" ";
					}
				}
				
				
				
				if(tableName.toUpperCase().equals("BASE_STAFF")){
					sqlMap.put(tableName, sqlMap.get(tableName) + tail + " BASE_STAFF_ALIAS." +column + " " + operator + " ?");
				}else{
					sqlMap.put(tableName, sqlMap.get(tableName) + tail + column + " " + operator + " ?");
				}
			
				paramList.add(tableName);
				oldTable=tableName;
			}
			return sqlMap;
		}
    	
}
