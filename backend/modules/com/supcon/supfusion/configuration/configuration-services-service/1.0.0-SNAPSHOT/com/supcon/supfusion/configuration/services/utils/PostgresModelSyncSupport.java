package com.supcon.supfusion.configuration.services.utils;

import com.supcon.supfusion.configuration.services.entity.Entity;
import com.supcon.supfusion.configuration.services.entity.Model;
import com.supcon.supfusion.configuration.services.i18n.InternationalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Locale;
import java.util.regex.Pattern;

/** PostgreSQL physical-table synchronization for low-code entity models. */
public final class PostgresModelSyncSupport {
    private static final Logger logger = LoggerFactory.getLogger(PostgresModelSyncSupport.class);
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("[A-Za-z][A-Za-z0-9_]{0,62}");
    private static final String[] AUXILIARY_SUFFIXES = {"_DI", "_ACL", "_MC", "_PA", "_SV", "_GI"};

    private PostgresModelSyncSupport() {
    }

    public static synchronized void sync(Entity entity, Model model, boolean isNew, JdbcTemplate template) {
        if (entity == null || model == null || template == null) {
            throw new IllegalArgumentException("entity, model and jdbcTemplate are required");
        }

        String tableName = identifier(model.getTableName());
        if (!isNew) {
            renameModelTablesIfNeeded(model.getOrgTableName(), tableName, template);
        }

        createModelTable(entity, model, tableName, template);
        createRequiredAuxiliaryTables(entity, model, tableName, template);
        ensureExtraColumn(model, tableName, template);
        updateTableComment(model, tableName, template);
    }

    public static synchronized void createMneCodeTable(Model model, JdbcTemplate template) {
        if (model == null || template == null) {
            throw new IllegalArgumentException("model and jdbcTemplate are required");
        }
        createMneCodeTable(identifier(model.getTableName()), template);
    }

    static boolean tableExists(JdbcTemplate template, String tableName) {
        String safeTableName = identifier(tableName);
        Integer count = template.queryForObject(
                "select count(1) from information_schema.tables "
                        + "where table_schema='public' and lower(table_name)=?",
                new Object[]{safeTableName.toLowerCase(Locale.ROOT)},
                Integer.class);
        return count != null && count.intValue() > 0;
    }

    private static boolean columnExists(JdbcTemplate template, String tableName, String columnName) {
        String safeTableName = identifier(tableName);
        String safeColumnName = identifier(columnName);
        Integer count = template.queryForObject(
                "select count(1) from information_schema.columns "
                        + "where table_schema='public' and lower(table_name)=? and lower(column_name)=?",
                new Object[]{
                        safeTableName.toLowerCase(Locale.ROOT),
                        safeColumnName.toLowerCase(Locale.ROOT)
                },
                Integer.class);
        return count != null && count.intValue() > 0;
    }

    private static void createModelTable(Entity entity, Model model, String tableName, JdbcTemplate template) {
        boolean mainModel = isTrue(model.getIsMain());
        boolean baseEntity = isTrue(entity.getIsBase());
        boolean treeModel = model.getDataType() != null && model.getDataType().intValue() == Model.DATA_TYPE_TREE;
        StringBuilder columns = new StringBuilder();

        appendColumn(columns, "ID bigint NOT NULL");
        appendColumn(columns, "VERSION integer DEFAULT 0");
        appendColumn(columns, "DELETE_TIME timestamp without time zone");
        appendColumn(columns, "MODIFY_TIME timestamp without time zone");
        appendColumn(columns, "CREATE_TIME timestamp without time zone");
        appendColumn(columns, "DELETE_STAFF_ID bigint");
        appendColumn(columns, "MODIFY_STAFF_ID bigint");
        appendColumn(columns, "CREATE_STAFF_ID bigint");
        appendColumn(columns, "VALID integer DEFAULT 1");
        appendColumn(columns, "CID bigint");

        if (treeModel) {
            appendColumn(columns, "LEAF integer");
            appendColumn(columns, "FULL_PATH_NAME varchar(4000)");
            appendColumn(columns, "PARENT_ID varchar(510)");
            appendColumn(columns, "SORT bigint");
            appendColumn(columns, "LAY_NO integer");
            appendColumn(columns, "LAY_REC varchar(4000)");
        } else {
            appendColumn(columns, "SORT integer");
        }

        if (mainModel) {
            appendMainModelColumns(columns, treeModel);
            if (!baseEntity) {
                appendColumn(columns, "TABLE_NO varchar(510)");
                appendColumn(columns, "TABLE_INFO_ID bigint");
            }
        } else if (!baseEntity) {
            appendColumn(columns, "TABLE_INFO_ID bigint");
        }

        if (isTrue(model.getIsExtraCol())) {
            appendColumn(columns, "EXTRA_COL text");
        }
        appendColumn(columns, "PRIMARY KEY (ID)");

        execute(template, "CREATE TABLE IF NOT EXISTS public." + tableName + " (" + columns + ")");
    }

    private static void appendMainModelColumns(StringBuilder columns, boolean treeModel) {
        appendColumn(columns, "EFFECTIVE_STATE bigint");
        appendColumn(columns, "PROCESS_VERSION integer");
        appendColumn(columns, "PROCESS_KEY varchar(510)");
        appendColumn(columns, "DEPLOYMENT_ID bigint");
        if (!treeModel) {
            appendColumn(columns, "GROUP_ID bigint");
        }
        appendColumn(columns, "STATUS bigint");
        appendColumn(columns, "EFFECT_TIME timestamp without time zone");
        appendColumn(columns, "EFFECT_STAFF_ID bigint");
        appendColumn(columns, "OWNER_DEPARTMENT_ID bigint");
        appendColumn(columns, "OWNER_POSITION_ID bigint");
        appendColumn(columns, "OWNER_STAFF_ID bigint");
        appendColumn(columns, "POSITION_LAY_REC varchar(510)");
        appendColumn(columns, "CREATE_POSITION_ID bigint");
        appendColumn(columns, "CREATE_DEPARTMENT_ID bigint");
        if (treeModel) {
            appendColumn(columns, "OA text");
        }
    }

    private static void createRequiredAuxiliaryTables(
            Entity entity,
            Model model,
            String modelTableName,
            JdbcTemplate template) {
        boolean baseEntity = isTrue(entity.getIsBase());
        boolean mainModel = isTrue(model.getIsMain());

        if (!baseEntity && mainModel) {
            createDealInfoTable(modelTableName, template);
        }
        if (isTrue(entity.getEnableAclRestrict())) {
            createAclTable(modelTableName, template);
        }
        if (isTrue(model.getIsMneCode())) {
            createMneCodeTable(modelTableName, template);
        }
        if (!baseEntity && isTrue(entity.getPayCloseAttention()) && mainModel) {
            createAttentionTable(modelTableName, "_PA", template);
        }
        if (!baseEntity && mainModel) {
            createAttentionTable(modelTableName, "_SV", template);
        }
        if (!baseEntity && isTrue(entity.getGroupEnabled())) {
            createGroupTable(modelTableName, template);
        }
    }

    private static void createDealInfoTable(String modelTableName, JdbcTemplate template) {
        String tableName = identifier(modelTableName + "_DI");
        execute(template, "CREATE TABLE IF NOT EXISTS public." + tableName + " ("
                + "ID bigint NOT NULL, VERSION integer DEFAULT 0, SIGNATURE varchar(800), "
                + "PENDING_CREATE_TIME timestamp without time zone, DEALINFO_TYPE varchar(510), "
                + "PROXY_STAFF_IDS varchar(510), PROXY_STAFF varchar(510), ASSIGN_STAFF_ID varchar(510), "
                + "ASSIGN_STAFF varchar(4000), PROCESS_VERSION integer, PROCESS_KEY varchar(510), "
                + "TASK_DESCRIPTION varchar(510), ACTIVITY_NAME varchar(510), OUTCOME_DES varchar(510), "
                + "OUTCOME varchar(510), CREATE_TIME timestamp without time zone, ENTITY_CODE varchar(510), "
                + "INSTANCE_ID varchar(510), USER_ID bigint, COMMENTS varchar(4000), CID bigint, "
                + "TABLE_INFO_ID bigint, USER_AGENT varchar(510), RECALLED_FLAG integer, STAFF bigint, "
                + "MAIN_OBJ bigint, SORT integer, PRIMARY KEY (ID))");
    }

    private static void createAclTable(String modelTableName, JdbcTemplate template) {
        String tableName = identifier(modelTableName + "_ACL");
        execute(template, "CREATE TABLE IF NOT EXISTS public." + tableName + " ("
                + "ID bigint NOT NULL, VERSION integer DEFAULT 0, OBJECT_ID bigint, SID_TYPE varchar(510), "
                + "PERMISSION varchar(510), SID bigint, PRIMARY KEY (ID))");
    }

    private static void createMneCodeTable(String modelTableName, JdbcTemplate template) {
        String safeModelTableName = identifier(modelTableName);
        String tableName = identifier(safeModelTableName + "_MC");
        execute(template, "CREATE TABLE IF NOT EXISTS public." + tableName + " ("
                + "ID bigint NOT NULL, VERSION integer DEFAULT 0, " + safeModelTableName
                + " bigint, MNE_CODE varchar(510), PRIMARY KEY (ID))");
    }

    private static void createAttentionTable(String modelTableName, String suffix, JdbcTemplate template) {
        String tableName = identifier(modelTableName + suffix);
        execute(template, "CREATE TABLE IF NOT EXISTS public." + tableName + " ("
                + "ID bigint NOT NULL, VERSION integer DEFAULT 0, DELETE_TIME timestamp without time zone, "
                + "MODIFY_TIME timestamp without time zone, CREATE_TIME timestamp without time zone, "
                + "DELETE_STAFF_ID bigint, MODIFY_STAFF_ID bigint, CREATE_STAFF_ID bigint, "
                + "TABLE_INFO_ID bigint, STAFF bigint, MAIN_OBJ bigint, VALID integer DEFAULT 1, PRIMARY KEY (ID))");
    }

    private static void createGroupTable(String modelTableName, JdbcTemplate template) {
        String tableName = identifier(modelTableName + "_GI");
        execute(template, "CREATE TABLE IF NOT EXISTS public." + tableName + " ("
                + "ID bigint NOT NULL, VERSION integer DEFAULT 0, GROUP_ID bigint, TABLE_INFO_ID bigint, "
                + "ENTITY_CODE varchar(510), PRIMARY KEY (ID))");
    }

    private static void renameModelTablesIfNeeded(String oldTableName, String newTableName, JdbcTemplate template) {
        if (oldTableName == null || oldTableName.trim().isEmpty()) {
            return;
        }
        String safeOldName = identifier(oldTableName);
        if (safeOldName.equals(newTableName)) {
            return;
        }

        renameTableIfPresent(safeOldName, newTableName, template);
        for (String suffix : AUXILIARY_SUFFIXES) {
            renameTableIfPresent(
                    identifier(safeOldName + suffix),
                    identifier(newTableName + suffix),
                    template);
        }
    }

    private static void renameTableIfPresent(String oldTableName, String newTableName, JdbcTemplate template) {
        if (!tableExists(template, oldTableName)) {
            return;
        }
        if (tableExists(template, newTableName)) {
            throw new IllegalStateException("PostgreSQL model table already exists: " + newTableName);
        }
        execute(template, "ALTER TABLE public." + oldTableName + " RENAME TO " + newTableName);
    }

    private static void ensureExtraColumn(Model model, String tableName, JdbcTemplate template) {
        if (isTrue(model.getIsExtraCol()) && !columnExists(template, tableName, "EXTRA_COL")) {
            execute(template, "ALTER TABLE public." + tableName + " ADD COLUMN EXTRA_COL text");
        }
    }

    private static void updateTableComment(Model model, String tableName, JdbcTemplate template) {
        if (model.getName() == null || model.getName().trim().isEmpty()) {
            return;
        }
        String comment = InternationalResource.get(model.getName());
        if (comment == null || comment.trim().isEmpty()) {
            comment = model.getName();
        }
        execute(template, "COMMENT ON TABLE public." + tableName + " IS '" + sqlLiteral(comment) + "'");
    }

    private static String identifier(String value) {
        if (value == null) {
            throw new IllegalArgumentException("PostgreSQL model table name is required");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!SAFE_IDENTIFIER.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Unsafe PostgreSQL model table identifier: " + value);
        }
        return normalized;
    }

    private static String sqlLiteral(String value) {
        return value.replace("'", "''");
    }

    private static boolean isTrue(Boolean value) {
        return Boolean.TRUE.equals(value);
    }

    private static void appendColumn(StringBuilder columns, String definition) {
        if (columns.length() > 0) {
            columns.append(", ");
        }
        columns.append(definition);
    }

    private static void execute(JdbcTemplate template, String sql) {
        logger.info("PostgreSQL model sync SQL: {}", sql);
        template.execute(sql);
    }
}
