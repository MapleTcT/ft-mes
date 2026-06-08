package com.supcon.supfusion.configuration.services.utils;

import com.supcon.supfusion.configuration.services.entity.Model;
import com.supcon.supfusion.configuration.services.entity.Property;
import com.supcon.supfusion.configuration.services.enums.DbColumnType;
import com.supcon.supfusion.configuration.services.i18n.InternationalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.ObjectUtils;

import java.util.List;

/**
 * 字段管理组态期同步更新数据库工具类
 * @author penghui
 */
public class FieldSyncDBUtils {
	
	private static final Logger logger = LoggerFactory.getLogger(FieldSyncDBUtils.class);
	
	public static synchronized void fieldSyncToDb(Property property, Model model, boolean isNew, JdbcTemplate template, String dbName) {
		boolean fieldIsExist = fieldIsExist(model.getTableName().toUpperCase(), property.getColumnName(), dbName, template);
		if (!isNew && fieldIsExist) {
			if (dbName.startsWith("oracle")) {
				updateField(property, model, template);
			} else if (dbName.startsWith("sqlserver")) {
				updateFieldSqlserver(property, model, template);
			} else if (dbName.startsWith("mysql") || dbName.startsWith("mariadb")) {
				updateFieldMysql(property, model, template);
			}
		} else {
			if (dbName.startsWith("oracle")) {
				createField(property, model, template);
			} else if (dbName.startsWith("sqlserver")) {
				createFieldSqlserver(property, model, template);
			}else if (dbName.startsWith("mysql") || dbName.startsWith("mariadb")) {
				createFieldMysql(property, model, template);
			}
		}
	}
	
	public static synchronized void customFieldSyncToDb(List<Property> propertyList, JdbcTemplate template) {
		String dbName = DbUtils.getDbName();
		if (dbName.startsWith("oracle")) {
			createCustomField(propertyList, template);
		}else if (dbName.startsWith("sqlserver")) {
			createCustomFieldSqlserver(propertyList, template);
		}else if (dbName.startsWith("mysql") || dbName.startsWith("mariadb")) {
			createCustomFieldMysql(propertyList, template);
		}
	}
	
	private static boolean fieldIsExist(String tableName, String colName, String dbType, JdbcTemplate template) {
		boolean isExist = false;
		if (null != dbType) {
			if (dbType.startsWith("oracle")) {
				String testSql =  "SELECT COUNT(*) FROM user_tab_columns WHERE TABLE_NAME = '" + tableName + "' AND COLUMN_NAME = '" + colName + "'";
				if(template.queryForObject(testSql, Integer.class) > 0){
					isExist = true;
				}	
			}
			if (dbType.startsWith("sqlserver")) {
				String testSql =  "SELECT COUNT(*) FROM sysobjects a, syscolumns b WHERE a.id=b.id and b.name='" + colName + "' and a.type='u' and a.name='" + tableName + "'";
				if(template.queryForObject(testSql, Integer.class) > 0){
					isExist = true;
				}
			}
			if (dbType.startsWith("mysql") || dbType.startsWith("mariadb")) {
				String testSql = "SELECT count(1) from information_schema.columns t WHERE t.TABLE_SCHEMA= '"+DbUtils.getCurrentDBName()+"' and table_name='" + tableName + "' and column_name = '" + colName + "'";
				if(template.queryForObject(testSql, Integer.class) > 0){
					isExist = true;
				}
			}
		}
		return isExist;
	}
	
	private static boolean fieldIndexIsExist(String tableName, String indexName, JdbcTemplate template) {
		boolean retBool = false;
		String tarSql = "";
		String dbName = DbUtils.getDbName();
		if (dbName.startsWith("sqlserver")) {
			tarSql = "select count(1) from sys.indexes where name like '%" + indexName + "' and object_name(object_id)='" + tableName + "'";
		}
		if (dbName.startsWith("oracle")) {
			tarSql = "select count(1) from user_indexes where index_name like '%" + indexName + "' and table_name='" + tableName + "'";
		}
		if (dbName.startsWith("mysql") || dbName.startsWith("mariadb")) {
			tarSql = "SELECT count(1) FROM mysql.innodb_index_stats  where database_name='" + DbUtils.getCurrentDBName() + "' and index_name like '%" + indexName + "' and table_name='" + tableName.toUpperCase() + "'";
			int retInt = template.queryForObject(tarSql, Integer.class);
			if (0 == retInt) {
				tarSql = "SELECT count(1) FROM mysql.innodb_index_stats  where database_name='" + DbUtils.getCurrentDBName() + "' and index_name like '%" + indexName + "' and table_name='" + tableName.toLowerCase() + "'";
			}
		}
		if (!"".equals(tarSql)) {
			int retInt = template.queryForObject(tarSql, Integer.class);
			if(retInt > 0){
				retBool = true;
			}
		}
		return retBool;
	}
	
	public static boolean tableIsExist(String tableName, JdbcTemplate template) {
		boolean retBool = false;
		String tarSql = "";
		String dbName = DbUtils.getDbName();
		if (dbName.startsWith("sqlserver")) {
			tarSql = "select count(1) from sys.tables where name='" + tableName + "'";
		}else if (dbName.startsWith("oracle")) {
			tarSql = "select count(1) from user_tables where table_name='" + tableName + "'";
		} else if (dbName.startsWith("mysql") || dbName.startsWith("mariadb")) {
			tarSql = "select count(1) from information_schema.tables t where t.TABLE_SCHEMA='"+ DbUtils.getCurrentDBName() +"' and table_name ='" + tableName + "'";
		}
		if (!"".equals(tarSql)) {
			int retInt = template.queryForObject(tarSql, Integer.class);
			retBool = ((1 <= retInt) ? true : false);
		}
		return retBool;
	}
	
	private static synchronized void createField(Property property, Model model, JdbcTemplate template) {
		String modelTableName = model.getTableName().toUpperCase();
		if(property != null){
		String colName = property.getColumnName();
		StringBuilder tarSql = new StringBuilder("");
		if (property.getType() == DbColumnType.TEXT) {//maxLength-update db
			Integer maxLength = property.getMaxLength();
			maxLength = (null != maxLength && maxLength > 0) ? maxLength * 2 : 510;
			tarSql.append("ALTER TABLE " + modelTableName + " ADD (" + colName + " VARCHAR2(" + maxLength + " CHAR));");
		}
		if (property.getType() == DbColumnType.BAPCODE) {//maxLength-not update db
			tarSql.append("ALTER TABLE " + modelTableName + " ADD (" + colName + " VARCHAR2(4000 CHAR)" + ");");
		}
		if (property.getType() == DbColumnType.SUMMARY) {//maxLength-not update db
			tarSql.append("ALTER TABLE " + modelTableName + " ADD (" + colName + " VARCHAR2(4000 CHAR)" + ");");
		}
		if (property.getType() == DbColumnType.INTEGER) {
			tarSql.append("ALTER TABLE " + modelTableName + " ADD (" + colName + " NUMBER(*,0)" + ");");
		}
		if (property.getType() == DbColumnType.DECIMAL) {//decimalNum
			tarSql.append("ALTER TABLE " + modelTableName + " ADD (" + colName + " NUMBER(19," + property.getDecimalNum() + "));");
		}
		if (property.getType() == DbColumnType.DATE) {
			tarSql.append("ALTER TABLE " + modelTableName + " ADD (" + colName + " TIMESTAMP (6)" + ");");
		}
		if (property.getType() == DbColumnType.TIME) {
			tarSql.append("ALTER TABLE " + modelTableName + " ADD (" + colName + " TIMESTAMP (6)" + ");");
		}
		if (property.getType() == DbColumnType.DATETIME) {
			tarSql.append("ALTER TABLE " + modelTableName + " ADD (" + colName + " TIMESTAMP (6)" + ");");
		}
		if (property.getType() == DbColumnType.BOOLEAN) {
			tarSql.append("ALTER TABLE " + modelTableName + " ADD (" + colName + " NUMBER(*,0)" + ");");
		}
		if (property.getType() == DbColumnType.LONGTEXT) {//maxLength-not update db
			tarSql.append("ALTER TABLE " + modelTableName + " ADD (" + colName + " CLOB" + ");");
		}
		if (property.getType() == DbColumnType.LONG) {
			tarSql.append("ALTER TABLE " + modelTableName + " ADD (" + colName + " NUMBER(19,0)" + ");");
		}
		if (property.getType() == DbColumnType.OBJECT) {
			if(property.getAssociatedProperty().getType()== DbColumnType.LONG) {
				tarSql.append("ALTER TABLE " + modelTableName + " ADD (" + colName + " NUMBER(19,0)" + ");");
			}else if(property.getAssociatedProperty().getType()== DbColumnType.BAPCODE) {
				tarSql.append("ALTER TABLE " + modelTableName + " ADD (" + colName + " VARCHAR2(4000 CHAR)" + ");");
			}else {
				Integer maxLength = property.getAssociatedProperty().getMaxLength();
				maxLength = (null != maxLength && maxLength > 0) ? maxLength * 2 : 510;
				tarSql.append("ALTER TABLE " + modelTableName + " ADD (" + colName + " VARCHAR2(" + maxLength + " CHAR));");
			
			}
		}
		if (property.getType() == DbColumnType.SYSTEMCODE) {
			tarSql.append("ALTER TABLE " + modelTableName + " ADD (" + colName + " VARCHAR2(4000 CHAR)" + ");");
		}
		if (property.getType() == DbColumnType.MONEY) {//decimalNum
			tarSql.append("ALTER TABLE " + modelTableName + " ADD (" + colName + " NUMBER(19," + property.getDecimalNum() + "));");
		}
		if (property.getType() == DbColumnType.PASSWORD) {//maxLength-not update db
			tarSql.append("ALTER TABLE " + modelTableName + " ADD (" + colName + " VARCHAR2(510 CHAR)" + ");");
		}
		if (property.getType() == DbColumnType.PICTURE) {
			tarSql.append("ALTER TABLE " + modelTableName + " ADD (" + colName + " VARCHAR2(510 CHAR)" + ");");
		}
		if (property.getType() == DbColumnType.PROPERTYATTACHMENT) {//not db
		}
		if (property.getType() == DbColumnType.OFFICE) {
			tarSql.append("ALTER TABLE " + modelTableName + " ADD (" + colName + " CLOB" + ");");
		}
		if (property.getType() == DbColumnType.TAGNUMBER) {
			tarSql.append("ALTER TABLE " + modelTableName + " ADD (" + colName + " VARCHAR2(510 CHAR)" + ");");
		}
		if (property.getType() != DbColumnType.PROPERTYATTACHMENT){
			tarSql.append("COMMENT ON COLUMN " + modelTableName + "." + colName + " IS '" + InternationalResource.get(property.getDisplayName()) + "';");
			if (null != property && null != property.getIsIndex() && property.getIsIndex()) {
				tarSql.append("CREATE INDEX IDX_" + model.getModelName() + "_" + colName + " ON " + modelTableName + "(" + colName + ");");
			}
			// 执行前判断，模型中列是否存在；如果存在则跳过
			String testSql =  "SELECT COUNT(*) FROM USER_TAB_COLUMNS WHERE TABLE_NAME = '" + modelTableName + "' AND COLUMN_NAME = '" + colName + "'";
			if(template.queryForObject(testSql, Integer.class) == 0){
				if (!"".equals(tarSql.toString())) {
					logger.debug("tarSql: " + tarSql.toString());
					template.batchUpdate(tarSql.toString().split("\\;"));
				}
			}
		}}
	}
	
	private static synchronized void updateField(Property property, Model model, JdbcTemplate template) {//含固有字段和自定义字段的修改
		String modelTableName = model.getTableName().toUpperCase();
		String modelName = model.getModelName().toUpperCase();
		String oldColName = property.getOrgColumnName().toUpperCase();
		String colName = property.getColumnName().toUpperCase();
		StringBuilder tarSql = new StringBuilder("");
		if (null != property && null != property.getIsIndex() && property.getIsIndex()) {
			String indexName = "IDX_" + modelName;
			if (property.getName().length() > 9) {
				indexName += "_" + property.getName().toUpperCase().substring(0, 8);
			} else {
				indexName += "_" + property.getName().toUpperCase();
			}
			if (!fieldIndexIsExist(modelTableName, indexName, template)) {
				tarSql.append("CREATE INDEX " + indexName + " ON " + modelTableName + "(" + colName + ");");
			} else {
				if (!ObjectUtils.isEmpty(oldColName) && !oldColName.toUpperCase().equals(colName)) {
					tarSql.append("DROP INDEX " + indexName + ";");
					tarSql.append("CREATE INDEX " + indexName + " ON " + modelTableName + "(" + colName + ");");
				}
			}
		}
		if (property.getType() == DbColumnType.TEXT || property.getType() == DbColumnType.BAPCODE) {
			Integer maxLength = property.getMaxLength();
			if (null != maxLength && maxLength > 0) {
				maxLength = maxLength * 2;
				if(maxLength > 4000){
					maxLength = 4000;
				}
				if (!ObjectUtils.isEmpty(oldColName)) {
					tarSql.append("ALTER TABLE " + modelTableName + " MODIFY (" + oldColName + " VARCHAR2(" + maxLength + " CHAR));");//执行变更SQL，不关心变更是否成功
				}
			}
		}
		if (!ObjectUtils.isEmpty(oldColName) && !oldColName.toUpperCase().equals(colName)) {
			tarSql.append("ALTER TABLE " + modelTableName + " RENAME COLUMN " + oldColName + " TO " + colName + ";");
		}
		if (!property.getIsInherent()) {
			tarSql.append("COMMENT ON COLUMN " + modelTableName + "." + colName + " IS '" + InternationalResource.get(property.getDisplayName()) + "';");
		}
		if (property.getType() != DbColumnType.PROPERTYATTACHMENT){
			if (!"".equals(tarSql.toString())) {
				logger.debug("tarSql: " + tarSql.toString());
				template.batchUpdate(tarSql.toString().split("\\;"));
			}
		}
	}
	
	private static synchronized void createCustomField(List<Property> propertyList, JdbcTemplate template) {
		if (null == propertyList || propertyList.size() <= 0) {
			return;
		}
		StringBuilder tarSql = new StringBuilder("");
		for (Property prop : propertyList) {
			if (null != prop.getIsCustom() && prop.getIsCustom()) {
				String modelTableName = prop.getModel().getTableName().toUpperCase();
				String colName = prop.getColumnName();
				String i18nFieldComment =  InternationalResource.get(prop.getDisplayName());
				if (prop.getType() == DbColumnType.TEXT) {
					tarSql.append("ALTER TABLE " + modelTableName + " ADD (" + colName + " VARCHAR2(4000 CHAR)" + ");");
				}
				if (prop.getType() == DbColumnType.INTEGER) {
					tarSql.append("ALTER TABLE " + modelTableName + " ADD (" + colName + " NUMBER(*,0)" + ");");
				}
				if (prop.getType() == DbColumnType.DECIMAL) {
					tarSql.append("ALTER TABLE " + modelTableName + " ADD (" + colName + " NUMBER(19,6)" + ");");
				}
				if (prop.getType() == DbColumnType.DATETIME) {
					tarSql.append("ALTER TABLE " + modelTableName + " ADD (" + colName + " TIMESTAMP (6)" + ");");
				}
				if (prop.getType() == DbColumnType.SYSTEMCODE) {
					tarSql.append("ALTER TABLE " + modelTableName + " ADD (" + colName + " VARCHAR2(4000 CHAR)" + ");");
				}
				if (prop.getType() == DbColumnType.OBJECT) {
					tarSql.append("ALTER TABLE " + modelTableName + " ADD (" + colName + " NUMBER(19,0)" + ");");
				}
				tarSql.append("COMMENT ON COLUMN " + modelTableName + "." + colName + " IS '" + i18nFieldComment + "';");
			}
		}
		if (!"".equals(tarSql.toString())) {
			logger.debug("tarSql: " + tarSql.toString());
			template.batchUpdate(tarSql.toString().split("\\;"));
		}
	}
	//-----------------------------sqlserver------------------------------------------
	private static synchronized void createFieldSqlserver(Property property, Model model, JdbcTemplate template) {
		String modelTableName = model.getTableName().toUpperCase();
		if(property!=null){
		String colName = property.getColumnName();
		StringBuilder tarSql = new StringBuilder("");
		if (property.getType() == DbColumnType.TEXT) {//maxLength-update db
			Integer maxLength = property.getMaxLength();
			maxLength = (null != maxLength && maxLength > 0) ? maxLength * 2 : 510;
			tarSql.append("ALTER TABLE " + modelTableName + " ADD " + colName + " varchar(" + maxLength + ");");
		}
		if (property.getType() == DbColumnType.BAPCODE) {//maxLength-not update db
			tarSql.append("ALTER TABLE " + modelTableName + " ADD " + colName + " varchar(4000)" + ";");
		}
		if (property.getType() == DbColumnType.SUMMARY) {//maxLength-not update db
			tarSql.append("ALTER TABLE " + modelTableName + " ADD " + colName + " varchar(4000)" + ";");
		}
		if (property.getType() == DbColumnType.INTEGER) {
			tarSql.append("ALTER TABLE " + modelTableName + " ADD " + colName + " int" + ";");
		}
		if (property.getType() == DbColumnType.DECIMAL) {//decimalNum
			tarSql.append("ALTER TABLE " + modelTableName + " ADD " + colName + " numeric(19," + property.getDecimalNum() + ")" + ";");
		}
		if (property.getType() == DbColumnType.DATE) {
			tarSql.append("ALTER TABLE " + modelTableName + " ADD " + colName + " datetime" + ";");
		}
		if (property.getType() == DbColumnType.TIME) {
			tarSql.append("ALTER TABLE " + modelTableName + " ADD " + colName + " datetime" + ";");
		}
		if (property.getType() == DbColumnType.DATETIME) {
			tarSql.append("ALTER TABLE " + modelTableName + " ADD " + colName + " datetime" + ";");
		}
		if (property.getType() == DbColumnType.BOOLEAN) {
			tarSql.append("ALTER TABLE " + modelTableName + " ADD " + colName + " int" + ";");
		}
		if (property.getType() == DbColumnType.LONGTEXT) {//maxLength-not update db
			tarSql.append("ALTER TABLE " + modelTableName + " ADD " + colName + " text" + ";");
		}
		if (property.getType() == DbColumnType.LONG) {
			tarSql.append("ALTER TABLE " + modelTableName + " ADD " + colName + " numeric" + ";");
		}
		if (property.getType() == DbColumnType.OBJECT) {
			if(property.getAssociatedProperty().getType()== DbColumnType.LONG) {
				tarSql.append("ALTER TABLE " + modelTableName + " ADD " + colName + " numeric" + ";");
			}else if(property.getAssociatedProperty().getType()== DbColumnType.BAPCODE) {
				tarSql.append("ALTER TABLE " + modelTableName + " ADD " + colName + " varchar(4000)" + ";");
			}else {
				Integer maxLength = property.getAssociatedProperty().getMaxLength();
				maxLength = (null != maxLength && maxLength > 0) ? maxLength * 2 : 510;
				tarSql.append("ALTER TABLE " + modelTableName + " ADD " + colName + " varchar(" + maxLength + ");");
			}
			
		}
		if (property.getType() == DbColumnType.SYSTEMCODE) {
			tarSql.append("ALTER TABLE " + modelTableName + " ADD " + colName + " varchar(4000)" + ";");
		}
		if (property.getType() == DbColumnType.MONEY) {//decimalNum
			tarSql.append("ALTER TABLE " + modelTableName + " ADD " + colName + " numeric(19," + property.getDecimalNum() + ")"  + ";");
		}
		if (property.getType() == DbColumnType.PASSWORD) {//maxLength-not update db
			tarSql.append("ALTER TABLE " + modelTableName + " ADD " + colName + " varchar(510)" + ";");
		}
		if (property.getType() == DbColumnType.PICTURE) {
			tarSql.append("ALTER TABLE " + modelTableName + " ADD " + colName + " varchar(510)" + ";");
		}
		if (property.getType() == DbColumnType.PROPERTYATTACHMENT) {//not db
		}
		if (property.getType() == DbColumnType.OFFICE) {
			tarSql.append("ALTER TABLE " + modelTableName + " ADD " + colName + " text" + ";");
		}
		if (property.getType() == DbColumnType.TAGNUMBER) {
			tarSql.append("ALTER TABLE " + modelTableName + " ADD " + colName + " varchar(510)" + ";");
		}
		if (property.getType() != DbColumnType.PROPERTYATTACHMENT) {
			if (null != property && null != property.getIsIndex() && property.getIsIndex()) {
				tarSql.append("CREATE INDEX IDX_" + model.getModelName() + "_" + colName + " ON " + modelTableName + "(" + colName + ");");
			}
			// 执行前判断，模型中列是否存在；如果存在则跳过
			String testSql =  "SELECT COUNT(*) FROM sysobjects a, syscolumns b WHERE a.id=b.id and b.name='" + colName + "' and a.type='u' and a.name='" + modelTableName + "'";
			if(template.queryForObject(testSql, Integer.class) == 0){
				if (!"".equals(tarSql.toString())) {
					logger.debug("tarSql: " + tarSql.toString());
					template.batchUpdate(tarSql.toString().split("\\;"));
				}
			}
		}}
	}
	
	private static synchronized void updateFieldSqlserver(Property property, Model model, JdbcTemplate template) {//含固有字段和自定义字段的修改
		String modelTableName = model.getTableName().toUpperCase();
		String modelName = model.getModelName().toUpperCase();
		if(property != null){
		String oldColName = property.getOrgColumnName().toUpperCase();
		String colName = property.getColumnName().toUpperCase();
		StringBuilder tarSql = new StringBuilder("");
		if (null != property && null != property.getIsIndex() && property.getIsIndex()) {
			String indexName = "IDX_" + modelName;
			if (property.getName().length() > 9) {
				indexName += "_" + property.getName().toUpperCase().substring(0, 8);
			} else {
				indexName += "_" + property.getName().toUpperCase();
			}
			if (!fieldIndexIsExist(modelTableName, indexName, template)) {
				tarSql.append("CREATE INDEX " + indexName + " ON " + modelTableName + "(" + colName + ");");
			} else {
				if (!ObjectUtils.isEmpty(oldColName)&& !oldColName.toUpperCase().equals(colName)) {
					tarSql.append("DROP INDEX " + indexName + " ON " + modelTableName + ";");
					tarSql.append("CREATE INDEX " + indexName + " ON " + modelTableName + "(" + colName + ");");
				}
			}
		}
		if (property.getType() == DbColumnType.TEXT || property.getType() == DbColumnType.BAPCODE) {
			Integer maxLength = property.getMaxLength();
			if (null != maxLength && maxLength > 0) {
				maxLength = maxLength * 2;
				if(maxLength > 4000){
					maxLength = 4000;
				}
				if (!ObjectUtils.isEmpty(oldColName)) {
					tarSql.append("ALTER TABLE " + modelTableName + " ALTER COLUMN " + oldColName + " varchar(" + maxLength + ");");
				}
			}
		}
		if (!ObjectUtils.isEmpty(oldColName) && !oldColName.toUpperCase().equals(colName)) {
			tarSql.append("EXEC sp_rename '" + modelTableName + "." + oldColName + "', '" + colName + "';");
		}
		if (property.getType() != DbColumnType.PROPERTYATTACHMENT) {
			if (!"".equals(tarSql.toString())) {
				logger.debug("tarSql: " + tarSql.toString());
				template.batchUpdate(tarSql.toString().split("\\;"));
			}
		}
		}
	}
	
	private static synchronized void createCustomFieldSqlserver(List<Property> propertyList, JdbcTemplate template) {
		if (null == propertyList || propertyList.size() <= 0) {
			return;
		}
		StringBuilder tarSql = new StringBuilder("");
		for (Property prop : propertyList) {
			if (null != prop.getIsCustom() && prop.getIsCustom()) {
				String modelTableName = prop.getModel().getTableName().toUpperCase();
				String colName = prop.getColumnName();
				String i18nFieldComment =  InternationalResource.get(prop.getDisplayName());
				if (prop.getType() == DbColumnType.TEXT) {
					tarSql.append("ALTER TABLE " + modelTableName + " ADD " + colName + " varchar(4000)" + ";");
				}
				if (prop.getType() == DbColumnType.INTEGER) {
					tarSql.append("ALTER TABLE " + modelTableName + " ADD " + colName + " int" + ";");
				}
				if (prop.getType() == DbColumnType.DECIMAL) {
					tarSql.append("ALTER TABLE " + modelTableName + " ADD " + colName + " numeric(19,6)" + ";");
				}
				if (prop.getType() == DbColumnType.DATETIME) {
					tarSql.append("ALTER TABLE " + modelTableName + " ADD " + colName + " datetime" + ";");
				}
				if (prop.getType() == DbColumnType.SYSTEMCODE) {
					tarSql.append("ALTER TABLE " + modelTableName + " ADD " + colName + " varchar(4000)" + ";");
				}
				if (prop.getType() == DbColumnType.OBJECT) {
					tarSql.append("ALTER TABLE " + modelTableName + " ADD " + colName + " numeric" + ";");
				}
			}
		}
		if (!"".equals(tarSql.toString())) {
			logger.debug("tarSql: " + tarSql.toString());
			template.batchUpdate(tarSql.toString().split("\\;"));
		}
	}


	/**
	 * mysql
	 */
	private static synchronized void createFieldMysql(Property property, Model model, JdbcTemplate template) {
		String modelTableName = model.getTableName().toUpperCase();
		if(property!=null){
			String colName = property.getColumnName();
			StringBuilder tarSql = new StringBuilder("");
			if (property.getType() == DbColumnType.TEXT) {//maxLength-update db
				Integer maxLength = property.getMaxLength();
				maxLength = (null != maxLength && maxLength > 0) ? maxLength * 2 : 510;
				tarSql.append("ALTER TABLE " + modelTableName + " ADD " + colName + " varchar(" + maxLength + ");");
			}
			if (property.getType() == DbColumnType.BAPCODE) {//maxLength-not update db
				tarSql.append("ALTER TABLE " + modelTableName + " ADD " + colName + " varchar(4000)" + ";");
			}
			if (property.getType() == DbColumnType.SUMMARY) {//maxLength-not update db
				tarSql.append("ALTER TABLE " + modelTableName + " ADD " + colName + " varchar(4000)" + ";");
			}
			if (property.getType() == DbColumnType.INTEGER) {
				tarSql.append("ALTER TABLE " + modelTableName + " ADD " + colName + " int" + ";");
			}
			if (property.getType() == DbColumnType.DECIMAL) {//decimalNum
				tarSql.append("ALTER TABLE " + modelTableName + " ADD " + colName + " decimal(19," + property.getDecimalNum() + ")" + ";");
			}
			if (property.getType() == DbColumnType.DATE) {
				tarSql.append("ALTER TABLE " + modelTableName + " ADD " + colName + " timestamp" + propertyNullable(property) + ";");
			}
			if (property.getType() == DbColumnType.TIME) {
				tarSql.append("ALTER TABLE " + modelTableName + " ADD " + colName + " timestamp" + propertyNullable(property) + ";");
			}
			if (property.getType() == DbColumnType.DATETIME) {
				tarSql.append("ALTER TABLE " + modelTableName + " ADD " + colName + " timestamp" + propertyNullable(property) + ";");
			}
			if (property.getType() == DbColumnType.BOOLEAN) {
				tarSql.append("ALTER TABLE " + modelTableName + " ADD " + colName + " int" + ";");
			}
			if (property.getType() == DbColumnType.LONGTEXT) {//maxLength-not update db
				tarSql.append("ALTER TABLE " + modelTableName + " ADD " + colName + " text" + ";");
			}
			if (property.getType() == DbColumnType.LONG) {
				tarSql.append("ALTER TABLE " + modelTableName + " ADD " + colName + " bigint" + ";");
			}
			if (property.getType() == DbColumnType.OBJECT) {	
				if(property.getAssociatedProperty().getType()== DbColumnType.LONG) {
					tarSql.append("ALTER TABLE " + modelTableName + " ADD " + colName + " bigint" + ";");
				}else if(property.getAssociatedProperty().getType()== DbColumnType.BAPCODE) {
					tarSql.append("ALTER TABLE " + modelTableName + " ADD " + colName + " varchar(4000)" + ";");
				}else {
					Integer maxLength = property.getAssociatedProperty().getMaxLength();
					maxLength = (null != maxLength && maxLength > 0) ? maxLength * 2 : 510;
					tarSql.append("ALTER TABLE " + modelTableName + " ADD " + colName + " varchar(" + maxLength + ");");
				}
			}
			if (property.getType() == DbColumnType.SYSTEMCODE) {
				tarSql.append("ALTER TABLE " + modelTableName + " ADD " + colName + " varchar(4000)" + ";");
			}
			if (property.getType() == DbColumnType.MONEY) {//decimalNum
				tarSql.append("ALTER TABLE " + modelTableName + " ADD " + colName + " decimal(19," + property.getDecimalNum() + ")"  + ";");
			}
			if (property.getType() == DbColumnType.PASSWORD) {//maxLength-not update db
				tarSql.append("ALTER TABLE " + modelTableName + " ADD " + colName + " varchar(510)" + ";");
			}
			if (property.getType() == DbColumnType.PICTURE) {
				tarSql.append("ALTER TABLE " + modelTableName + " ADD " + colName + " varchar(510)" + ";");
			}
			if (property.getType() == DbColumnType.PROPERTYATTACHMENT) {//not db
			}
			if (property.getType() == DbColumnType.OFFICE) {
				tarSql.append("ALTER TABLE " + modelTableName + " ADD " + colName + " text" + ";");
			}
			if (property.getType() == DbColumnType.TAGNUMBER) {
				tarSql.append("ALTER TABLE " + modelTableName + " ADD " + colName + " varchar(510)" + ";");
			}
			if (property.getType() != DbColumnType.PROPERTYATTACHMENT) {
				if (null != property && null != property.getIsIndex() && property.getIsIndex()) {
//					tarSql.append("CREATE INDEX IDX_" + model.getModelName() + "_" + colName + " ON " + modelTableName + "(" + colName + ");");
					tarSql.append("ALTER TABLE " + modelTableName + " ADD INDEX IDX_" + model.getModelName() + "_" + colName + "(" + colName + ");");
				}
				// 执行前判断，模型中列是否存在；如果存在则跳过
//				String testSql = "SELECT COUNT(*) FROM sysobjects a, syscolumns b WHERE a.id=b.id and b.name='" + colName + "' and a.type='u' and a.name='" + modelTableName + "'";
				String testSql = "SELECT count(1) from information_schema.columns t WHERE t.TABLE_SCHEMA= '"+DbUtils.getCurrentDBName()+"' and table_name='" + modelTableName + "' and column_name = '" + colName + "'";

				if(template.queryForObject(testSql, Integer.class) == 0){
					if (!"".equals(tarSql.toString())) {
						logger.debug("tarSql: " + tarSql.toString());
						template.batchUpdate(tarSql.toString().split("\\;"));
					}
				}
			}}
	}

	private static String propertyNullable(Property property) {
		return property.getNullable() != null && property.getNullable() ? " NULL" : "";
	}

	private static synchronized void updateFieldMysql(Property property, Model model, JdbcTemplate template) {//含固有字段和自定义字段的修改
		String modelTableName = model.getTableName().toUpperCase();
		String modelName = model.getModelName().toUpperCase();
		if(property != null){
			String oldColName = property.getOrgColumnName().toUpperCase();
			String colName = property.getColumnName().toUpperCase();
			StringBuilder tarSql = new StringBuilder("");
			if (null != property && null != property.getIsIndex() && property.getIsIndex()) {
				String indexName = "IDX_" + modelName;
				if (property.getName().length() > 9) {
					indexName += "_" + property.getName().toUpperCase().substring(0, 8);
				} else {
					indexName += "_" + property.getName().toUpperCase();
				}
				if (!fieldIndexIsExist(modelTableName, indexName, template)) {
					tarSql.append("CREATE INDEX " + indexName + " ON " + modelTableName + "(" + colName + ");");
				} else {
					if (!ObjectUtils.isEmpty(oldColName) && !oldColName.toUpperCase().equals(colName)) {
						tarSql.append("DROP INDEX " + indexName + " ON " + modelTableName + ";");
						tarSql.append("CREATE INDEX " + indexName + " ON " + modelTableName + "(" + colName + ");");
					}
				}
			}
			if (property.getType() == DbColumnType.TEXT || property.getType() == DbColumnType.BAPCODE) {
				Integer maxLength = property.getMaxLength();
				if (null != maxLength && maxLength > 0) {
					maxLength = maxLength * 2;
					if(maxLength > 4000){
						maxLength = 4000;
					}
//					tarSql.append("ALTER TABLE " + modelTableName + " ALTER COLUMN " + oldColName + " varchar(" + maxLength + ");");
					if (!ObjectUtils.isEmpty(oldColName)) {
						tarSql.append("ALTER TABLE " + modelTableName + " MODIFY COLUMN " + oldColName + " varchar(" + maxLength + ");");
					}
				}
			}
			if (!ObjectUtils.isEmpty(oldColName) && !oldColName.toUpperCase().equals(colName)) {
//				tarSql.append("EXEC sp_rename '" + modelTableName + "." + oldColName + "', '" + colName + "', 'COLUMN';");
				tarSql.append("ALTER TABLE " + modelTableName + " RENAME COLUMN " + oldColName + " TO " + colName + ";");
			}
			if (property.getType() == DbColumnType.DATE || property.getType() == DbColumnType.DATETIME || property.getType() == DbColumnType.TIME) {
				if (property.getNullable() != null && property.getNullable()) {
					tarSql.append("ALTER TABLE " + modelTableName + " MODIFY COLUMN " + colName + " timestamp NULL;");
				} else {
					tarSql.append("ALTER TABLE " + modelTableName + " MODIFY COLUMN " + colName + " timestamp NOT NULL;");
				}
			}
			if (property.getType() != DbColumnType.PROPERTYATTACHMENT) {
				if (!"".equals(tarSql.toString())) {
					logger.debug("tarSql: " + tarSql.toString());
					template.batchUpdate(tarSql.toString().split("\\;"));
				}
			}
		}
	}

	private static synchronized void createCustomFieldMysql(List<Property> propertyList, JdbcTemplate template) {
		if (null == propertyList || propertyList.size() <= 0) {
			return;
		}
		StringBuilder tarSql = new StringBuilder("");
		for (Property prop : propertyList) {
			if (null != prop.getIsCustom() && prop.getIsCustom()) {
				String modelTableName = prop.getModel().getTableName().toUpperCase();
				String colName = prop.getColumnName();
				String i18nFieldComment =  InternationalResource.get(prop.getDisplayName());
				if (prop.getType() == DbColumnType.TEXT) {
					tarSql.append("ALTER TABLE " + modelTableName + " ADD " + colName + " varchar(4000)" + ";");
				}
				if (prop.getType() == DbColumnType.INTEGER) {
					tarSql.append("ALTER TABLE " + modelTableName + " ADD " + colName + " int" + ";");
				}
				if (prop.getType() == DbColumnType.DECIMAL) {
					tarSql.append("ALTER TABLE " + modelTableName + " ADD " + colName + " decimal(19,6)" + ";");
				}
				if (prop.getType() == DbColumnType.DATETIME) {
					tarSql.append("ALTER TABLE " + modelTableName + " ADD " + colName + " timestamp" + ";");
				}
				if (prop.getType() == DbColumnType.SYSTEMCODE) {
					tarSql.append("ALTER TABLE " + modelTableName + " ADD " + colName + " varchar(4000)" + ";");
				}
				if (prop.getType() == DbColumnType.OBJECT) {
					tarSql.append("ALTER TABLE " + modelTableName + " ADD " + colName + " bigint" + ";");
				}
			}
		}
		if (!"".equals(tarSql.toString())) {
			logger.debug("tarSql: " + tarSql.toString());
			template.batchUpdate(tarSql.toString().split("\\;"));
		}
	}
	
}
