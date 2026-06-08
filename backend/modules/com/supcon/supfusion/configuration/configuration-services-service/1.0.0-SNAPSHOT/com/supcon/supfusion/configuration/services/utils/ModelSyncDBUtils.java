package com.supcon.supfusion.configuration.services.utils;

import com.supcon.supfusion.configuration.services.entity.Entity;
import com.supcon.supfusion.configuration.services.entity.Model;
import com.supcon.supfusion.configuration.services.i18n.InternationalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 模型管理组态期同步更新数据库工具类
 * @author penghui
 */
public class ModelSyncDBUtils {
    private static final Logger logger = LoggerFactory.getLogger(ModelSyncDBUtils.class);

    public static synchronized void modelSyncToDb(Entity entity, Model model, boolean isNew, JdbcTemplate template, String dbName) {
        if (dbName.startsWith("oracle")) {
            if (isNew) {
                createModelTableAndInherentFields(entity, model, template, dbName);
                createDefaultTablesOfModel(entity, model, template, dbName);
            } else {
                updateModelAndDefaultTables(entity, model, template);
            }
        }else if (dbName.startsWith("sqlserver")) {
            if (isNew) {
                createModelTableAndInherentFieldsSqlserver(entity, model, template, dbName);
                createDefaultTablesOfModelSqlserver(entity, model, template, dbName);
            } else {
                updateModelAndDefaultTablesSqlserver(entity, model, template);
            }
        } else if (dbName.startsWith("mysql") || dbName.startsWith("mariadb")) {
            if (isNew) {
                createModelTableAndInherentFieldsMysql(entity, model, template, dbName);
                createDefaultTablesOfModelMysql(entity, model, template, dbName);
            } else {
                updateModelAndDefaultTablesMysql(entity, model, template);
            }
        }
    }

    public static synchronized void createMneCodeTableOfModel(JdbcTemplate template, Model model, String dbName) {//saveProperty invoke
        String tableName = model.getTableName().toUpperCase();
        String mneCodeSql = "";
        if (dbName.startsWith("oracle")) {
            mneCodeSql = createDefaultMCTableOfModel(tableName);
        }else if (dbName.startsWith("sqlserver")) {
            mneCodeSql = createDefaultMCTableOfModelSqlserver(tableName);
        }else if (dbName.startsWith("mysql") || dbName.startsWith("mariadb")) {
            mneCodeSql = createDefaultMCTableOfModelMysql(tableName);
        }
        if (!"".equals(mneCodeSql)) {
            template.batchUpdate(mneCodeSql.split("\\;"));
        }
    }

    private static synchronized void updateModelAndDefaultTables(Entity entity, Model model, JdbcTemplate template) {
        StringBuilder tarSql = new StringBuilder("");
        String oldTableName = model.getOrgTableName();
        String newTableName = model.getTableName();
        String newTableComment = model.getName();

        Boolean enableAcl = entity.getEnableAclRestrict();
        Boolean isGroupAstrict = entity.getGroupEnabled();
        Boolean isBaseEntity = entity.getIsBase();
        Boolean isAttention = entity.getPayCloseAttention();
        Boolean isMainModel = model.getIsMain();
        Boolean isMneCode = model.getIsMneCode();
        if (null != oldTableName && null != newTableName && !oldTableName.toUpperCase().equals(newTableName.toUpperCase())) {//修改表名
            tarSql.append("ALTER TABLE " + oldTableName.toUpperCase() + " RENAME TO " + newTableName.toUpperCase() + ";");
            if (isBaseEntity != null && !isBaseEntity && isMainModel) {//_DI
                tarSql.append("ALTER TABLE " + oldTableName.toUpperCase() + "_DI" + " RENAME TO " + newTableName.toUpperCase() + "_DI" + ";");
            }
            if (enableAcl) {//_ACL
                tarSql.append("ALTER TABLE " + oldTableName.toUpperCase() + "_ACL" + " RENAME TO " + newTableName.toUpperCase() + "_ACL" + ";");
            }
            if (isMneCode) {//_MC
                tarSql.append("ALTER TABLE " + oldTableName.toUpperCase() + "_MC" + " RENAME TO " + newTableName.toUpperCase() + "_MC" + ";");
            }
            if (!isBaseEntity && isAttention && isMainModel) {//_PA
                tarSql.append("ALTER TABLE " + oldTableName.toUpperCase() + "_PA" + " RENAME TO " + newTableName.toUpperCase() + "_PA" + ";");
            }
            if (!isBaseEntity && isMainModel) {//_SV
                tarSql.append("ALTER TABLE " + oldTableName.toUpperCase() + "_SV" + " RENAME TO " + newTableName.toUpperCase() + "_SV" + ";");
            }
            if (!isBaseEntity && isGroupAstrict) {//_GI
                tarSql.append("ALTER TABLE " + oldTableName.toUpperCase() + "_GI" + " RENAME TO " + newTableName.toUpperCase() + "_GI" + ";");
            }
        }
        if (null != newTableComment && !"".equals(newTableComment)) {//修改表注释
            tarSql.append("COMMENT ON TABLE " + newTableName.toUpperCase() + " IS '" + InternationalResource.get(newTableComment) + "';");
        }
        Boolean bExtraCol = model.getIsExtraCol();
        if (null != bExtraCol && bExtraCol.booleanValue()) {//新增大字段
            if (!extraColFieldIsExist(oldTableName)) {
                tarSql.append("ALTER TABLE " + newTableName.toUpperCase() + " ADD (EXTRA_COL CLOB" + ");");
                tarSql.append("COMMENT ON COLUMN " + newTableName.toUpperCase() + ".EXTRA_COL IS '大字段';");
            }
        }
        if (!"".equals(tarSql.toString())) {
            logger.debug("tarSql: " + tarSql.toString());
            template.batchUpdate(tarSql.toString().split("\\;"));
        }
    }

    private static boolean extraColFieldIsExist(String mTableName) {
        boolean extraColIsExist = false;
        try {
            extraColIsExist = DbUtils.getConnection().getMetaData().getColumns(null, null, mTableName.toUpperCase(), "EXTRA_COL").next();
        } catch (Exception e) {
            extraColIsExist = false;
            logger.error(e.getMessage(), e);
        }
        return extraColIsExist;
    }

    private static synchronized void createModelTableAndInherentFields(Entity entity, Model model, JdbcTemplate template, String dbName) {
        String tableName = model.getTableName().toUpperCase();
        if (!checkTableIsExist(tableName, template, dbName)) {
            Integer dataType = model.getDataType();
            StringBuilder tarSql = new StringBuilder("");
            if (null == dataType || dataType.intValue() == 1) {// 普通数据类型
                if (!model.getIsMain()) {//辅模型
                    tarSql.append("CREATE TABLE " + tableName + "(");
                    tarSql.append("ID NUMBER(19,0),");
                    tarSql.append("VERSION NUMBER(*,0) DEFAULT 0,");
                    tarSql.append("DELETE_TIME TIMESTAMP (6), ");
                    tarSql.append("MODIFY_TIME TIMESTAMP (6),");
                    tarSql.append("CREATE_TIME TIMESTAMP (6),");
                    tarSql.append("DELETE_STAFF_ID NUMBER(19,0),");
                    tarSql.append("MODIFY_STAFF_ID NUMBER(19,0),");
                    tarSql.append("CREATE_STAFF_ID NUMBER(19,0),");
                    tarSql.append("VALID NUMBER(*,0) DEFAULT 1,");
                    tarSql.append("CID NUMBER(19,0),");
                    tarSql.append("SORT NUMBER(*,0),");
                    if(!entity.getIsBase()) {
                        tarSql.append("TABLE_INFO_ID NUMBER(19,0),");
                    }
                    Boolean bExtraCol = model.getIsExtraCol();
                    if (null != bExtraCol && bExtraCol.booleanValue()) {
                        tarSql.append("EXTRA_COL CLOB,");
                    }
                    tarSql.append("PRIMARY KEY (ID));");
                    tarSql.append("COMMENT ON TABLE " + tableName + " IS '" + InternationalResource.get(model.getName()) + "';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".VERSION IS '版本信息';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".DELETE_TIME IS '删除时间';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".MODIFY_TIME IS '修改时间';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".CREATE_TIME IS '创建时间';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".DELETE_STAFF_ID IS '删除者';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".MODIFY_STAFF_ID IS '修改者';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".CREATE_STAFF_ID IS '创建者';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".VALID IS '是否有效';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".CID IS '公司ID';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".SORT IS '顺序';");
                    if(!entity.getIsBase()) {
                        tarSql.append("COMMENT ON COLUMN " + tableName + ".TABLE_INFO_ID IS '表单ID';");
                    }
                    if (null != bExtraCol && bExtraCol.booleanValue()) {
                        tarSql.append("COMMENT ON COLUMN " + tableName + ".EXTRA_COL IS '大字段';");
                    }
                } else {//主模型
                    tarSql.append("CREATE TABLE " + tableName + "(");
                    tarSql.append("ID NUMBER(19,0),");
                    tarSql.append("VERSION NUMBER(*,0) DEFAULT 0,");
                    tarSql.append("DELETE_TIME TIMESTAMP (6), ");
                    tarSql.append("MODIFY_TIME TIMESTAMP (6),");
                    tarSql.append("CREATE_TIME TIMESTAMP (6),");
                    tarSql.append("DELETE_STAFF_ID NUMBER(19,0),");
                    tarSql.append("MODIFY_STAFF_ID NUMBER(19,0),");
                    tarSql.append("CREATE_STAFF_ID NUMBER(19,0),");
                    tarSql.append("VALID NUMBER(*,0) DEFAULT 1,");
                    tarSql.append("CID NUMBER(19,0),");
                    tarSql.append("SORT NUMBER(*,0),");//
                    tarSql.append("EFFECTIVE_STATE NUMBER(19,0),");
                    tarSql.append("PROCESS_VERSION NUMBER(*,0),");
                    tarSql.append("PROCESS_KEY VARCHAR2(510 CHAR),");
                    tarSql.append("DEPLOYMENT_ID NUMBER(19,0),");
                    tarSql.append("GROUP_ID NUMBER(19,0),");
                    tarSql.append("STATUS NUMBER(19,0),");
                    tarSql.append("EFFECT_TIME TIMESTAMP (6),");
                    tarSql.append("EFFECT_STAFF_ID NUMBER(19,0),");
                    tarSql.append("OWNER_DEPARTMENT_ID NUMBER(19,0),");
                    tarSql.append("OWNER_POSITION_ID NUMBER(19,0),");
                    tarSql.append("OWNER_STAFF_ID NUMBER(19,0),");
                    tarSql.append("POSITION_LAY_REC VARCHAR2(510 CHAR),");
                    tarSql.append("CREATE_POSITION_ID NUMBER(19,0),");
                    tarSql.append("CREATE_DEPARTMENT_ID NUMBER(19,0),");
                    if(!entity.getIsBase()){
                        tarSql.append("TABLE_NO VARCHAR2(510 CHAR),");
                        tarSql.append("TABLE_INFO_ID NUMBER(19,0),");//
                    }
                    Boolean bExtraCol = model.getIsExtraCol();
                    if (null != bExtraCol && bExtraCol.booleanValue()) {
                        tarSql.append("EXTRA_COL CLOB,");
                    }
                    tarSql.append("PRIMARY KEY (ID));");
                    tarSql.append("COMMENT ON TABLE " + tableName + " IS '" + InternationalResource.get(model.getName()) + "';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".VERSION IS '版本信息';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".DELETE_TIME IS '删除时间';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".MODIFY_TIME IS '修改时间';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".CREATE_TIME IS '创建时间';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".DELETE_STAFF_ID IS '删除者';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".MODIFY_STAFF_ID IS '修改者';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".CREATE_STAFF_ID IS '创建者';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".VALID IS '是否有效';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".CID IS '公司ID';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".SORT IS '顺序';");//
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".EFFECTIVE_STATE IS '生效状态';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".PROCESS_VERSION IS '流程版本';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".PROCESS_KEY IS '流程key';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".DEPLOYMENT_ID IS '流程ID';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".GROUP_ID IS '组ID';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".STATUS IS '状态';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".EFFECT_TIME IS '生效时间';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".EFFECT_STAFF_ID IS '生效人';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".OWNER_DEPARTMENT_ID IS '所有者部门';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".OWNER_POSITION_ID IS '所有者主岗';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".OWNER_STAFF_ID IS '所有者';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".POSITION_LAY_REC IS '岗位层级结构';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".CREATE_POSITION_ID IS '创建岗位';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".CREATE_DEPARTMENT_ID IS '创建部门';");
                    if(!entity.getIsBase()){
                        tarSql.append("COMMENT ON COLUMN " + tableName + ".TABLE_NO IS '单据编号';");
                        tarSql.append("COMMENT ON COLUMN " + tableName + ".TABLE_INFO_ID IS '表单ID';");//
                    }
                    if (null != bExtraCol && bExtraCol.booleanValue()) {
                        tarSql.append("COMMENT ON COLUMN " + tableName + ".EXTRA_COL IS '大字段';");
                    }
                }
            } else {// 树形数据类型
                if (!model.getIsMain()) {//辅模型
                    tarSql.append("CREATE TABLE " + tableName + "(");
                    tarSql.append("ID NUMBER(19,0),");
                    tarSql.append("VERSION NUMBER(*,0) DEFAULT 0,");
                    tarSql.append("DELETE_TIME TIMESTAMP (6), ");
                    tarSql.append("MODIFY_TIME TIMESTAMP (6),");
                    tarSql.append("CREATE_TIME TIMESTAMP (6),");
                    tarSql.append("DELETE_STAFF_ID NUMBER(19,0),");
                    tarSql.append("MODIFY_STAFF_ID NUMBER(19,0),");
                    tarSql.append("CREATE_STAFF_ID NUMBER(19,0),");
                    tarSql.append("VALID NUMBER(*,0) DEFAULT 1,");
                    tarSql.append("CID NUMBER(19,0),");//
                    tarSql.append("LEAF NUMBER(*,0),");
                    tarSql.append("FULL_PATH_NAME VARCHAR2(4000 CHAR),");
                    tarSql.append("PARENT_ID VARCHAR2(510 CHAR),");
                    tarSql.append("SORT NUMBER(19,0),");//
                    tarSql.append("LAY_NO NUMBER(*,0),");
                    tarSql.append("LAY_REC VARCHAR2(4000 CHAR),");
                    if(!entity.getIsBase()) {
                        tarSql.append("TABLE_INFO_ID NUMBER(19,0),");//
                    }
                    Boolean bExtraCol = model.getIsExtraCol();
                    if (null != bExtraCol && bExtraCol.booleanValue()) {
                        tarSql.append("EXTRA_COL CLOB,");
                    }
                    tarSql.append("PRIMARY KEY (ID));");
                    tarSql.append("COMMENT ON TABLE " + tableName + " IS '" + InternationalResource.get(model.getName()) + "';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".VERSION IS '版本信息';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".DELETE_TIME IS '删除时间';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".MODIFY_TIME IS '修改时间';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".CREATE_TIME IS '创建时间';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".DELETE_STAFF_ID IS '删除者';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".MODIFY_STAFF_ID IS '修改者';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".CREATE_STAFF_ID IS '创建者';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".VALID IS '是否有效';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".CID IS '公司ID';");//
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".LEAF IS '是否叶子';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".FULL_PATH_NAME IS '层级全路径';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".PARENT_ID IS '上级节点ID';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".SORT IS '顺序';");//
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".LAY_NO IS '层级';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".LAY_REC IS '层级结构';");
                    if(!entity.getIsBase()) {
                        tarSql.append("COMMENT ON COLUMN " + tableName + ".TABLE_INFO_ID IS '表单ID';");//
                    }
                    if (null != bExtraCol && bExtraCol.booleanValue()) {
                        tarSql.append("COMMENT ON COLUMN " + tableName + ".EXTRA_COL IS '大字段';");
                    }
                } else {//主模型
                    tarSql.append("CREATE TABLE " + tableName + "(");
                    tarSql.append("ID NUMBER(19,0),");
                    tarSql.append("VERSION NUMBER(*,0) DEFAULT 0,");
                    tarSql.append("DELETE_TIME TIMESTAMP (6), ");
                    tarSql.append("MODIFY_TIME TIMESTAMP (6),");
                    tarSql.append("CREATE_TIME TIMESTAMP (6),");
                    tarSql.append("DELETE_STAFF_ID NUMBER(19,0),");
                    tarSql.append("MODIFY_STAFF_ID NUMBER(19,0),");
                    tarSql.append("CREATE_STAFF_ID NUMBER(19,0),");
                    tarSql.append("VALID NUMBER(*,0) DEFAULT 1,");
                    tarSql.append("CID NUMBER(19,0),");//
                    tarSql.append("LEAF NUMBER(*,0),");
                    tarSql.append("FULL_PATH_NAME VARCHAR2(4000 CHAR),");
                    tarSql.append("PARENT_ID VARCHAR2(510 CHAR),");
                    tarSql.append("SORT NUMBER(19,0),");//
                    tarSql.append("LAY_NO NUMBER(*,0),");
                    tarSql.append("LAY_REC VARCHAR2(4000 CHAR),");
                    tarSql.append("EFFECTIVE_STATE NUMBER(19,0),");//
                    tarSql.append("PROCESS_VERSION NUMBER(*,0),");
                    tarSql.append("PROCESS_KEY VARCHAR2(510 CHAR),");
                    tarSql.append("DEPLOYMENT_ID NUMBER(19,0),");
                    tarSql.append("STATUS NUMBER(19,0),");
                    tarSql.append("EFFECT_TIME TIMESTAMP (6),");
                    tarSql.append("EFFECT_STAFF_ID NUMBER(19,0),");
                    tarSql.append("OWNER_DEPARTMENT_ID NUMBER(19,0),");
                    tarSql.append("OWNER_POSITION_ID NUMBER(19,0),");
                    tarSql.append("OWNER_STAFF_ID NUMBER(19,0),");
                    tarSql.append("POSITION_LAY_REC VARCHAR2(510 CHAR),");

                    tarSql.append("CREATE_POSITION_ID NUMBER(19,0),");
                    tarSql.append("CREATE_DEPARTMENT_ID NUMBER(19,0),");//
                    tarSql.append("OA CLOB,");
                    if(!entity.getIsBase()){
                        tarSql.append("TABLE_NO VARCHAR2(510 CHAR),");
                        tarSql.append("TABLE_INFO_ID NUMBER(19,0),");//
                    }
                    Boolean bExtraCol = model.getIsExtraCol();
                    if (null != bExtraCol && bExtraCol.booleanValue()) {
                        tarSql.append("EXTRA_COL CLOB,");
                    }
                    tarSql.append("PRIMARY KEY (ID));");
                    tarSql.append("COMMENT ON TABLE " + tableName + " IS '" + InternationalResource.get(model.getName()) + "';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".VERSION IS '版本信息';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".DELETE_TIME IS '删除时间';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".MODIFY_TIME IS '修改时间';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".CREATE_TIME IS '创建时间';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".DELETE_STAFF_ID IS '删除者';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".MODIFY_STAFF_ID IS '修改者';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".CREATE_STAFF_ID IS '创建者';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".VALID IS '是否有效';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".CID IS '公司ID';");//
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".LEAF IS '是否叶子';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".FULL_PATH_NAME IS '层级全路径';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".PARENT_ID IS '上级节点ID';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".SORT IS '顺序';");//
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".LAY_NO IS '层级';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".LAY_REC IS '层级结构';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".EFFECTIVE_STATE IS '生效状态';");//
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".PROCESS_VERSION IS '流程版本';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".PROCESS_KEY IS '流程key';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".DEPLOYMENT_ID IS '流程ID';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".STATUS IS '状态';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".EFFECT_TIME IS '生效时间';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".EFFECT_STAFF_ID IS '生效人';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".OWNER_DEPARTMENT_ID IS '所有者部门';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".OWNER_POSITION_ID IS '所有者主岗';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".OWNER_STAFF_ID IS '所有者';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".POSITION_LAY_REC IS '岗位层级结构';");

                    tarSql.append("COMMENT ON COLUMN " + tableName + ".CREATE_POSITION_ID IS '创建岗位';");
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".CREATE_DEPARTMENT_ID IS '创建部门';");//
                    tarSql.append("COMMENT ON COLUMN " + tableName + ".OA IS 'OA字段';");
                    if(!entity.getIsBase()){
                        tarSql.append("COMMENT ON COLUMN " + tableName + ".TABLE_NO IS '单据编号';");
                        tarSql.append("COMMENT ON COLUMN " + tableName + ".TABLE_INFO_ID IS '表单ID';");//
                    }
                    if (null != bExtraCol && bExtraCol.booleanValue()) {
                        tarSql.append("COMMENT ON COLUMN " + tableName + ".EXTRA_COL IS '大字段';");
                    }
                }
            }
            if (!"".equals(tarSql.toString())) {
                logger.debug("tarSql: " + tarSql.toString());
                template.batchUpdate(tarSql.toString().split("\\;"));
            }
        }
    }

    private static synchronized void createDefaultTablesOfModel(Entity entity, Model model, JdbcTemplate template, String dbName) {
        String modelTableName = model.getTableName().toUpperCase();
        Boolean enableAcl = entity.getEnableAclRestrict();
        Boolean isGroupAstrict = entity.getGroupEnabled();
        Boolean isBaseEntity = entity.getIsBase();
        Boolean isAttention = entity.getPayCloseAttention();
        Boolean isMainModel = model.getIsMain();
        Boolean isMneCode = model.getIsMneCode();
        StringBuilder tarSql = new StringBuilder("");
        String tableName = "";
        if (isBaseEntity != null && !isBaseEntity && isMainModel) { //_DI
            tableName = modelTableName + "_DI";
            tarSql.append("CREATE TABLE " + tableName + "(");
            tarSql.append("ID NUMBER(19,0),");
            tarSql.append("VERSION NUMBER(*,0) DEFAULT 0,");
            tarSql.append("SIGNATURE VARCHAR2(800 CHAR),");
            tarSql.append("PENDING_CREATE_TIME TIMESTAMP (6),");
            tarSql.append("DEALINFO_TYPE VARCHAR2(510 CHAR),");
            tarSql.append("PROXY_STAFF_IDS VARCHAR2(510 CHAR),");
            tarSql.append("PROXY_STAFF VARCHAR2(510 CHAR),");
            tarSql.append("ASSIGN_STAFF_ID VARCHAR2(510 CHAR),");
            tarSql.append("ASSIGN_STAFF VARCHAR2(4000 CHAR),");
            tarSql.append("PROCESS_VERSION NUMBER(*,0),");
            tarSql.append("PROCESS_KEY VARCHAR2(510 CHAR),");
            tarSql.append("TASK_DESCRIPTION VARCHAR2(510 CHAR),");
            tarSql.append("ACTIVITY_NAME VARCHAR2(510 CHAR),");
            tarSql.append("OUTCOME_DES VARCHAR2(510 CHAR),");
            tarSql.append("OUTCOME VARCHAR2(510 CHAR),");
            tarSql.append("CREATE_TIME TIMESTAMP (6),");
            tarSql.append("ENTITY_CODE VARCHAR2(510 CHAR),");
            tarSql.append("INSTANCE_ID VARCHAR2(510 CHAR),");
            tarSql.append("USER_ID NUMBER(19,0),");
            tarSql.append("COMMENTS VARCHAR2(4000 CHAR),");
            tarSql.append("CID NUMBER(19,0),");
            tarSql.append("TABLE_INFO_ID NUMBER(19,0),");
            tarSql.append("USER_AGENT VARCHAR2(510 CHAR),");
            tarSql.append("RECALLED_FLAG NUMBER(*,0),");
            tarSql.append("STAFF NUMBER(19,0),");
            tarSql.append("MAIN_OBJ NUMBER(19,0),");
            tarSql.append("SORT NUMBER(*,0),");
            tarSql.append("PRIMARY KEY (ID));");
            //tarSql.append("COMMENT ON TABLE " + tableName + " IS '" + InternationalResource.get(model.getName() + "_DI表") + "';");
            tarSql.append("COMMENT ON COLUMN " + tableName + ".VERSION IS '版本信息';");
            tarSql.append("COMMENT ON COLUMN " + tableName + ".SIGNATURE IS '签名';");
            tarSql.append("COMMENT ON COLUMN " + tableName + ".PENDING_CREATE_TIME IS '待办创建时间';");
            tarSql.append("COMMENT ON COLUMN " + tableName + ".DEALINFO_TYPE IS '处理意见类型';");
            tarSql.append("COMMENT ON COLUMN " + tableName + ".PROXY_STAFF_IDS IS '委托人ID';");
            tarSql.append("COMMENT ON COLUMN " + tableName + ".PROXY_STAFF IS '委托人';");
            tarSql.append("COMMENT ON COLUMN " + tableName + ".ASSIGN_STAFF_ID IS '指定人员ID';");
            tarSql.append("COMMENT ON COLUMN " + tableName + ".ASSIGN_STAFF IS '指定人员';");
            tarSql.append("COMMENT ON COLUMN " + tableName + ".PROCESS_VERSION IS '流程版本';");
            tarSql.append("COMMENT ON COLUMN " + tableName + ".PROCESS_KEY IS '流程key';");
            tarSql.append("COMMENT ON COLUMN " + tableName + ".TASK_DESCRIPTION IS '活动描述';");
            tarSql.append("COMMENT ON COLUMN " + tableName + ".ACTIVITY_NAME IS '活动名称，在一个流程内唯一';");
            tarSql.append("COMMENT ON COLUMN " + tableName + ".OUTCOME_DES IS '出迁移线描述';");
            tarSql.append("COMMENT ON COLUMN " + tableName + ".OUTCOME IS '出迁移线';");
            tarSql.append("COMMENT ON COLUMN " + tableName + ".CREATE_TIME IS '创建时间';");
            tarSql.append("COMMENT ON COLUMN " + tableName + ".ENTITY_CODE IS '实体CODE';");
            tarSql.append("COMMENT ON COLUMN " + tableName + ".INSTANCE_ID IS '流程实例ID, 索引字段';");
            tarSql.append("COMMENT ON COLUMN " + tableName + ".USER_ID IS '处理人';");
            tarSql.append("COMMENT ON COLUMN " + tableName + ".COMMENTS IS '处理意见';");
            tarSql.append("COMMENT ON COLUMN " + tableName + ".CID IS '修改时间';");
            tarSql.append("COMMENT ON COLUMN " + tableName + ".TABLE_INFO_ID IS '表单ID';");
			/*tarSql.append("COMMENT ON COLUMN " + tableName + ".USER_AGENT IS '';");
			tarSql.append("COMMENT ON COLUMN " + tableName + ".RECALLED_FLAG IS '';");
			tarSql.append("COMMENT ON COLUMN " + tableName + ".STAFF IS '';");
			tarSql.append("COMMENT ON COLUMN " + tableName + ".MAIN_OBJ IS '';");
			tarSql.append("COMMENT ON COLUMN " + tableName + ".SORT IS '';");*/
        }
        if (enableAcl) {//_ACL
            tableName = modelTableName + "_ACL";
            tarSql.append("CREATE TABLE " + tableName + "(");
            tarSql.append("ID NUMBER(19,0),");
            tarSql.append("VERSION NUMBER(*,0) DEFAULT 0,");
            tarSql.append("OBJECT_ID NUMBER(19,0),");
            tarSql.append("SID_TYPE VARCHAR2(510 CHAR),");
            tarSql.append("PERMISSION VARCHAR2(510 CHAR),");
            tarSql.append("SID NUMBER(19,0),");
            tarSql.append("PRIMARY KEY (ID));");
            //tarSql.append("COMMENT ON TABLE " + tableName + " IS '" + InternationalResource.get(model.getName() + "_ACL表") + "';");
            tarSql.append("COMMENT ON COLUMN " + tableName + ".VERSION IS '版本信息';");
			/*tarSql.append("COMMENT ON COLUMN " + tableName + ".OBJECT_ID IS '';");
			tarSql.append("COMMENT ON COLUMN " + tableName + ".SID_TYPE IS '';");
			tarSql.append("COMMENT ON COLUMN " + tableName + ".PERMISSION IS '';");
			tarSql.append("COMMENT ON COLUMN " + tableName + ".SID IS '';");*/
        }
        if (isMneCode) {//_MC field enable mneCode-isUsedMneCode
            String mneCodeSql = createDefaultMCTableOfModel(modelTableName);
            tarSql.append(mneCodeSql);
        }
        if (!isBaseEntity && isAttention && isMainModel) {//_PA
            tableName = modelTableName + "_PA";
            tarSql.append("CREATE TABLE " + tableName + "(");
            tarSql.append("ID NUMBER(19,0),");
            tarSql.append("VERSION NUMBER(*,0) DEFAULT 0,");
            tarSql.append("DELETE_TIME TIMESTAMP (6), ");
            tarSql.append("MODIFY_TIME TIMESTAMP (6),");
            tarSql.append("CREATE_TIME TIMESTAMP (6),");
            tarSql.append("DELETE_STAFF_ID NUMBER(19,0),");
            tarSql.append("MODIFY_STAFF_ID NUMBER(19,0),");
            tarSql.append("CREATE_STAFF_ID NUMBER(19,0),");
            tarSql.append("TABLE_INFO_ID NUMBER(19,0),");
            tarSql.append("STAFF NUMBER(19,0),");
            tarSql.append("MAIN_OBJ NUMBER(19,0),");
            tarSql.append("VALID NUMBER(*,0) DEFAULT 1,");
            tarSql.append("PRIMARY KEY (ID));");
            //tarSql.append("COMMENT ON TABLE " + tableName + " IS '" + InternationalResource.get(model.getName() + "_PA表") + "';");
            tarSql.append("COMMENT ON COLUMN " + tableName + ".VERSION IS '版本信息';");
            tarSql.append("COMMENT ON COLUMN " + tableName + ".DELETE_TIME IS '删除时间';");
            tarSql.append("COMMENT ON COLUMN " + tableName + ".MODIFY_TIME IS '修改时间';");
            tarSql.append("COMMENT ON COLUMN " + tableName + ".CREATE_TIME IS '创建时间';");
            tarSql.append("COMMENT ON COLUMN " + tableName + ".DELETE_STAFF_ID IS '删除者';");
            tarSql.append("COMMENT ON COLUMN " + tableName + ".MODIFY_STAFF_ID IS '修改者';");
            tarSql.append("COMMENT ON COLUMN " + tableName + ".CREATE_STAFF_ID IS '创建者';");
            tarSql.append("COMMENT ON COLUMN " + tableName + ".TABLE_INFO_ID IS '表单ID';");
			/*tarSql.append("COMMENT ON COLUMN " + tableName + ".STAFF IS '';");
			tarSql.append("COMMENT ON COLUMN " + tableName + ".MAIN_OBJ IS '';");*/
            tarSql.append("COMMENT ON COLUMN " + tableName + ".VALID IS '是否有效';");
        }
        if (!isBaseEntity && isMainModel) {//_SV
            tableName = modelTableName + "_SV";
            tarSql.append("CREATE TABLE " + tableName + "(");
            tarSql.append("ID NUMBER(19,0),");
            tarSql.append("VERSION NUMBER(*,0) DEFAULT 0,");
            tarSql.append("DELETE_TIME TIMESTAMP (6), ");
            tarSql.append("MODIFY_TIME TIMESTAMP (6),");
            tarSql.append("CREATE_TIME TIMESTAMP (6),");
            tarSql.append("DELETE_STAFF_ID NUMBER(19,0),");
            tarSql.append("MODIFY_STAFF_ID NUMBER(19,0),");
            tarSql.append("CREATE_STAFF_ID NUMBER(19,0),");
            tarSql.append("TABLE_INFO_ID NUMBER(19,0),");
            tarSql.append("STAFF NUMBER(19,0),");
            tarSql.append("MAIN_OBJ NUMBER(19,0),");
            tarSql.append("VALID NUMBER(*,0) DEFAULT 1,");
            tarSql.append("PRIMARY KEY (ID));");
            //tarSql.append("COMMENT ON TABLE " + tableName + " IS '" + InternationalResource.get(model.getName() + "_SV表") + "';");
            tarSql.append("COMMENT ON COLUMN " + tableName + ".VERSION IS '版本信息';");
            tarSql.append("COMMENT ON COLUMN " + tableName + ".DELETE_TIME IS '删除时间';");
            tarSql.append("COMMENT ON COLUMN " + tableName + ".MODIFY_TIME IS '修改时间';");
            tarSql.append("COMMENT ON COLUMN " + tableName + ".CREATE_TIME IS '创建时间';");
            tarSql.append("COMMENT ON COLUMN " + tableName + ".DELETE_STAFF_ID IS '删除者';");
            tarSql.append("COMMENT ON COLUMN " + tableName + ".MODIFY_STAFF_ID IS '修改者';");
            tarSql.append("COMMENT ON COLUMN " + tableName + ".CREATE_STAFF_ID IS '创建者';");
            tarSql.append("COMMENT ON COLUMN " + tableName + ".TABLE_INFO_ID IS '表单ID';");
			/*tarSql.append("COMMENT ON COLUMN " + tableName + ".STAFF IS '';");
			tarSql.append("COMMENT ON COLUMN " + tableName + ".MAIN_OBJ IS '';");*/
            tarSql.append("COMMENT ON COLUMN " + tableName + ".VALID IS '是否有效';");
        }
        if (!isBaseEntity && isGroupAstrict) {//_GI
            tableName = modelTableName + "_GI";
            tarSql.append("CREATE TABLE " + tableName + "(");
            tarSql.append("ID NUMBER(19,0),");
            tarSql.append("VERSION NUMBER(*,0) DEFAULT 0,");
            tarSql.append("GROUP_ID NUMBER(19,0),");
            tarSql.append("TABLE_INFO_ID NUMBER(19,0),");
            tarSql.append("ENTITY_CODE VARCHAR2(510 CHAR),");
            tarSql.append("PRIMARY KEY (ID));");
            //tarSql.append("COMMENT ON TABLE " + tableName + " IS '" + InternationalResource.get(model.getName() + "_GI表") + "';");
            tarSql.append("COMMENT ON COLUMN " + tableName + ".VERSION IS '版本信息';");
            tarSql.append("COMMENT ON COLUMN " + tableName + ".GROUP_ID IS '组ID';");
            tarSql.append("COMMENT ON COLUMN " + tableName + ".TABLE_INFO_ID IS '单据TableInfo';");
            tarSql.append("COMMENT ON COLUMN " + tableName + ".ENTITY_CODE IS '实体code';");
        }
        if (!"".equals(tarSql.toString()) && !checkTableIsExist(tableName, template, dbName)) {
            logger.debug("tarSql: " + tarSql.toString());
            template.batchUpdate(tarSql.toString().split("\\;"));
        }
    }

    private static synchronized String createDefaultMCTableOfModel(String modelTableName) {
        String tableName = modelTableName + "_MC";
        StringBuilder tarSql = new StringBuilder("");
        tarSql.append("CREATE TABLE " + tableName + "(");
        tarSql.append("ID NUMBER(19,0),");
        tarSql.append("VERSION NUMBER(*,0) DEFAULT 0,");
        tarSql.append(modelTableName + " NUMBER(19,0),");
        tarSql.append("MNE_CODE VARCHAR2(510 CHAR),");

        tarSql.append("PRIMARY KEY (ID));");
        //tarSql.append("COMMENT ON TABLE " + tableName + " IS '" + InternationalResource.get(model.getName() + "_MC表") + "';");
        tarSql.append("COMMENT ON COLUMN " + tableName + ".VERSION IS '版本信息';");
		/*tarSql.append("COMMENT ON COLUMN " + tableName + "." + modelTableName + " IS '';");
		tarSql.append("COMMENT ON COLUMN " + tableName + ".MNE_CODE IS '';");*/
        return tarSql.toString();
    }



    //----------------------sqlserver------------------------------
    private static synchronized void createModelTableAndInherentFieldsSqlserver(Entity entity, Model model, JdbcTemplate template, String dbName) {
        String tableName = model.getTableName().toUpperCase();
        if (!checkTableIsExist(tableName, template, dbName)) {
            Integer dataType = model.getDataType();
            StringBuilder tarSql = new StringBuilder("");
            if (null == dataType || dataType.intValue() == 1) {// 普通数据类型
                if (!model.getIsMain()) {//辅模型
                    tarSql.append("CREATE TABLE " + tableName + "(");
                    tarSql.append("ID numeric NOT NULL,");
                    tarSql.append("VERSION int DEFAULT ((0)),");
                    tarSql.append("DELETE_TIME datetime,");
                    tarSql.append("MODIFY_TIME datetime,");
                    tarSql.append("CREATE_TIME datetime,");
                    tarSql.append("DELETE_STAFF_ID numeric,");
                    tarSql.append("MODIFY_STAFF_ID numeric,");
                    tarSql.append("CREATE_STAFF_ID numeric,");
                    tarSql.append("VALID tinyint DEFAULT ((1)),");
                    tarSql.append("CID numeric,");
                    tarSql.append("SORT int,");
                    if(!entity.getIsBase()) {
                        tarSql.append("TABLE_INFO_ID numeric,");
                    }
                    Boolean bExtraCol = model.getIsExtraCol();
                    if (null != bExtraCol && bExtraCol.booleanValue()) {
                        tarSql.append("EXTRA_COL text,");
                    }
                    tarSql.append("PRIMARY KEY (ID));");
                } else {//主模型
                    tarSql.append("CREATE TABLE " + tableName + "(");
                    tarSql.append("ID numeric NOT NULL,");
                    tarSql.append("VERSION int DEFAULT ((0)),");
                    tarSql.append("DELETE_TIME datetime, ");
                    tarSql.append("MODIFY_TIME datetime,");
                    tarSql.append("CREATE_TIME datetime,");
                    tarSql.append("DELETE_STAFF_ID numeric,");
                    tarSql.append("MODIFY_STAFF_ID numeric,");
                    tarSql.append("CREATE_STAFF_ID numeric,");
                    tarSql.append("VALID tinyint DEFAULT ((1)),");
                    tarSql.append("CID numeric,");
                    tarSql.append("SORT int,");//
                    tarSql.append("EFFECTIVE_STATE numeric,");
                    tarSql.append("PROCESS_VERSION int,");
                    tarSql.append("PROCESS_KEY varchar(510),");
                    tarSql.append("DEPLOYMENT_ID numeric,");
                    tarSql.append("GROUP_ID numeric,");
                    tarSql.append("STATUS numeric,");
                    tarSql.append("EFFECT_TIME datetime,");
                    tarSql.append("EFFECT_STAFF_ID numeric,");
                    tarSql.append("OWNER_DEPARTMENT_ID numeric,");
                    tarSql.append("OWNER_POSITION_ID numeric,");
                    tarSql.append("OWNER_STAFF_ID numeric,");
                    tarSql.append("POSITION_LAY_REC varchar(510),");
                    tarSql.append("CREATE_POSITION_ID numeric,");
                    tarSql.append("CREATE_DEPARTMENT_ID numeric,");
                    if(!entity.getIsBase()){
                        tarSql.append("TABLE_NO varchar(510),");
                        tarSql.append("TABLE_INFO_ID numeric,");//
                    }
                    Boolean bExtraCol = model.getIsExtraCol();
                    if (null != bExtraCol && bExtraCol.booleanValue()) {
                        tarSql.append("EXTRA_COL text,");
                    }
                    tarSql.append("PRIMARY KEY (ID));");
                }
            } else {// 树形数据类型
                if (!model.getIsMain()) {//辅模型
                    tarSql.append("CREATE TABLE " + tableName + "(");
                    tarSql.append("ID numeric NOT NULL,");
                    tarSql.append("VERSION int DEFAULT ((0)),");
                    tarSql.append("DELETE_TIME datetime, ");
                    tarSql.append("MODIFY_TIME datetime,");
                    tarSql.append("CREATE_TIME datetime,");
                    tarSql.append("DELETE_STAFF_ID numeric,");
                    tarSql.append("MODIFY_STAFF_ID numeric,");
                    tarSql.append("CREATE_STAFF_ID numeric,");
                    tarSql.append("VALID tinyint DEFAULT ((1)),");
                    tarSql.append("CID numeric,");//
                    tarSql.append("LEAF tinyint,");
                    tarSql.append("FULL_PATH_NAME varchar(4000),");
                    tarSql.append("PARENT_ID varchar(510),");
                    tarSql.append("SORT numeric,");//
                    tarSql.append("LAY_NO int,");
                    tarSql.append("LAY_REC varchar(4000),");
                    if(!entity.getIsBase()){
                        tarSql.append("TABLE_INFO_ID numeric,");//
                    }
                    Boolean bExtraCol = model.getIsExtraCol();
                    if (null != bExtraCol && bExtraCol.booleanValue()) {
                        tarSql.append("EXTRA_COL text,");
                    }
                    tarSql.append("PRIMARY KEY (ID));");
                } else {//主模型
                    tarSql.append("CREATE TABLE " + tableName + "(");
                    tarSql.append("ID numeric NOT NULL,");
                    tarSql.append("VERSION int DEFAULT ((0)),");
                    tarSql.append("DELETE_TIME datetime, ");
                    tarSql.append("MODIFY_TIME datetime,");
                    tarSql.append("CREATE_TIME datetime,");
                    tarSql.append("DELETE_STAFF_ID numeric,");
                    tarSql.append("MODIFY_STAFF_ID numeric,");
                    tarSql.append("CREATE_STAFF_ID numeric,");
                    tarSql.append("VALID tinyint DEFAULT ((1)),");
                    tarSql.append("CID numeric,");//
                    tarSql.append("LEAF tinyint,");
                    tarSql.append("FULL_PATH_NAME varchar(4000),");
                    tarSql.append("PARENT_ID varchar(510),");
                    tarSql.append("SORT numeric,");//
                    tarSql.append("LAY_NO int,");
                    tarSql.append("LAY_REC varchar(4000),");
                    tarSql.append("EFFECTIVE_STATE numeric,");//
                    tarSql.append("PROCESS_VERSION int,");
                    tarSql.append("PROCESS_KEY varchar(510),");
                    tarSql.append("DEPLOYMENT_ID numeric,");
                    tarSql.append("STATUS numeric,");
                    tarSql.append("EFFECT_TIME datetime,");
                    tarSql.append("EFFECT_STAFF_ID numeric,");
                    tarSql.append("OWNER_DEPARTMENT_ID numeric,");
                    tarSql.append("OWNER_POSITION_ID numeric,");
                    tarSql.append("OWNER_STAFF_ID numeric,");
                    tarSql.append("POSITION_LAY_REC varchar(510),");

                    tarSql.append("CREATE_POSITION_ID numeric,");
                    tarSql.append("CREATE_DEPARTMENT_ID numeric,");//
                    tarSql.append("OA text,");
                    if(!entity.getIsBase()){
                        tarSql.append("TABLE_NO varchar(510),");
                        tarSql.append("TABLE_INFO_ID numeric,");//
                    }
                    Boolean bExtraCol = model.getIsExtraCol();
                    if (null != bExtraCol && bExtraCol.booleanValue()) {
                        tarSql.append("EXTRA_COL text,");
                    }
                    tarSql.append("PRIMARY KEY (ID));");
                }
            }
            if (!"".equals(tarSql.toString())) {
                logger.debug("tarSql: " + tarSql.toString());
                template.batchUpdate(tarSql.toString().split("\\;"));
            }
        }
    }

    private static synchronized void createDefaultTablesOfModelSqlserver(Entity entity, Model model, JdbcTemplate template, String dbName) {
        String modelTableName = model.getTableName().toUpperCase();
        Boolean enableAcl = entity.getEnableAclRestrict();
        Boolean isGroupAstrict = entity.getGroupEnabled();
        Boolean isBaseEntity = entity.getIsBase();
        Boolean isAttention = entity.getPayCloseAttention();
        Boolean isMainModel = model.getIsMain();
        Boolean isMneCode = model.getIsMneCode();
        StringBuilder tarSql = new StringBuilder("");
        String tableName = "";
        if (isBaseEntity != null && !isBaseEntity && isMainModel) {//_DI
            tableName = modelTableName + "_DI";
            tarSql.append("CREATE TABLE " + tableName + "(");
            tarSql.append("ID numeric NOT NULL,");
            tarSql.append("VERSION int DEFAULT ((0)),");
            tarSql.append("SIGNATURE varchar(800),");
            tarSql.append("PENDING_CREATE_TIME datetime,");
            tarSql.append("DEALINFO_TYPE varchar(510),");
            tarSql.append("PROXY_STAFF_IDS varchar(510),");
            tarSql.append("PROXY_STAFF varchar(510),");
            tarSql.append("ASSIGN_STAFF_ID varchar(510),");
            tarSql.append("ASSIGN_STAFF varchar(4000),");
            tarSql.append("PROCESS_VERSION int,");
            tarSql.append("PROCESS_KEY varchar(510),");
            tarSql.append("TASK_DESCRIPTION varchar(510),");
            tarSql.append("ACTIVITY_NAME varchar(510),");
            tarSql.append("OUTCOME_DES varchar(510),");
            tarSql.append("OUTCOME varchar(510),");
            tarSql.append("CREATE_TIME datetime,");
            tarSql.append("ENTITY_CODE varchar(510),");
            tarSql.append("INSTANCE_ID varchar(510),");
            tarSql.append("USER_ID numeric,");
            tarSql.append("COMMENTS varchar(4000),");
            tarSql.append("CID numeric,");
            tarSql.append("TABLE_INFO_ID numeric,");
            tarSql.append("USER_AGENT varchar(510),");
            tarSql.append("RECALLED_FLAG tinyint,");
            tarSql.append("STAFF numeric,");
            tarSql.append("MAIN_OBJ numeric,");
            tarSql.append("SORT int,");
            tarSql.append("PRIMARY KEY (ID));");
        }
        if (enableAcl) {//_ACL
            tableName = modelTableName + "_ACL";
            tarSql.append("CREATE TABLE " + tableName + "(");
            tarSql.append("ID numeric NOT NULL,");
            tarSql.append("VERSION int DEFAULT ((0)),");
            tarSql.append("OBJECT_ID numeric,");
            tarSql.append("SID_TYPE varchar(510),");
            tarSql.append("PERMISSION varchar(510),");
            tarSql.append("SID numeric,");
            tarSql.append("PRIMARY KEY (ID));");
        }
        if (isMneCode) {
            //_MC field enable mneCode-isUsedMneCode
            String mneCodeSql = createDefaultMCTableOfModelSqlserver(modelTableName);
            tarSql.append(mneCodeSql);
        }
        if (!isBaseEntity && isAttention && isMainModel) {
            tableName = modelTableName + "_PA";
            tarSql.append("CREATE TABLE " + tableName + "(");
            tarSql.append("ID numeric NOT NULL,");
            tarSql.append("VERSION int DEFAULT ((0)),");
            tarSql.append("DELETE_TIME datetime, ");
            tarSql.append("MODIFY_TIME datetime,");
            tarSql.append("CREATE_TIME datetime,");
            tarSql.append("DELETE_STAFF_ID numeric,");
            tarSql.append("MODIFY_STAFF_ID numeric,");
            tarSql.append("CREATE_STAFF_ID numeric,");
            tarSql.append("TABLE_INFO_ID numeric,");
            tarSql.append("STAFF numeric,");
            tarSql.append("MAIN_OBJ numeric,");
            tarSql.append("VALID tinyint DEFAULT ((1)),");
            tarSql.append("PRIMARY KEY (ID));");
        }
        if (!isBaseEntity && isMainModel) {//_SV
            tableName = modelTableName + "_SV";
            tarSql.append("CREATE TABLE " + tableName + "(");
            tarSql.append("ID numeric NOT NULL,");
            tarSql.append("VERSION int DEFAULT ((0)),");
            tarSql.append("DELETE_TIME datetime, ");
            tarSql.append("MODIFY_TIME datetime,");
            tarSql.append("CREATE_TIME datetime,");
            tarSql.append("DELETE_STAFF_ID numeric,");
            tarSql.append("MODIFY_STAFF_ID numeric,");
            tarSql.append("CREATE_STAFF_ID numeric,");
            tarSql.append("TABLE_INFO_ID numeric,");
            tarSql.append("STAFF numeric,");
            tarSql.append("MAIN_OBJ numeric,");
            tarSql.append("VALID tinyint DEFAULT ((1)),");
            tarSql.append("PRIMARY KEY (ID));");
        }
        if (!isBaseEntity && isGroupAstrict) {//_GI
            tableName = modelTableName + "_GI";
            tarSql.append("CREATE TABLE " + tableName + "(");
            tarSql.append("ID numeric NOT NULL,");
            tarSql.append("VERSION int DEFAULT ((0)),");
            tarSql.append("GROUP_ID numeric,");
            tarSql.append("TABLE_INFO_ID numeric,");
            tarSql.append("ENTITY_CODE varchar(510),");
            tarSql.append("PRIMARY KEY (ID));");
        }
        if (!"".equals(tarSql.toString()) && !checkTableIsExist(tableName, template, dbName)) {
            logger.debug("tarSql: " + tarSql.toString());
            template.batchUpdate(tarSql.toString().split("\\;"));
        }
    }

    private static synchronized void updateModelAndDefaultTablesSqlserver(Entity entity, Model model, JdbcTemplate template) {
        StringBuilder tarSql = new StringBuilder("");
        String oldTableName = model.getOrgTableName();
        String newTableName = model.getTableName();
        String newTableComment = model.getName();

        Boolean enableAcl = entity.getEnableAclRestrict();
        Boolean isGroupAstrict = entity.getGroupEnabled();
        Boolean isBaseEntity = entity.getIsBase();
        Boolean isAttention = entity.getPayCloseAttention();
        Boolean isMainModel = model.getIsMain();
        Boolean isMneCode = model.getIsMneCode();
        if (null != oldTableName && null != newTableName && !oldTableName.toUpperCase().equals(newTableName.toUpperCase())) {//修改表名
            String preStrSql = "EXEC sp_rename '";
            tarSql.append(preStrSql + oldTableName.toUpperCase() + "', '" + newTableName.toUpperCase() + "';");
            if (isBaseEntity != null && !isBaseEntity && isMainModel) {//_DI
                tarSql.append(preStrSql + oldTableName.toUpperCase() + "_DI" + "', '" + newTableName.toUpperCase() + "_DI" + "';");
            }
            if (enableAcl) {//_ACL
                tarSql.append(preStrSql + oldTableName.toUpperCase() + "_ACL" + "', '" + newTableName.toUpperCase() + "_ACL" + "';");
            }
            if (isMneCode) {//_MC
                tarSql.append(preStrSql + oldTableName.toUpperCase() + "_MC" + "', '" + newTableName.toUpperCase() + "_MC" + "';");
            }
            if (!isBaseEntity && isAttention && isMainModel) {//_PA
                tarSql.append(preStrSql + oldTableName.toUpperCase() + "_PA" + "', '" + newTableName.toUpperCase() + "_PA" + "';");
            }
            if (!isBaseEntity && isMainModel) {//_SV
                tarSql.append(preStrSql + oldTableName.toUpperCase() + "_SV" + "', '" + newTableName.toUpperCase() + "_SV" + "';");
            }
            if (!isBaseEntity && isGroupAstrict) {//_GI
                tarSql.append(preStrSql + oldTableName.toUpperCase() + "_GI" + "', '" + newTableName.toUpperCase() + "_GI" + "';");
            }
        }
        Boolean bExtraCol = model.getIsExtraCol();
        if (null != bExtraCol && bExtraCol.booleanValue()) {//新增大字段
            if (!extraColFieldIsExist(oldTableName)) {
                tarSql.append("ALTER TABLE " + newTableName.toUpperCase() + " ADD EXTRA_COL text" + ";");
            }
        }
        if (!"".equals(tarSql.toString())) {
            logger.debug("tarSql: " + tarSql.toString());
            template.batchUpdate(tarSql.toString().split("\\;"));
        }
    }

    private static synchronized String createDefaultMCTableOfModelSqlserver(String modelTableName) {
        String tableName = modelTableName + "_MC";
        StringBuilder tarSql = new StringBuilder("");
        tarSql.append("CREATE TABLE " + tableName + "(");
        tarSql.append("ID numeric NOT NULL,");
        tarSql.append("VERSION int DEFAULT ((0)),");
        tarSql.append(modelTableName + " numeric,");
        tarSql.append("MNE_CODE varchar(510),");

        tarSql.append("PRIMARY KEY (ID));");
        return tarSql.toString();
    }

    private static String createDefaultMCTableOfModelMysql(String modelTableName) {
        String tableName = modelTableName + "_MC";
        StringBuilder tarSql = new StringBuilder("");
        tarSql.append("CREATE TABLE " + tableName + "(");
        tarSql.append("ID bigint NOT NULL,");
        tarSql.append("VERSION int DEFAULT ((0)),");
        tarSql.append(modelTableName + " bigint,");
        tarSql.append("MNE_CODE varchar(510),");

        tarSql.append("PRIMARY KEY (ID));");
        return tarSql.toString();
    }

    private static synchronized boolean checkTableIsExist(String tableName, JdbcTemplate template, String dbName) {
        boolean isExist = false;
        String sql = "";
        if (dbName.startsWith("oracle")) {
            sql = "SELECT COUNT(1) FROM ALL_TABLES WHERE TABLE_NAME='" + tableName + "'";
        } else if (dbName.startsWith("sqlserver")) {
            sql = "SELECT COUNT(1) FROM SYSOBJECTS WHERE NAME = '" + tableName + "' AND TYPE = 'U'";
        } else if (dbName.startsWith("mysql") || dbName.startsWith("mariadb")) {
            sql = "select count(1) from information_schema.tables t where t.TABLE_SCHEMA='"+ DbUtils.getCurrentDBName() +"' and table_name ='" + tableName + "'";
        }
        int resSql = template.queryForObject(sql, Integer.class);
        isExist = ((resSql >= 1) ? true : false);
        return isExist;
    }

    private static void createModelTableAndInherentFieldsMysql(Entity entity, Model model, JdbcTemplate template, String dbName) {
        String tableName = model.getTableName().toUpperCase();
        if (!checkTableIsExist(tableName, template, dbName)) {
            Integer dataType = model.getDataType();
            StringBuilder tarSql = new StringBuilder("");
            if (null == dataType || dataType.intValue() == 1) {// 普通数据类型
                if (!model.getIsMain()) {//辅模型
                    tarSql.append("CREATE TABLE " + tableName + "(");
                    tarSql.append("ID bigint NOT NULL,");
                    tarSql.append("VERSION int DEFAULT ((0)),");
                    tarSql.append("DELETE_TIME timestamp,");
                    tarSql.append("MODIFY_TIME timestamp,");
                    tarSql.append("CREATE_TIME timestamp,");
                    tarSql.append("DELETE_STAFF_ID bigint,");
                    tarSql.append("MODIFY_STAFF_ID bigint,");
                    tarSql.append("CREATE_STAFF_ID bigint,");
                    tarSql.append("VALID int DEFAULT ((1)),");
                    tarSql.append("CID bigint,");
                    tarSql.append("SORT int,");
                    if(!entity.getIsBase()) {
                        tarSql.append("TABLE_INFO_ID bigint,");
                    }
                    Boolean bExtraCol = model.getIsExtraCol();
                    if (null != bExtraCol && bExtraCol.booleanValue()) {
                        tarSql.append("EXTRA_COL text,");
                    }
                    tarSql.append("PRIMARY KEY (ID));");
                } else {//主模型
                    tarSql.append("CREATE TABLE " + tableName + "(");
                    tarSql.append("ID bigint NOT NULL,");
                    tarSql.append("VERSION int DEFAULT ((0)),");
                    tarSql.append("DELETE_TIME timestamp, ");
                    tarSql.append("MODIFY_TIME timestamp,");
                    tarSql.append("CREATE_TIME timestamp,");
                    tarSql.append("DELETE_STAFF_ID bigint,");
                    tarSql.append("MODIFY_STAFF_ID bigint,");
                    tarSql.append("CREATE_STAFF_ID bigint,");
                    tarSql.append("VALID tinyint DEFAULT ((1)),");
                    tarSql.append("CID bigint,");
                    tarSql.append("SORT int,");//
                    tarSql.append("EFFECTIVE_STATE bigint,");
                    tarSql.append("PROCESS_VERSION int,");
                    tarSql.append("PROCESS_KEY varchar(510),");
                    tarSql.append("DEPLOYMENT_ID bigint,");
                    tarSql.append("GROUP_ID bigint,");
                    tarSql.append("STATUS bigint,");
                    tarSql.append("EFFECT_TIME timestamp,");
                    tarSql.append("EFFECT_STAFF_ID bigint,");
                    tarSql.append("OWNER_DEPARTMENT_ID bigint,");
                    tarSql.append("OWNER_POSITION_ID bigint,");
                    tarSql.append("OWNER_STAFF_ID bigint,");
                    tarSql.append("POSITION_LAY_REC varchar(510),");
                    tarSql.append("CREATE_POSITION_ID bigint,");
                    tarSql.append("CREATE_DEPARTMENT_ID bigint,");
                    if(!entity.getIsBase()){
                        tarSql.append("TABLE_NO varchar(510),");
                        tarSql.append("TABLE_INFO_ID bigint,");//
                    }
                    Boolean bExtraCol = model.getIsExtraCol();
                    if (null != bExtraCol && bExtraCol.booleanValue()) {
                        tarSql.append("EXTRA_COL text,");
                    }
                    tarSql.append("PRIMARY KEY (ID));");
                }
            } else {// 树形数据类型
                if (!model.getIsMain()) {//辅模型
                    tarSql.append("CREATE TABLE " + tableName + "(");
                    tarSql.append("ID bigint NOT NULL,");
                    tarSql.append("VERSION int DEFAULT ((0)),");
                    tarSql.append("DELETE_TIME timestamp, ");
                    tarSql.append("MODIFY_TIME timestamp,");
                    tarSql.append("CREATE_TIME timestamp,");
                    tarSql.append("DELETE_STAFF_ID bigint,");
                    tarSql.append("MODIFY_STAFF_ID bigint,");
                    tarSql.append("CREATE_STAFF_ID bigint,");
                    tarSql.append("VALID tinyint DEFAULT ((1)),");
                    tarSql.append("CID bigint,");//
                    tarSql.append("LEAF tinyint,");
                    tarSql.append("FULL_PATH_NAME varchar(4000),");
                    tarSql.append("PARENT_ID varchar(510),");
                    tarSql.append("SORT bigint,");//
                    tarSql.append("LAY_NO int,");
                    tarSql.append("LAY_REC varchar(4000),");
                    if(!entity.getIsBase()){
                        tarSql.append("TABLE_INFO_ID bigint,");//
                    }
                    Boolean bExtraCol = model.getIsExtraCol();
                    if (null != bExtraCol && bExtraCol.booleanValue()) {
                        tarSql.append("EXTRA_COL text,");
                    }
                    tarSql.append("PRIMARY KEY (ID));");
                } else {//主模型
                    tarSql.append("CREATE TABLE " + tableName + "(");
                    tarSql.append("ID bigint NOT NULL,");
                    tarSql.append("VERSION int DEFAULT ((0)),");
                    tarSql.append("DELETE_TIME timestamp, ");
                    tarSql.append("MODIFY_TIME timestamp,");
                    tarSql.append("CREATE_TIME timestamp,");
                    tarSql.append("DELETE_STAFF_ID bigint,");
                    tarSql.append("MODIFY_STAFF_ID bigint,");
                    tarSql.append("CREATE_STAFF_ID bigint,");
                    tarSql.append("VALID tinyint DEFAULT ((1)),");
                    tarSql.append("CID bigint,");//
                    tarSql.append("LEAF tinyint,");
                    tarSql.append("FULL_PATH_NAME varchar(4000),");
                    tarSql.append("PARENT_ID varchar(510),");
                    tarSql.append("SORT bigint,");//
                    tarSql.append("LAY_NO int,");
                    tarSql.append("LAY_REC varchar(4000),");
                    tarSql.append("EFFECTIVE_STATE bigint,");//
                    tarSql.append("PROCESS_VERSION int,");
                    tarSql.append("PROCESS_KEY varchar(510),");
                    tarSql.append("DEPLOYMENT_ID bigint,");
                    tarSql.append("STATUS bigint,");
                    tarSql.append("EFFECT_TIME datetime,");
                    tarSql.append("EFFECT_STAFF_ID bigint,");
                    tarSql.append("OWNER_DEPARTMENT_ID bigint,");
                    tarSql.append("OWNER_POSITION_ID bigint,");
                    tarSql.append("OWNER_STAFF_ID bigint,");
                    tarSql.append("POSITION_LAY_REC varchar(510),");

                    tarSql.append("CREATE_POSITION_ID bigint,");
                    tarSql.append("CREATE_DEPARTMENT_ID bigint,");//
                    tarSql.append("OA text,");
                    if(!entity.getIsBase()){
                        tarSql.append("TABLE_NO varchar(510),");
                        tarSql.append("TABLE_INFO_ID bigint,");//
                    }
                    Boolean bExtraCol = model.getIsExtraCol();
                    if (null != bExtraCol && bExtraCol.booleanValue()) {
                        tarSql.append("EXTRA_COL text,");
                    }
                    tarSql.append("PRIMARY KEY (ID));");
                }
            }
            if (!"".equals(tarSql.toString())) {
                logger.info("tarSql: " + tarSql.toString());
                template.batchUpdate(tarSql.toString().split("\\;"));
            }
        }
    }

    private static void createDefaultTablesOfModelMysql(Entity entity, Model model, JdbcTemplate template, String dbName) {
        String modelTableName = model.getTableName().toUpperCase();
        Boolean enableAcl = entity.getEnableAclRestrict();
        Boolean isGroupAstrict = entity.getGroupEnabled();
        Boolean isBaseEntity = entity.getIsBase();
        Boolean isAttention = entity.getPayCloseAttention();
        Boolean isMainModel = model.getIsMain();
        Boolean isMneCode = model.getIsMneCode();
        StringBuilder tarSql = new StringBuilder("");
        String tableName = "";
        if (isBaseEntity != null && !isBaseEntity && isMainModel) {//_DI
            tableName = modelTableName + "_DI";
            tarSql.append("CREATE TABLE " + tableName + "(");
            tarSql.append("ID bigint NOT NULL,");
            tarSql.append("VERSION int DEFAULT ((0)),");
            tarSql.append("SIGNATURE varchar(800),");
            tarSql.append("PENDING_CREATE_TIME timestamp,");
            tarSql.append("DEALINFO_TYPE varchar(510),");
            tarSql.append("PROXY_STAFF_IDS varchar(510),");
            tarSql.append("PROXY_STAFF varchar(510),");
            tarSql.append("ASSIGN_STAFF_ID varchar(510),");
            tarSql.append("ASSIGN_STAFF varchar(4000),");
            tarSql.append("PROCESS_VERSION int,");
            tarSql.append("PROCESS_KEY varchar(510),");
            tarSql.append("TASK_DESCRIPTION varchar(510),");
            tarSql.append("ACTIVITY_NAME varchar(510),");
            tarSql.append("OUTCOME_DES varchar(510),");
            tarSql.append("OUTCOME varchar(510),");
            tarSql.append("CREATE_TIME timestamp,");
            tarSql.append("ENTITY_CODE varchar(510),");
            tarSql.append("INSTANCE_ID varchar(510),");
            tarSql.append("USER_ID bigint,");
            tarSql.append("COMMENTS varchar(4000),");
            tarSql.append("CID bigint,");
            tarSql.append("TABLE_INFO_ID bigint,");
            tarSql.append("USER_AGENT varchar(510),");
            tarSql.append("RECALLED_FLAG tinyint,");
            tarSql.append("STAFF bigint,");
            tarSql.append("MAIN_OBJ bigint,");
            tarSql.append("SORT int,");
            tarSql.append("PRIMARY KEY (ID));");
        }
        if (enableAcl) {//_ACL
            tableName = modelTableName + "_ACL";
            tarSql.append("CREATE TABLE " + tableName + "(");
            tarSql.append("ID bigint NOT NULL,");
            tarSql.append("VERSION int DEFAULT ((0)),");
            tarSql.append("OBJECT_ID bigint,");
            tarSql.append("SID_TYPE varchar(510),");
            tarSql.append("PERMISSION varchar(510),");
            tarSql.append("SID bigint,");
            tarSql.append("PRIMARY KEY (ID));");
        }
        if (isMneCode) {
            //_MC field enable mneCode-isUsedMneCode
            String mneCodeSql = createDefaultMCTableOfModelMysql(modelTableName);
            tarSql.append(mneCodeSql);
        }
        if (!isBaseEntity && isAttention && isMainModel) {
            tableName = modelTableName + "_PA";
            tarSql.append("CREATE TABLE " + tableName + "(");
            tarSql.append("ID bigint NOT NULL,");
            tarSql.append("VERSION int DEFAULT ((0)),");
            tarSql.append("DELETE_TIME timestamp, ");
            tarSql.append("MODIFY_TIME timestamp,");
            tarSql.append("CREATE_TIME timestamp,");
            tarSql.append("DELETE_STAFF_ID bigint,");
            tarSql.append("MODIFY_STAFF_ID bigint,");
            tarSql.append("CREATE_STAFF_ID bigint,");
            tarSql.append("TABLE_INFO_ID bigint,");
            tarSql.append("STAFF bigint,");
            tarSql.append("MAIN_OBJ bigint,");
            tarSql.append("VALID tinyint DEFAULT ((1)),");
            tarSql.append("PRIMARY KEY (ID));");
        }
        if (!isBaseEntity && isMainModel) {//_SV
            tableName = modelTableName + "_SV";
            tarSql.append("CREATE TABLE " + tableName + "(");
            tarSql.append("ID bigint NOT NULL,");
            tarSql.append("VERSION int DEFAULT ((0)),");
            tarSql.append("DELETE_TIME timestamp, ");
            tarSql.append("MODIFY_TIME timestamp,");
            tarSql.append("CREATE_TIME timestamp,");
            tarSql.append("DELETE_STAFF_ID bigint,");
            tarSql.append("MODIFY_STAFF_ID bigint,");
            tarSql.append("CREATE_STAFF_ID bigint,");
            tarSql.append("TABLE_INFO_ID bigint,");
            tarSql.append("STAFF bigint,");
            tarSql.append("MAIN_OBJ bigint,");
            tarSql.append("VALID tinyint DEFAULT ((1)),");
            tarSql.append("PRIMARY KEY (ID));");
        }
        if (!isBaseEntity && isGroupAstrict) {//_GI
            tableName = modelTableName + "_GI";
            tarSql.append("CREATE TABLE " + tableName + "(");
            tarSql.append("ID bigint NOT NULL,");
            tarSql.append("VERSION int DEFAULT ((0)),");
            tarSql.append("GROUP_ID bigint,");
            tarSql.append("TABLE_INFO_ID bigint,");
            tarSql.append("ENTITY_CODE varchar(510),");
            tarSql.append("PRIMARY KEY (ID));");
        }
        if (!"".equals(tarSql.toString()) && !checkTableIsExist(tableName, template, dbName)) {
            logger.debug("tarSql: " + tarSql.toString());
            template.batchUpdate(tarSql.toString().split("\\;"));
        }
    }

    private static void updateModelAndDefaultTablesMysql(Entity entity, Model model, JdbcTemplate template) {
        StringBuilder tarSql = new StringBuilder("");
        String oldTableName = model.getOrgTableName();
        String newTableName = model.getTableName();
        String newTableComment = model.getName();

        Boolean enableAcl = entity.getEnableAclRestrict();
        Boolean isGroupAstrict = entity.getGroupEnabled();
        Boolean isBaseEntity = entity.getIsBase();
        Boolean isAttention = entity.getPayCloseAttention();
        Boolean isMainModel = model.getIsMain();
        Boolean isMneCode = model.getIsMneCode();
        if (null != oldTableName && null != newTableName && !oldTableName.toUpperCase().equals(newTableName.toUpperCase())) {//修改表名
            tarSql.append("ALTER TABLE " + oldTableName.toUpperCase() + " RENAME TO " + newTableName.toUpperCase() + ";");
            if (isBaseEntity != null && !isBaseEntity && isMainModel) {//_DI
                tarSql.append("ALTER TABLE " + oldTableName.toUpperCase() + "_DI" + " RENAME TO " + newTableName.toUpperCase() + "_DI" + ";");
            }
            if (enableAcl) {//_ACL
                tarSql.append("ALTER TABLE " + oldTableName.toUpperCase() + "_ACL" + " RENAME TO " + newTableName.toUpperCase() + "_ACL" + ";");
            }
            if (isMneCode) {//_MC
                tarSql.append("ALTER TABLE " + oldTableName.toUpperCase() + "_MC" + " RENAME TO " + newTableName.toUpperCase() + "_MC" + ";");
            }
            if (!isBaseEntity && isAttention && isMainModel) {//_PA
                tarSql.append("ALTER TABLE " + oldTableName.toUpperCase() + "_PA" + " RENAME TO " + newTableName.toUpperCase() + "_PA" + ";");
            }
            if (!isBaseEntity && isMainModel) {//_SV
                tarSql.append("ALTER TABLE " + oldTableName.toUpperCase() + "_SV" + " RENAME TO " + newTableName.toUpperCase() + "_SV" + ";");
            }
            if (!isBaseEntity && isGroupAstrict) {//_GI
                tarSql.append("ALTER TABLE " + oldTableName.toUpperCase() + "_GI" + " RENAME TO " + newTableName.toUpperCase() + "_GI" + ";");
            }
        }
        if (null != newTableComment && !"".equals(newTableComment)) {//修改表注释
            tarSql.append("ALTER TABLE " + newTableName.toUpperCase() + " COMMENT '" + InternationalResource.get(newTableComment) + "';");
        }
        Boolean bExtraCol = model.getIsExtraCol();
        if (null != bExtraCol && bExtraCol.booleanValue()) {//新增大字段
            if (!extraColFieldIsExist(oldTableName)) {
//                tarSql.append("ALTER TABLE " + newTableName.toUpperCase() + " ADD (EXTRA_COL CLOB" + ");");
                tarSql.append("ALTER TABLE " + newTableName.toUpperCase() + " ADD COLUMN EXTRA_COL TEXT COMMENT '大字段';");
            }
        }
        if (!"".equals(tarSql.toString())) {
            logger.debug("tarSql: " + tarSql.toString());
            template.batchUpdate(tarSql.toString().split("\\;"));
        }
    }
}
