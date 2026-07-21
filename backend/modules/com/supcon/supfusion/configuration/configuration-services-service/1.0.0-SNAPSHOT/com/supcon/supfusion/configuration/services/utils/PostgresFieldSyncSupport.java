package com.supcon.supfusion.configuration.services.utils;

import com.supcon.supfusion.configuration.services.entity.Model;
import com.supcon.supfusion.configuration.services.entity.Property;
import com.supcon.supfusion.configuration.services.enums.DbColumnType;
import com.supcon.supfusion.configuration.services.i18n.InternationalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** PostgreSQL physical-column synchronization for low-code model properties. */
public final class PostgresFieldSyncSupport {
    private static final Logger logger = LoggerFactory.getLogger(PostgresFieldSyncSupport.class);
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("[A-Za-z][A-Za-z0-9_]{0,62}");

    private PostgresFieldSyncSupport() {
    }

    public static synchronized void sync(
            Property property,
            Model model,
            boolean isNew,
            JdbcTemplate template) {
        if (property == null || model == null || template == null) {
            throw new IllegalArgumentException("property, model and jdbcTemplate are required");
        }
        if (property.getType() == null) {
            throw new IllegalArgumentException("PostgreSQL property type is required");
        }
        if (property.getType() == DbColumnType.PROPERTYATTACHMENT) {
            return;
        }

        String tableName = identifier(model.getTableName(), "model table");
        String columnName = identifier(property.getColumnName(), "property column");
        if (!PostgresModelSyncSupport.tableExists(template, tableName)) {
            throw new IllegalStateException("PostgreSQL model table does not exist: " + tableName);
        }

        String oldColumnName = normalizedOptionalIdentifier(property.getOrgColumnName(), "original property column");
        if (!isNew && oldColumnName != null && !oldColumnName.equals(columnName)) {
            renameColumnIfNeeded(template, tableName, oldColumnName, columnName);
        }

        ColumnSpec target = columnSpec(property);
        if (!columnExists(template, tableName, columnName)) {
            execute(template, "ALTER TABLE public." + tableName + " ADD COLUMN " + columnName + " " + target.sqlType);
        } else {
            synchronizeCompatibleType(template, tableName, columnName, target);
        }

        updateColumnComment(template, tableName, columnName, property);
        synchronizeManagedIndex(template, tableName, columnName, oldColumnName, property);
    }

    public static synchronized void syncCustom(List<Property> properties, JdbcTemplate template) {
        if (properties == null || properties.isEmpty()) {
            return;
        }
        for (Property property : properties) {
            if (property != null && Boolean.TRUE.equals(property.getIsCustom())) {
                sync(property, property.getModel(), false, template);
            }
        }
    }

    static boolean columnExists(JdbcTemplate template, String tableName, String columnName) {
        String safeTableName = identifier(tableName, "model table");
        String safeColumnName = identifier(columnName, "property column");
        Integer count = template.queryForObject(
                "select count(1) from information_schema.columns "
                        + "where table_schema='public' and lower(table_name)=? and lower(column_name)=?",
                new Object[]{safeTableName.toLowerCase(Locale.ROOT), safeColumnName.toLowerCase(Locale.ROOT)},
                Integer.class);
        return count != null && count.intValue() > 0;
    }

    private static void renameColumnIfNeeded(
            JdbcTemplate template,
            String tableName,
            String oldColumnName,
            String newColumnName) {
        boolean oldExists = columnExists(template, tableName, oldColumnName);
        boolean newExists = columnExists(template, tableName, newColumnName);
        if (!oldExists && newExists) {
            return;
        }
        if (!oldExists) {
            throw new IllegalStateException(
                    "PostgreSQL original property column does not exist: " + tableName + "." + oldColumnName);
        }
        if (newExists) {
            throw new IllegalStateException(
                    "PostgreSQL target property column already exists: " + tableName + "." + newColumnName);
        }
        execute(template, "ALTER TABLE public." + tableName + " RENAME COLUMN " + oldColumnName + " TO " + newColumnName);
    }

    private static void synchronizeCompatibleType(
            JdbcTemplate template,
            String tableName,
            String columnName,
            ColumnSpec target) {
        ColumnState current = readColumnState(template, tableName, columnName);
        if (isCompatibleWithoutAlter(current, target)) {
            return;
        }
        if (isSafeTypeChange(current, target)) {
            execute(
                    template,
                    "ALTER TABLE public." + tableName + " ALTER COLUMN " + columnName + " TYPE " + target.sqlType);
            return;
        }
        throw new IllegalStateException(
                "Unsafe PostgreSQL property type change for " + tableName + "." + columnName
                        + ": " + current.describe() + " -> " + target.sqlType);
    }

    private static boolean isCompatibleWithoutAlter(ColumnState current, ColumnSpec target) {
        if (target.family == TypeFamily.VARCHAR && "character varying".equals(current.dataType)) {
            return current.characterLength != null && current.characterLength.intValue() >= target.length;
        }
        if (target.family == TypeFamily.VARCHAR && "text".equals(current.dataType)) {
            return true;
        }
        if (target.family == TypeFamily.TEXT && "text".equals(current.dataType)) {
            return true;
        }
        if (target.family == TypeFamily.INTEGER) {
            return "integer".equals(current.dataType) || "bigint".equals(current.dataType);
        }
        if (target.family == TypeFamily.BIGINT) {
            return "bigint".equals(current.dataType);
        }
        if (target.family == TypeFamily.NUMERIC && "numeric".equals(current.dataType)) {
            if (current.numericPrecision == null || current.numericScale == null) {
                return true;
            }
            int currentIntegerDigits = current.numericPrecision.intValue() - current.numericScale.intValue();
            int targetIntegerDigits = target.precision - target.scale;
            return currentIntegerDigits >= targetIntegerDigits && current.numericScale.intValue() >= target.scale;
        }
        if (target.family == TypeFamily.TIMESTAMP) {
            return "timestamp without time zone".equals(current.dataType);
        }
        if (target.family == TypeFamily.BYTEA) {
            return "bytea".equals(current.dataType);
        }
        return false;
    }

    private static boolean isSafeTypeChange(ColumnState current, ColumnSpec target) {
        if (target.family == TypeFamily.VARCHAR && "character varying".equals(current.dataType)) {
            return current.characterLength != null && target.length >= current.characterLength.intValue();
        }
        if (target.family == TypeFamily.TEXT) {
            return "character varying".equals(current.dataType);
        }
        if (target.family == TypeFamily.BIGINT) {
            return "smallint".equals(current.dataType) || "integer".equals(current.dataType);
        }
        if (target.family == TypeFamily.INTEGER) {
            return "smallint".equals(current.dataType);
        }
        if (target.family == TypeFamily.NUMERIC && "numeric".equals(current.dataType)) {
            if (current.numericPrecision == null || current.numericScale == null) {
                return false;
            }
            int currentIntegerDigits = current.numericPrecision.intValue() - current.numericScale.intValue();
            int targetIntegerDigits = target.precision - target.scale;
            return targetIntegerDigits >= currentIntegerDigits && target.scale >= current.numericScale.intValue();
        }
        if (target.family == TypeFamily.TIMESTAMP) {
            return "date".equals(current.dataType);
        }
        return false;
    }

    private static ColumnState readColumnState(JdbcTemplate template, String tableName, String columnName) {
        String value = template.queryForObject(
                "select data_type || '|' || coalesce(character_maximum_length::text,'') || '|' "
                        + "|| coalesce(numeric_precision::text,'') || '|' || coalesce(numeric_scale::text,'') "
                        + "from information_schema.columns where table_schema='public' "
                        + "and lower(table_name)=? and lower(column_name)=?",
                new Object[]{tableName.toLowerCase(Locale.ROOT), columnName.toLowerCase(Locale.ROOT)},
                String.class);
        if (value == null) {
            throw new IllegalStateException("PostgreSQL property column metadata is missing: " + tableName + "." + columnName);
        }
        String[] values = value.split("\\|", -1);
        return new ColumnState(
                values[0].toLowerCase(Locale.ROOT),
                integerOrNull(values[1]),
                integerOrNull(values[2]),
                integerOrNull(values[3]));
    }

    private static void updateColumnComment(
            JdbcTemplate template,
            String tableName,
            String columnName,
            Property property) {
        String comment = InternationalResource.get(property.getDisplayName());
        if (comment == null || comment.trim().isEmpty()) {
            comment = property.getDisplayName();
        }
        if (comment != null && !comment.trim().isEmpty()) {
            execute(
                    template,
                    "COMMENT ON COLUMN public." + tableName + "." + columnName + " IS '" + sqlLiteral(comment) + "'");
        }
    }

    private static void synchronizeManagedIndex(
            JdbcTemplate template,
            String tableName,
            String columnName,
            String oldColumnName,
            Property property) {
        boolean indexRequested = Boolean.TRUE.equals(property.getIsIndex());
        String targetIndexName = indexName(tableName, columnName);
        String oldIndexName = oldColumnName == null ? null : indexName(tableName, oldColumnName);

        if (oldIndexName != null
                && !oldIndexName.equals(targetIndexName)
                && managedIndexExists(template, tableName, columnName, oldIndexName)) {
            if (!indexRequested || managedIndexExists(template, tableName, columnName, targetIndexName)) {
                dropManagedIndex(template, oldIndexName);
            } else if (indexRelationExists(template, targetIndexName)) {
                throw new IllegalStateException(
                        "PostgreSQL managed property index name is already in use: " + targetIndexName);
            } else {
                renameManagedIndex(template, oldIndexName, targetIndexName);
            }
        }

        if (!indexRequested) {
            if (managedIndexExists(template, tableName, columnName, targetIndexName)) {
                dropManagedIndex(template, targetIndexName);
            }
            return;
        }

        if (managedIndexExists(template, tableName, columnName, targetIndexName)
                || hasEquivalentIndexForColumn(template, tableName, columnName)) {
            return;
        }
        if (indexRelationExists(template, targetIndexName)) {
            throw new IllegalStateException(
                    "PostgreSQL managed property index name is already in use: " + targetIndexName);
        }
        execute(
                template,
                "CREATE INDEX IF NOT EXISTS " + targetIndexName + " ON public." + tableName + " (" + columnName + ")");
    }

    private static boolean managedIndexExists(
            JdbcTemplate template,
            String tableName,
            String columnName,
            String indexName) {
        Integer count = template.queryForObject(
                "select count(1) from pg_index i "
                        + "join pg_class t on t.oid=i.indrelid "
                        + "join pg_namespace n on n.oid=t.relnamespace "
                        + "join pg_class x on x.oid=i.indexrelid "
                        + "join pg_attribute a on a.attrelid=t.oid and a.attnum=any(i.indkey) "
                        + "where n.nspname='public' and lower(t.relname)=? and lower(a.attname)=? "
                        + "and lower(x.relname)=? and i.indnatts=1 and not i.indisprimary and not i.indisunique "
                        + "and i.indisvalid and i.indisready",
                new Object[]{
                        tableName.toLowerCase(Locale.ROOT),
                        columnName.toLowerCase(Locale.ROOT),
                        indexName.toLowerCase(Locale.ROOT)},
                Integer.class);
        return count != null && count.intValue() > 0;
    }

    private static boolean hasEquivalentIndexForColumn(
            JdbcTemplate template,
            String tableName,
            String columnName) {
        Integer count = template.queryForObject(
                "select count(1) from pg_index i "
                        + "join pg_class t on t.oid=i.indrelid "
                        + "join pg_namespace n on n.oid=t.relnamespace "
                        + "join pg_attribute a on a.attrelid=t.oid and a.attnum=any(i.indkey) "
                        + "where n.nspname='public' and lower(t.relname)=? and lower(a.attname)=? "
                        + "and i.indnatts=1 and not i.indisprimary and i.indisvalid and i.indisready",
                new Object[]{tableName.toLowerCase(Locale.ROOT), columnName.toLowerCase(Locale.ROOT)},
                Integer.class);
        return count != null && count.intValue() > 0;
    }

    private static boolean indexRelationExists(JdbcTemplate template, String indexName) {
        Integer count = template.queryForObject(
                "select count(1) from pg_class x join pg_namespace n on n.oid=x.relnamespace "
                        + "where n.nspname='public' and x.relkind in ('i','I') and lower(x.relname)=?",
                new Object[]{indexName.toLowerCase(Locale.ROOT)},
                Integer.class);
        return count != null && count.intValue() > 0;
    }

    private static void renameManagedIndex(
            JdbcTemplate template,
            String oldIndexName,
            String newIndexName) {
        execute(template, "ALTER INDEX public." + oldIndexName + " RENAME TO " + newIndexName);
    }

    private static void dropManagedIndex(JdbcTemplate template, String indexName) {
        execute(template, "DROP INDEX IF EXISTS public." + indexName);
    }

    private static String indexName(String tableName, String columnName) {
        String base = "IDX_" + tableName + "_" + columnName;
        if (base.length() <= 63) {
            return identifier(base, "property index");
        }
        String hash = Integer.toHexString(base.hashCode()).toUpperCase(Locale.ROOT);
        return identifier(base.substring(0, 62 - hash.length()) + "_" + hash, "property index");
    }

    private static ColumnSpec columnSpec(Property property) {
        DbColumnType type = property.getType();
        if (type == DbColumnType.TEXT) {
            int requested = property.getMaxLength() == null || property.getMaxLength().intValue() <= 0
                    ? 255 : property.getMaxLength().intValue();
            int length = (int) Math.min(4000L, (long) requested * 2L);
            return ColumnSpec.varchar(length);
        }
        if (type == DbColumnType.BAPCODE || type == DbColumnType.SUMMARY || type == DbColumnType.SYSTEMCODE) {
            return ColumnSpec.varchar(4000);
        }
        if (type == DbColumnType.INTEGER || type == DbColumnType.BOOLEAN) {
            return ColumnSpec.simple(TypeFamily.INTEGER, "integer");
        }
        if (type == DbColumnType.DECIMAL || type == DbColumnType.MONEY) {
            int scale = property.getDecimalNum() == null ? 6 : property.getDecimalNum().intValue();
            if (scale < 0 || scale > 18) {
                throw new IllegalArgumentException("PostgreSQL decimal scale must be between 0 and 18: " + scale);
            }
            return ColumnSpec.numeric(19, scale);
        }
        if (type == DbColumnType.DATE || type == DbColumnType.TIME || type == DbColumnType.DATETIME) {
            return ColumnSpec.simple(TypeFamily.TIMESTAMP, "timestamp without time zone");
        }
        if (type == DbColumnType.LONGTEXT || type == DbColumnType.OFFICE) {
            return ColumnSpec.simple(TypeFamily.TEXT, "text");
        }
        if (type == DbColumnType.LONG) {
            return ColumnSpec.simple(TypeFamily.BIGINT, "bigint");
        }
        if (type == DbColumnType.OBJECT) {
            Property associated = property.getAssociatedProperty();
            if (associated != null && associated.getType() == DbColumnType.BAPCODE) {
                return ColumnSpec.varchar(4000);
            }
            if (associated != null && associated.getType() != null && associated.getType() != DbColumnType.LONG) {
                int requested = associated.getMaxLength() == null || associated.getMaxLength().intValue() <= 0
                        ? 255 : associated.getMaxLength().intValue();
                return ColumnSpec.varchar((int) Math.min(4000L, (long) requested * 2L));
            }
            return ColumnSpec.simple(TypeFamily.BIGINT, "bigint");
        }
        if (type == DbColumnType.PASSWORD || type == DbColumnType.PICTURE || type == DbColumnType.TAGNUMBER
                || type == DbColumnType.ENUMERATE || type == DbColumnType.ITEMINDEX
                || type == DbColumnType.COLOR || type == DbColumnType.LAYER) {
            return ColumnSpec.varchar(510);
        }
        if (type == DbColumnType.BINARY) {
            return ColumnSpec.simple(TypeFamily.BYTEA, "bytea");
        }
        throw new IllegalArgumentException("Unsupported PostgreSQL property type: " + type);
    }

    private static String normalizedOptionalIdentifier(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return identifier(value, label);
    }

    private static String identifier(String value, String label) {
        if (value == null) {
            throw new IllegalArgumentException("PostgreSQL " + label + " name is required");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!SAFE_IDENTIFIER.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Unsafe PostgreSQL " + label + " identifier: " + value);
        }
        return normalized;
    }

    private static String sqlLiteral(String value) {
        return value.replace("'", "''");
    }

    private static Integer integerOrNull(String value) {
        return value == null || value.isEmpty() ? null : Integer.valueOf(value);
    }

    private static void execute(JdbcTemplate template, String sql) {
        logger.info("PostgreSQL field sync SQL: {}", sql);
        template.execute(sql);
    }

    private enum TypeFamily {
        VARCHAR,
        TEXT,
        INTEGER,
        BIGINT,
        NUMERIC,
        TIMESTAMP,
        BYTEA
    }

    private static final class ColumnSpec {
        private final TypeFamily family;
        private final String sqlType;
        private final int length;
        private final int precision;
        private final int scale;

        private ColumnSpec(TypeFamily family, String sqlType, int length, int precision, int scale) {
            this.family = family;
            this.sqlType = sqlType;
            this.length = length;
            this.precision = precision;
            this.scale = scale;
        }

        private static ColumnSpec varchar(int length) {
            return new ColumnSpec(TypeFamily.VARCHAR, "varchar(" + length + ")", length, 0, 0);
        }

        private static ColumnSpec numeric(int precision, int scale) {
            return new ColumnSpec(
                    TypeFamily.NUMERIC,
                    "numeric(" + precision + "," + scale + ")",
                    0,
                    precision,
                    scale);
        }

        private static ColumnSpec simple(TypeFamily family, String sqlType) {
            return new ColumnSpec(family, sqlType, 0, 0, 0);
        }
    }

    private static final class ColumnState {
        private final String dataType;
        private final Integer characterLength;
        private final Integer numericPrecision;
        private final Integer numericScale;

        private ColumnState(
                String dataType,
                Integer characterLength,
                Integer numericPrecision,
                Integer numericScale) {
            this.dataType = dataType;
            this.characterLength = characterLength;
            this.numericPrecision = numericPrecision;
            this.numericScale = numericScale;
        }

        private String describe() {
            if ("character varying".equals(dataType)) {
                return dataType + "(" + characterLength + ")";
            }
            if ("numeric".equals(dataType)) {
                return dataType + "(" + numericPrecision + "," + numericScale + ")";
            }
            return dataType;
        }
    }
}
