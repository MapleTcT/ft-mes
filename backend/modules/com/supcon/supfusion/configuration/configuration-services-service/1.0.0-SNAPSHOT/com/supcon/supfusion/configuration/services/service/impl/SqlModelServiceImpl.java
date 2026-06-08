package com.supcon.supfusion.configuration.services.service.impl;

import com.supcon.supfusion.base.services.BaseServiceImpl;
import com.supcon.supfusion.configuration.services.dao.SqlModelDaoImpl;
import com.supcon.supfusion.configuration.services.entity.*;
import com.supcon.supfusion.configuration.services.enums.DbColumnType;
import com.supcon.supfusion.configuration.services.enums.FieldType;
import com.supcon.supfusion.configuration.services.enums.JdbcType;
import com.supcon.supfusion.configuration.services.enums.ShowFormat;
import com.supcon.supfusion.configuration.services.exceptions.EcException;
import com.supcon.supfusion.configuration.services.service.ModelService;
import com.supcon.supfusion.configuration.services.service.PropertyKeyService;
import com.supcon.supfusion.configuration.services.service.PropertyService;
import com.supcon.supfusion.configuration.services.service.SqlModelService;
import com.supcon.supfusion.configuration.services.utils.*;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Transactional
@ServiceApiService
public class SqlModelServiceImpl extends BaseServiceImpl implements SqlModelService {

    @Autowired
    private JdbcTemplate template;
    @Autowired
    private PropertyKeyService propertyKeyService;
    @Autowired
    private ModelService modelService;
    @Autowired
    private PropertyService propertyService;
    @Autowired
    private SqlModelDaoImpl sqlModelDao;

    @Override
    public SqlModel getSqlModel(String modelCode) {
        List<SqlModel> list = sqlModelDao.findByHql("from SqlModel where code = ?0", modelCode);
        if (list != null && !list.isEmpty()) {
            return list.get(0);
        }
        return null;
    }

    @Override
    public void addSqlModel(Model model) {
        SqlModel sqlModel = model.getSqlModel();
        analysisSqlModel(sqlModel);
        modelService.saveModel(model);
        saveSqlProperties(model, sqlModel.getProperties());
        createDbView(sqlModel, model.getTableName(), model.getIsMain());
        saveSqlModel(model.getCode(), sqlModel);
    }

    private void saveSqlModel(String code, SqlModel sqlModel) {
        if (StringUtils.isEmpty(sqlModel.getCode())) {
            sqlModel.setCode(code);
        }
        sqlModelDao.save(sqlModel);
    }

    private void createDbView(SqlModel sqlModel, String tableName, boolean isMain) {
        setDbViewSql(sqlModel, tableName, isMain);
        String createViewSql = null;
        if ("sqlserver".equals(sqlModel.getCurrentDbType())) {
            deleteDBView(tableName);
            createViewSql = sqlModel.getSqlserverView();
        } else if ("oracle".equals(sqlModel.getCurrentDbType())) {
            createViewSql = sqlModel.getOracleView();
        } else if ("mariadb".equals(sqlModel.getCurrentDbType())) {
            createViewSql = sqlModel.getMariadbView();
        }
        try {
            template.execute(createViewSql);
        } catch (Exception e) {
            log.error("创建数据库视图失败，原因：" + e.getMessage(), e);
            throw new EcException(EcException.Code.CREATE_VIEW_ERROR);
        }
    }

    private void setDbViewSql(SqlModel sqlModel, String tableName, boolean isMain) {
        List<String> inherents = getInherentsFromProperties(sqlModel);
        if (!isMultiDb(sqlModel.getModelSql())) {
            sqlModel.setOracleView(getDbViewSql(sqlModel.getCurrentDbSql(), "oracle", tableName, inherents, isMain));
            sqlModel.setSqlserverView(getDbViewSql(sqlModel.getCurrentDbSql(), "sqlserver", tableName, inherents, isMain));
            sqlModel.setMariadbView(getDbViewSql(sqlModel.getCurrentDbSql(), "mariadb", tableName, inherents, isMain));
        } else {
            if (!StringUtils.isEmpty(sqlModel.getOracleSql())) {
                sqlModel.setOracleView(getDbViewSql(sqlModel.getOracleSql(), "oracle", tableName, inherents, isMain));
            }
            if (!StringUtils.isEmpty(sqlModel.getSqlserverSql())) {
                sqlModel.setSqlserverView(getDbViewSql(sqlModel.getSqlserverSql(), "sqlserver", tableName, inherents, isMain));
            }
            if (!StringUtils.isEmpty(sqlModel.getMariadbSql())) {
                sqlModel.setMariadbView(getDbViewSql(sqlModel.getMariadbSql(), "mariadb", tableName, inherents, isMain));
            }
        }
    }

    private String getDbViewSql(String sql, String DbType, String tableName, List<String> inherents, boolean isMain) {
        Set<String> inherentSet = null;
        if (isMain) {
            inherentSet = Inherent.inherentMainMap.keySet();
        } else {
            inherentSet = Inherent.inherentMap.keySet();
        }
        StringBuilder inherentSb = new StringBuilder();
        for (String inh : inherentSet) {
            if (inherents == null || inherents.isEmpty() || !inherents.contains(inh)) {
                if ("ID".equals(inh)) {
                    inherentSb.append("'1' as id,");
                } else if ("VALID".equals(inh)) {
                    inherentSb.append("'1' as valid,");
                } else if ("VERSION".equals(inh)) {
                    inherentSb.append("'0' as version,");
                } else if ("CID".equals(inh)) {
                    inherentSb.append(getCurrentCompanyId()).append(" as cid,");
                } else if ("STATUS".equals(inh)) {
                    inherentSb.append("'1' as status,");
                } else {
                    if ("sqlserver".equals(DbType)) {
                        inherentSb.append("convert(varchar(5),null) AS ").append(inh).append(",");
                    } else {
                        inherentSb.append("null as ").append(inh).append(",");
                    }
                }
            }
        }
        if ("sqlserver".equals(DbType)) {
            return "create view " + tableName + " as select " + inherentSb.toString() + " t.* from (" + sql + ") t";
        }
        return "create or replace view " + tableName + " as select " + inherentSb.toString() + " t.* from (" + sql + ") t";
    }

    private List<String> getInherentsFromProperties(SqlModel sqlModel) {
        List<String> inherents = new ArrayList<>();
        List<Property> properties = getPropertiesFromJson(sqlModel.getProperties());
        for (Property property : properties) {
            if (property.getIsInherent()) {
                inherents.add(property.getColumnName());
            }
        }
        return inherents;
    }

    private void saveSqlProperties(Model model, String properties) {
        List<Property> propertyList = getPropertiesFromJson(properties);
        if (null != propertyList && propertyList.size() > 0) {
            for (Property property : propertyList) {
                // 固有字段在创建模型时自动创建
//                String nameInherent = Inherent.getInherentByColumn(property.getColumnName(), model.getIsMain());
//                if (!StringUtils.isEmpty(nameInherent)) {
//                    continue;
//                }
                checkSqlProperty(property);
                if (!StringUtils.isEmpty(property.getCode())) {
                    Property p = propertyService.getProperty(property.getCode());
                    p.setDisplayName(property.getDisplayName());
                    p.setAssociatedProperty(property.getAssociatedProperty());
                    p.setAssociatedType(property.getAssociatedType());
                    p.setIsMainAssociated(property.getIsMainAssociated());
                    p.setFetchMode(property.getFetchMode());
                    p.setIsBussinessKey(p.getIsBussinessKey());
                    p.setIsMainDisplay(property.getIsMainDisplay());
                    p.setFillcontent(property.getFillcontent());
                    propertyService.save(p);
                } else {
                    property.setCode(model.getCode() + "_" + property.getName());
                    if (property.getIsInherent() && DbColumnType.OBJECT == property.getType()) {
                        property.setAssociatedProperty(propertyService.getProperty(Inherent.getAssPropertyCode(property.getName())));
                        property.setAssociatedType(1);
                    }
                    property.setCreateStaffId(getCurrentStaff().getId());
                    property.setIsUsedForList(true);
                    property.setFieldType(property.getType().getDefaultFieldType());
                    property.setFormat(property.getType().getDefaultShowFormat());
                    property.setModel(model);
                    property.setEntityCode(model.getEntity().getCode());
                    property.setModuleCode(model.getModuleCode());
                    propertyService.save(property);
                }
            }
        }
        List<Property> delProperties = new ArrayList<>();
        List<Property> pList = propertyService.getProperties(model.getCode());
        StringBuilder sb = new StringBuilder();
        for (Property p : pList) {
            if (!propertyList.contains(p)) { // 多余字段，需要删除
                Set<String> strings = modelService.checkDeleteProperty(p);
                if (strings != null && !strings.isEmpty()) {
                    for (String string : strings) {
                        sb.append("<li>").append("字段").append(p.getName()).append("存在依赖：").append(string).append("</li>");
                    }
                }
                delProperties.add(p);
            }
        }
        if (!StringUtils.isEmpty(sb.toString())) {
            throw new EcException("删除失败：" + sb.toString());
        }
        for (Property p : delProperties) {
            modelService.deletePropertyPhysical(p.getCode(), true);
        }
    }

    private List<Property> getPropertiesFromJson(String properties) {
        List<Property> propertyList = new ArrayList<>();
        try {
            JSONArray dataJson = new JSONArray(properties);
            for (int i = 0; i < dataJson.length(); i++) {
                JSONObject o = dataJson.getJSONObject(i);
                Property p = new Property();
                p.setCode(o.getString("code"));
                p.setColumnName(o.getString("columnName"));
                p.setName(o.getString("name"));
                p.setDisplayName(o.getString("displayName"));
                if (null != o.optString("type") && !"".equals(o.optString("type"))) {
                    p.setType(DbColumnType.valueOf(o.getString("type")));
                }
                if (null != o.optString("fieldType") && !"".equals(o.optString("fieldType"))) {
                    p.setFieldType(FieldType.valueOf(o.getString("fieldType")));
                }
                if (null != o.optString("format") && !"".equals(o.optString("format"))) {
                    p.setFormat(ShowFormat.valueOf(o.getString("format")));
                }
                if (null != o.optString("associatedProperty.code") && !"".equals(o.optString("associatedProperty.code"))) {
                    Property associatedProperty = new Property();
                    associatedProperty.setCode(o.getString("associatedProperty.code"));
                    p.setAssociatedProperty(associatedProperty);
                }
                if (null != o.optString("associatedType") && !"".equals(o.optString("associatedType"))) {
                    p.setAssociatedType(Integer.valueOf(o.getString("associatedType")));
                }
                if (null != o.optString("isMainAssociated") && !"".equals(o.optString("isMainAssociated"))) {
                    p.setIsMainAssociated(Boolean.valueOf(o.getString("isMainAssociated")));
                }
                if (null != o.optString("fetchMode") && !"".equals(o.optString("fetchMode"))) {
                    p.setFetchMode(o.getString("fetchMode"));
                }
                if (null != o.optString("fillcontent") && !"".equals(o.optString("fillcontent"))) {
                    p.setFillcontent(o.getString("fillcontent"));
                }
                if (null != o.optString("isBussinessKey") && !"".equals(o.optString("isBussinessKey"))) {
                    p.setIsBussinessKey(Boolean.valueOf(o.getString("isBussinessKey")));
                }
                if (null != o.optString("isMainDisplay") && !"".equals(o.optString("isMainDisplay"))) {
                    p.setIsMainDisplay(Boolean.valueOf(o.getString("isMainDisplay")));
                }
                if (null != o.optString("isInherent") && !"".equals(o.optString("isInherent"))) {
                    p.setIsInherent(Boolean.valueOf(o.getString("isInherent")));
                }
                propertyList.add(p);
            }
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }
        return propertyList;
    }

    @Override
    public List<Property> getProperties(Model model) {
        SqlModel sqlModel = model.getSqlModel();
        analysisSqlModel(sqlModel);
        List<Property> properties = new ArrayList<>(sqlModel.getDbColumns().size());
        for (SqlModelColumn column : sqlModel.getDbColumns()) {
            String name = null;
            DbColumnType dbColumnType = null;
            Property associatedProperty = null;
            // 判断是否固有字段
            String inherent = Inherent.getInherentByColumn(column.getColumnName(), model.getIsMain());
            if (!StringUtils.isEmpty(inherent)) {
                name = inherent;
                dbColumnType = Inherent.getInherentType(inherent);
                if (dbColumnType == DbColumnType.OBJECT) {
                    associatedProperty = modelService.getProperty(Inherent.getAssPropertyCode(name));
                }
            } else {
                name = getSqlPropertyCode(column.getColumnName());
                dbColumnType = JdbcType.forCode(column.getColumnType()).getDbColumnType();
            }
            String code = model.getCode() + "_" + name;
            Property property = modelService.getProperty(code);
            if (property == null) {
                property = new Property();
                property.setName(name);
                property.setType(dbColumnType);
                property.setAssociatedProperty(associatedProperty);
                property.setDisplayName(column.getColumnName());
                property.setColumnName(column.getColumnName());
                property.setIsInherent(!StringUtils.isEmpty(inherent));
            }
            properties.add(property);
        }
        return properties;
    }

    private void checkSqlProperty(Property p) {
        if(!propertyKeyService.checkJavaKey(p.getName()) || !propertyKeyService.checkDBKey(p.getName())) {
            throw new EcException(p.getName() + "ec.exception.KEY");
        }
        if (null != p.getType() && !p.getIsInherent() && DbColumnType.OBJECT == p.getType()) {
            if (!(null != p.getAssociatedProperty() && null != p.getAssociatedProperty().getCode() && !"".equals(p.getAssociatedProperty().getCode()))) {
                throw new EcException(EcException.Code.ASS_PROPERTY_NOT_SELECTED, p.getCode());
            }
        }
        if (p.getIsBussinessKey()) {
            if (DbColumnType.TEXT != p.getType() && DbColumnType.BAPCODE != p.getType()
                    && DbColumnType.INTEGER != p.getType() && DbColumnType.LONG != p.getType()) {
                throw new EcException(EcException.Code.CHECKBUSSINESSKEY);
            }
        }
        if (p.getIsMainDisplay()) {
            if (DbColumnType.TEXT != p.getType() && DbColumnType.BAPCODE != p.getType()
                    && DbColumnType.SUMMARY != p.getType() && DbColumnType.LONGTEXT != p.getType()
                    && DbColumnType.PASSWORD != p.getType()) {
                throw new EcException(EcException.Code.CHECKMAINDISPLAY);
            }
        }
    }

    @Override
    public void deleteDBView(String viewName) {
        try {
            String dropView = "DROP VIEW " + viewName;
            template.execute(dropView);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void checkSqlModel(Model model) {
        // 校验模型
        modelService.checkModelInfo(model);
        // 解析sql模型
        analysisSqlModel(model.getSqlModel());
        // 校验数据库列
        checkDbColumns(model);
    }

    @Override
    public void deleteSqlModel(String modelCode) {
        sqlModelDao.delete(modelCode);
    }

    public void analysisSqlModel(SqlModel sqlModel) {
        // 设置当前数据库信息
        String dbName = DbUtils.getDbName();
        if ("mysql".equals(dbName)) {
            dbName = "mariadb";
        }
        sqlModel.setCurrentDbType(dbName);
        // 设置多数据库sql以及当前数据库sql
        if (isMultiDb(sqlModel.getModelSql())) {
            setMultiDb(sqlModel);
        } else {
            sqlModel.setCurrentDbSql(sqlModel.getModelSql().replaceAll(";", ""));
        }
        //校验sql
        checkSql(sqlModel.getCurrentDbSql());
        // 设置数据库列信息
        sqlModel.setDbColumns(getColumnsBySql(sqlModel.getCurrentDbSql()));
    }

    private boolean isMultiDb(String modelSql) {
        String sqlLowerCase = modelSql.toLowerCase().replaceAll("\\s", "");
        if (sqlLowerCase.startsWith("--oracle") || sqlLowerCase.startsWith("--sqlserver") || sqlLowerCase.startsWith("--mariadb")) {
            return true;
        }
        return false;
    }

    private void setMultiDb(SqlModel sqlModel) {
        String[] split = sqlModel.getModelSql().substring(2).split("\n--");
        for (String s : split) {
            // 获取注释行，根据竖线分割
            String[] split2 = s.split("\n")[0].split("\\|");
            for (String s2 : split2) {
                String s3 = s2.trim().toLowerCase();
                String sql = s.substring(s2.length() + 1).trim().replaceAll(";", "");
                if ("oracle".equals(s3)) {
                    sqlModel.setOracleSql(sql);
                } else if ("sqlserver".equals(s3)) {
                    sqlModel.setSqlserverSql(sql);
                } else if ("mariadb".equals(s3)) {
                    sqlModel.setMariadbSql(sql);
                } else {
                    throw new EcException("不支持数据库类型：" + s2);
                }
            }
        }
        if ("oracle".equals(sqlModel.getCurrentDbType())) {
            sqlModel.setCurrentDbSql(sqlModel.getOracleSql());
        } else if ("sqlserver".equals(sqlModel.getCurrentDbType())) {
            sqlModel.setCurrentDbSql(sqlModel.getSqlserverSql());
        } else if ("mariadb".equals(sqlModel.getCurrentDbType())) {
            sqlModel.setCurrentDbSql(sqlModel.getMariadbSql());
        }
    }

    private void checkSql(String sql) {
        if (null == sql || "".equals(sql)) {
            throw new EcException(EcException.Code.SQLCANNOTNULL);
        }
        String sqlLower = sql.toLowerCase();
        if (sqlLower.contains("update\n") || sqlLower.contains("delete\n")
            || sqlLower.contains("insert\n") || sqlLower.contains("create\n")
            || sqlLower.contains("drop\n") || sqlLower.contains("alter\n")
            || sqlLower.contains("update ") || sqlLower.contains("delete ")
            || sqlLower.contains("insert ") || sqlLower.contains("create ")
            || sqlLower.contains("drop ") || sqlLower.contains("alter ")) {
            throw new EcException(EcException.Code.ONLYSELECT);
        }
        if (sqlLower.replaceAll("\\s", "").replaceAll("\n", "").contains("select*")) {
            throw new EcException(EcException.Code.CANNOTSELECTALL);
        }
    }

    private void checkDbColumns(Model model) {
        List<SqlModelColumn> columns = model.getSqlModel().getDbColumns();
        Map columnNameMap = new HashMap<String, String>();
        for (SqlModelColumn column : columns) {
            if (!checkDbColumnName(column.getColumnName())) {
                throw new EcException(EcException.Code.SQL_COlUMN_NAME_ERROR);
            }
            String nameInherent = Inherent.getInherentByColumn(column.getColumnName(), model.getIsMain());
            // 判断固有字段关键字
            if (!StringUtils.isEmpty(nameInherent)) {
                DbColumnType dbColumnType = JdbcType.forCode(column.getColumnType()).getDbColumnType();
                DbColumnType inherentType = Inherent.getInherentType(nameInherent);
                if (inherentType == DbColumnType.OBJECT) {
                    if (dbColumnType != DbColumnType.DECIMAL) {
                        throw new EcException(EcException.Code.TYPECANNOTINHERENT, column.getColumnName());
                    }
                } else if (inherentType == DbColumnType.LONG && dbColumnType != DbColumnType.DECIMAL) {
                    throw new EcException(EcException.Code.TYPECANNOTINHERENT, column.getColumnName());
                } else if (inherentType == DbColumnType.DATETIME && dbColumnType != DbColumnType.DATETIME) {
                    throw new EcException(EcException.Code.TYPECANNOTINHERENT, column.getColumnName());
                } else if (inherentType == DbColumnType.INTEGER && (dbColumnType != DbColumnType.DECIMAL && dbColumnType != DbColumnType.INTEGER)) {
                    throw new EcException(EcException.Code.TYPECANNOTINHERENT, column.getColumnName());
                } else if (inherentType == DbColumnType.LONGTEXT && dbColumnType != DbColumnType.LONGTEXT) {
                    throw new EcException(EcException.Code.TYPECANNOTINHERENT, column.getColumnName());
                } else if (inherentType == DbColumnType.TEXT && dbColumnType != DbColumnType.TEXT) {
                    throw new EcException(EcException.Code.TYPECANNOTINHERENT, column.getColumnName());
                }
            }
            String code = getSqlPropertyCode(column.getColumnName());
            if (!columnNameMap.containsKey(code)) {
                columnNameMap.put(code, column.getColumnName());
            } else {
                throw new EcException(EcException.Code.SQLCOLUMNREPEAT, code);
            }
        }
    }

    /**
     * 清除字段名称格式，去除标点符号、中文转全拼
     * @param columnName
     * @return
     */
    private String getSqlPropertyCode(String columnName) {
//        columnName = columnName.replaceAll("[\\pP\\p{Punct}]",""); // 清除标点、符号
//        columnName = columnName.replaceAll(" ", "");

        return lineToHump(columnName);
    }

    // 字母数字下划线组合
    static final Pattern p = Pattern.compile("^[A-Z|a-z]\\w*[A-Z|a-z|\\d]$");

    private boolean checkDbColumnName(String str) {
        Matcher m = p.matcher(str);
        if (m.find()) {
            return true;
        }
        return false;
    }

    /**
     * 获取sql执行后的列信息
     * @param sql
     * @return
     */
    private List<SqlModelColumn> getColumnsBySql(String sql) {
        List<SqlModelColumn> columns = new ArrayList<SqlModelColumn>();
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = template.getDataSource().getConnection();
            stmt = conn.prepareStatement(sql);
            ResultSetMetaData data = stmt.executeQuery().getMetaData();
            for(int i = 1 ; i<= data.getColumnCount() ; i++){
                SqlModelColumn column = new SqlModelColumn();
                // 列名，优先取别名
                String columnName = Optional.ofNullable(data.getColumnLabel(i)).orElse(data.getColumnName(i));
                column.setColumnName(columnName);
                column.setColumnType(data.getColumnType(i));
                column.setColumnTypeName(data.getColumnTypeName(i));
                column.setColumnDisplaySize(data.getColumnDisplaySize(i));
                columns.add(column);
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            throw new EcException(e.getMessage());
        } finally {
            if (null != stmt) {
                try {
                    stmt.close();
                } catch (SQLException e) {}
            }
            if (null != conn) {
                try {
                    conn.close();
                } catch (SQLException e) {}
            }
        }
        return columns;
    }

    private static Pattern linePattern = Pattern.compile("_(\\w)");

    /**
     * 下划线转驼峰
     * @param str
     * @return
     */
    private static String lineToHump(String str) {
        str = str.toLowerCase();
        Matcher matcher = linePattern.matcher(str);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, matcher.group(1).toUpperCase());
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
