# Oracle 迁移 Backlog

本文件由 `scripts/generate-oracle-migration-audit.py` 生成，用来跟踪仓库内仍然出现的 Oracle/ojdbc/Oracle 方言引用。

注意：本报告不是说所有引用都要立刻删除。`documentation-or-workflow`、`tooling-or-audit-code`、`allowed-legacy-contract` 属于可解释引用；`*-backlog` 和 `legacy-*` 是后续模块迁移时要逐步消化的项。

## 摘要

- Generated At：`2026-06-22T10:39:07+00:00`。
- Repo Commit：`8a072c75a795c0027ed53a52a9a74e3ea3cbf794`。
- 总引用数：`958`。
- 未分类引用数：`0`；新增未分类 Oracle 引用会让生成器失败。
- 默认运行路径仍以 PostgreSQL 为准；Oracle 只能作为显式 legacy 路径。
- 机器可读报告：`metadata/oracle-migration-audit.json`。

## 分类统计

| Category | Count | Meaning |
| --- | --- | --- |
| allowed-legacy-contract | 6 | Oracle is explicit legacy compatibility, not the default runtime path. |
| decompiled-runtime-backlog | 16 | Decompiled runtime config/source contains Oracle-specific branch or keyword. |
| documentation-or-workflow | 186 | Documentation/template reference; keep wording aligned with PostgreSQL-first policy. |
| frontend-row-index-noise | 4 | Frontend rowNum variable naming is not Oracle SQL ROWNUM. |
| legacy-ojdbc-dependency | 6 | Recovered module POM declares Oracle JDBC and needs module-level replacement. |
| legacy-oracle-sql-resource | 160 | Recovered Oracle SQL/mapper resource; keep as reference until PostgreSQL module migration is complete. |
| postgres-compat-reference | 6 | PostgreSQL compatibility SQL may mention Oracle as source context. |
| postgres-conversion-tooling | 35 | Runtime conversion script; Oracle references should convert away from Oracle defaults. |
| recovered-source-backlog | 266 | Recovered source contains Oracle-specific branch or keyword; verify during module promotion. |
| runtime-patch-backlog | 9 | Runtime patch still contains Oracle branch logic that should be retired after source promotion. |
| tooling-or-audit-code | 264 | Tooling may mention Oracle to generate or check migration audit outputs. |

## 高频文件

| File | Findings |
| --- | --- |
| backend/modules/com/supcon/supfusion/rbac-dao/1.0.0-SNAPSHOT/META-INF/oracle/rbac_1.sql | 61 |
| scripts/generate-oracle-replacement-status.py | 52 |
| scripts/generate-oracle-migration-audit.py | 47 |
| scripts/verify-project-goal-acceptance.py | 38 |
| backend/modules/com/supcon/supfusion/systemcode-dao/1.0.0-SNAPSHOT/META-INF/mariadb/syscode_1.sql | 34 |
| backend/modules/com/supcon/supfusion/systemcode-dao/1.0.0-SNAPSHOT/META-INF/mysql/syscode_1.sql | 34 |
| scripts/verify-module-intake-precheck.py | 33 |
| docs/oracle-to-postgres-transition.md | 30 |
| scripts/generate-backend-dependency-inventory.py | 24 |
| backend/modules/com/supcon/supfusion/flow/flow-dao/1.0.0-RELEASE/META-INF/oracle/flow_1.sql | 20 |
| docs/project-objectives.md | 20 |
| scripts/precheck-module-intake.py | 20 |
| backend/modules/com/supcon/supfusion/notification/admin-dao/1.0.0-SNAPSHOT/META-INF/oracle/ntfm_1.sql | 17 |
| scripts/verify-source-modules.py | 17 |
| docs/sustainable-development.md | 15 |
| Makefile | 14 |
| scripts/verify-sustainable-repo.py | 14 |
| backend/modules/com/supcon/supfusion/system-config-dao/1.0.0-SNAPSHOT/META-INF/mariadb/systemconfig_1.sql | 12 |
| backend/modules/com/supcon/supfusion/system-config-dao/1.0.0-SNAPSHOT/META-INF/mysql/systemconfig_1.sql | 12 |
| deploy/docker/scripts/audit-postgres-mappings.py | 12 |
| docs/backend-module-promotion-guide.md | 12 |
| backend/modules/com/supcon/supfusion/auth-dao/1.0.0-SNAPSHOT/META-INF/oracle/auth_1.sql | 11 |
| backend/modules/com/supcon/supfusion/notification/admin-service/1.0.0-SNAPSHOT/com/supcon/supfusion/notification/admin/service/impl/NoticeTaskServiceImpl.java | 9 |
| backend/modules/com/supcon/supfusion/rbac-dao/1.0.0-SNAPSHOT/META-INF/mariadb/rbac_1.sql | 9 |
| backend/modules/com/supcon/supfusion/rbac-dao/1.0.0-SNAPSHOT/META-INF/mysql/rbac_1.sql | 9 |
| deploy/docker/scripts/patch-postgres-runtime.py | 9 |
| docs/backend-module-dependency-inventory.md | 9 |
| backend/modules/com/supcon/supfusion/i18n-dao/1.0.1-SNAPSHOT/com/supcon/supfusion/i18n/dao/mapper/oracle/I18nResourceDao.xml | 8 |
| backend/modules/com/supcon/supfusion/i18n-service/1.0.1-SNAPSHOT/com/supcon/supfusion/i18n/service/impl/I18nResourceServiceImpl.java | 8 |
| deploy/docker/scripts/adp-nacos-config-drift-smoke.js | 8 |

## 优先 Backlog 样例

| File | Line | Category | Pattern | Excerpt |
| --- | --- | --- | --- | --- |
| backend/decompiled-services/auditlog/src/main/java/com/supcon/supfusion/auditlog/bootstrap/config/MybatisPlusConfig.java | 36 | decompiled-runtime-backlog | oracle-keyword | if (DbType.ORACLE.getDb().equals(dbType)) { |
| backend/decompiled-services/auditlog/src/main/java/com/supcon/supfusion/auditlog/bootstrap/config/MybatisPlusConfig.java | 37 | decompiled-runtime-backlog | oracle-keyword | paginationInterceptor.setDbType(DbType.ORACLE); |
| backend/decompiled-services/configuration/src/main/resources/application-dev.yml | 47 | decompiled-runtime-backlog | oracle-keyword | db-type: ${DB_TYPE:oracle} |
| backend/decompiled-services/configuration/src/main/resources/application-dev.yml | 56 | decompiled-runtime-backlog | from-dual | validation-query-sql: SELECT 1 FROM DUAL |
| backend/decompiled-services/configuration/src/main/resources/application.yml | 75 | decompiled-runtime-backlog | oracle-keyword | db-type: ${SUPOS_SYSTEM_DB_TYPE:oracle} |
| backend/decompiled-services/configuration/src/main/resources/application.yml | 85 | decompiled-runtime-backlog | from-dual | validation-query-sql: SELECT 1 FROM DUAL |
| backend/decompiled-services/configuration/src/main/resources/i18n/appConfig/appConfig_zh_CN.properties | 2943 | decompiled-runtime-backlog | oracle-keyword | ec.scheduler.datasource.datasourceType.oracle=ORACLE |
| backend/decompiled-services/customProperty/src/main/resources/application-dev.yml | 39 | decompiled-runtime-backlog | oracle-keyword | db-type: ${DB_TYPE:oracle} |
| backend/decompiled-services/customProperty/src/main/resources/application-dev.yml | 49 | decompiled-runtime-backlog | from-dual | validationQuerySql: ${VALIDATION_QUERY_SQL:SELECT 1 FROM DUAL} |
| backend/decompiled-services/fileServer/src/main/resources/application-dev.yml | 46 | decompiled-runtime-backlog | oracle-keyword | db-type: ${DB_TYPE:oracle} |
| backend/decompiled-services/i18n/src/main/resources/application-dev.yml | 71 | decompiled-runtime-backlog | oracle-keyword | db-type: oracle |
| backend/decompiled-services/i18n/src/main/resources/application-dev.yml | 81 | decompiled-runtime-backlog | from-dual | validationQuerySql: ${VALIDATIONQUERYSQL:SELECT 1 FROM DUAL} |
| backend/decompiled-services/license/src/main/resources/application.yml | 90 | decompiled-runtime-backlog | oracle-keyword | db-type: ${SUPOS_SYSTEM_DB_TYPE:oracle} |
| backend/decompiled-services/notification-admin/src/main/resources/application-dev.yml | 42 | decompiled-runtime-backlog | from-dual | validationQuerySql: SELECT 1 FROM DUAL |
| backend/decompiled-services/notification-apiserver/src/main/resources/application-dev.yml | 46 | decompiled-runtime-backlog | from-dual | validationQuerySql: SELECT 1 FROM DUAL |
| backend/decompiled-services/notification-engine/src/main/resources/application-dev.yml | 44 | decompiled-runtime-backlog | from-dual | validationQuerySql: SELECT 1 FROM DUAL |
| backend/modules/com/supcon/supfusion/auth-dao/1.0.0-SNAPSHOT/META-INF/mariadb/auth_1.sql | 214 | recovered-source-backlog | from-dual | insert into auth_user(id,user_name,company_id,password,user_type,person_id) select 1,'admin',1000,'$2a$10$QEd181jr.RNYME6hz/.xpONiMe3uGkI5sI8fjH5DQWwgwBKEs0/Cy',1,1 from dual where not exists(select user_name from auth_u |
| backend/modules/com/supcon/supfusion/auth-dao/1.0.0-SNAPSHOT/META-INF/mysql/auth_1.sql | 214 | recovered-source-backlog | from-dual | insert into auth_user(id,user_name,company_id,password,user_type,person_id) select 1,'admin',1000,'$2a$10$QEd181jr.RNYME6hz/.xpONiMe3uGkI5sI8fjH5DQWwgwBKEs0/Cy',1,1 from dual where not exists(select user_name from auth_u |
| backend/modules/com/supcon/supfusion/auth-dao/1.0.0-SNAPSHOT/META-INF/oracle/auth_1.sql | 25 | legacy-oracle-sql-resource | sysdate | create_time timestamp default sysdate not null, -- comment '创建时间', |
| backend/modules/com/supcon/supfusion/auth-dao/1.0.0-SNAPSHOT/META-INF/oracle/auth_1.sql | 49 | legacy-oracle-sql-resource | sysdate | create_time timestamp default sysdate not null, -- comment '创建时间', |
| backend/modules/com/supcon/supfusion/auth-dao/1.0.0-SNAPSHOT/META-INF/oracle/auth_1.sql | 85 | legacy-oracle-sql-resource | sysdate | create_time timestamp default sysdate, -- comment '创建时间', |
| backend/modules/com/supcon/supfusion/auth-dao/1.0.0-SNAPSHOT/META-INF/oracle/auth_1.sql | 112 | legacy-oracle-sql-resource | sysdate | create_time timestamp default sysdate, -- comment '创建时间', |
| backend/modules/com/supcon/supfusion/auth-dao/1.0.0-SNAPSHOT/META-INF/oracle/auth_1.sql | 132 | legacy-oracle-sql-resource | sysdate | create_time timestamp default sysdate, -- comment '创建时间', |
| backend/modules/com/supcon/supfusion/auth-dao/1.0.0-SNAPSHOT/META-INF/oracle/auth_1.sql | 161 | legacy-oracle-sql-resource | sysdate | create_time timestamp default sysdate, -- comment '创建时间', |
| backend/modules/com/supcon/supfusion/auth-dao/1.0.0-SNAPSHOT/META-INF/oracle/auth_1.sql | 184 | legacy-oracle-sql-resource | sysdate | create_time timestamp default sysdate, -- comment '创建时间', |
| backend/modules/com/supcon/supfusion/auth-dao/1.0.0-SNAPSHOT/META-INF/oracle/auth_1.sql | 211 | legacy-oracle-sql-resource | sysdate | create_time timestamp default sysdate, -- comment '创建时间', |
| backend/modules/com/supcon/supfusion/auth-dao/1.0.0-SNAPSHOT/META-INF/oracle/auth_1.sql | 330 | legacy-oracle-sql-resource | from-dual | insert when (not exists (select 1 from auth_user where user_name = 'admin')) then into auth_user (id,user_name,company_id,password,user_type,person_id) select 1 as id,'admin' as user_name,1000 as company_id,'$2a$10$QEd18 |
| backend/modules/com/supcon/supfusion/auth-dao/1.0.0-SNAPSHOT/META-INF/oracle/auth_1.sql | 369 | legacy-oracle-sql-resource | sysdate | create_time timestamp default sysdate,-- comment '创建时间', |
| backend/modules/com/supcon/supfusion/auth-dao/1.0.0-SNAPSHOT/META-INF/oracle/auth_1.sql | 393 | legacy-oracle-sql-resource | sysdate | create_time timestamp default sysdate, -- comment '创建时间', |
| backend/modules/com/supcon/supfusion/auth-service/1.0.0-SNAPSHOT/com/supcon/supfusion/auth/service/config/MybatisPlusConfig.java | 31 | recovered-source-backlog | oracle-keyword | if (DbType.ORACLE.getDb().equals(dbType)) { |
| backend/modules/com/supcon/supfusion/auth-service/1.0.0-SNAPSHOT/com/supcon/supfusion/auth/service/config/MybatisPlusConfig.java | 32 | recovered-source-backlog | oracle-keyword | paginationInterceptor.setDbType(DbType.ORACLE); |
| backend/modules/com/supcon/supfusion/auth-upgrade/1.0.1.RELEASE/META-INF/maven/com.supcon.supfusion/auth-upgrade/pom.xml | 41 | legacy-ojdbc-dependency | oracle-keyword | <groupId>com.oracle.jdbc</groupId> |
| backend/modules/com/supcon/supfusion/auth-upgrade/1.0.1.RELEASE/META-INF/maven/com.supcon.supfusion/auth-upgrade/pom.xml | 42 | legacy-ojdbc-dependency | ojdbc | <artifactId>ojdbc6</artifactId> |
| backend/modules/com/supcon/supfusion/auth-upgrade/1.0.1.RELEASE/com/bluetron/supos/upgrade/auth/MigrateTask.java | 326 | recovered-source-backlog | oracle-keyword | // if (targetDbType.equals("oracle")) { |
| backend/modules/com/supcon/supfusion/auth-upgrade/1.0.1.RELEASE/com/bluetron/supos/upgrade/auth/MigrateTask.java | 327 | recovered-source-backlog | oracle-keyword | // CLOB clob = oracle.sql.CLOB.createTemporary(targetConnection, false, CLOB.DURATION_SESSION); |
| backend/modules/com/supcon/supfusion/cloud-task-scheduler-server-dao/1.0.0-SNAPSHOT/META-INF/oracle/scheduler_2.sql | 2 | legacy-oracle-sql-resource | oracle-keyword | -- A HINT SUBMITTED BY A USER: ORACLE DB MUST BE CREATED AS "SHARED" AND THE |
| backend/modules/com/supcon/supfusion/cloud-task-scheduler-server-dao/1.0.0-SNAPSHOT/META-INF/oracle/scheduler_2.sql | 5 | legacy-oracle-sql-resource | oracle-keyword | -- ORACLE INSTALL, SO MOST USERS NEED NOT WORRY ABOUT THIS. |
| backend/modules/com/supcon/supfusion/configuration/configuration-services-base/1.0.0-SNAPSHOT/com/supcon/supfusion/base/services/impl/StaffServiceImpl.java | 93 | recovered-source-backlog | rownum | * rownum |
| backend/modules/com/supcon/supfusion/configuration/configuration-services-dao/1.0.0-SNAPSHOT/META-INF/oracle/configuration-init.sql | 3622 | legacy-oracle-sql-resource | sysdate | alter table EC_MODULE_RELATION MODIFY CREATE_TIME default sysdate NULL; |
| backend/modules/com/supcon/supfusion/configuration/configuration-services-dao/1.0.0-SNAPSHOT/META-INF/oracle/project-init.sql | 1144 | legacy-oracle-sql-resource | sysdate | alter table PROJECT_MODULE_RELATION MODIFY CREATE_TIME default sysdate NULL; |
| backend/modules/com/supcon/supfusion/configuration/configuration-services-dao/1.0.0-SNAPSHOT/com/supcon/supfusion/configuration/services/enums/JdbcType.java | 33 | recovered-source-backlog | oracle-keyword | CURSOR(-10), // Oracle |
| backend/modules/com/supcon/supfusion/configuration/configuration-services-open-api/1.0.0-SNAPSHOT/com/supcon/supfusion/configuration/services/openapi/vo/ModelVO.java | 52 | recovered-source-backlog | oracle-keyword | private String viewSql; // sql模型数据库视图语句 oracle;sqlserver;... |
| backend/modules/com/supcon/supfusion/configuration/configuration-services-open-api/1.0.0-SNAPSHOT/templates/model/editsql.ftl | 157 | recovered-source-backlog | oracle-keyword | <pre><code>--oracle |
| backend/modules/com/supcon/supfusion/configuration/configuration-services-open-api/1.0.0-SNAPSHOT/templates/scheduler/datasourceEdit.ftl | 32 | recovered-source-backlog | oracle-keyword | <option value="ORACLE"<#if schedulerDatasource?? && schedulerDatasource.datasourceType = "ORACLE">selected</#if>>${getText('ec.scheduler.datasource.datasourceType.oracle')}</option> |
| backend/modules/com/supcon/supfusion/configuration/configuration-services-service/1.0.0-SNAPSHOT/META-INF/keyFile/keyfile-db.txt | 6 | recovered-source-backlog | rownum | RAW ,RENAME ,RESOURCE, REVOKE, ROW, ROWID ,ROWNUM, ROWS ,SELECT ,SESSION, SET, SHARE ,SIZE ,SMALLINT, START , |
| backend/modules/com/supcon/supfusion/configuration/configuration-services-service/1.0.0-SNAPSHOT/META-INF/keyFile/keyfile-db.txt | 7 | recovered-source-backlog | sysdate | SUCCESSFUL ,SYNONYM ,SYSDATE, TABLE, THEN ,TO ,TRIGGER, UID ,UNION, UNIQUE, UPDATE, USER, VALIDATE, VALUES ,VARCHAR, |
| backend/modules/com/supcon/supfusion/configuration/configuration-services-service/1.0.0-SNAPSHOT/com/supcon/supfusion/configuration/services/service/impl/EntityServiceImpl.java | 557 | recovered-source-backlog | oracle-keyword | } else if (entityDao.getDBType() == IBaseDao.DBTYPE.ORACLE) { |
| backend/modules/com/supcon/supfusion/configuration/configuration-services-service/1.0.0-SNAPSHOT/com/supcon/supfusion/configuration/services/service/impl/ModelServiceImpl.java | 1012 | recovered-source-backlog | oracle-keyword | } else if (dbName.startsWith("oracle")) { |
| backend/modules/com/supcon/supfusion/configuration/configuration-services-service/1.0.0-SNAPSHOT/com/supcon/supfusion/configuration/services/service/impl/ModelServiceImpl.java | 1030 | recovered-source-backlog | oracle-keyword | if (dbName.startsWith("oracle")) |
| backend/modules/com/supcon/supfusion/configuration/configuration-services-service/1.0.0-SNAPSHOT/com/supcon/supfusion/configuration/services/service/impl/ModelServiceImpl.java | 1031 | recovered-source-backlog | oracle-keyword | dbName = "oracle"; |
| backend/modules/com/supcon/supfusion/configuration/configuration-services-service/1.0.0-SNAPSHOT/com/supcon/supfusion/configuration/services/service/impl/SqlModelServiceImpl.java | 78 | recovered-source-backlog | oracle-keyword | } else if ("oracle".equals(sqlModel.getCurrentDbType())) { |
| backend/modules/com/supcon/supfusion/configuration/configuration-services-service/1.0.0-SNAPSHOT/com/supcon/supfusion/configuration/services/service/impl/SqlModelServiceImpl.java | 94 | recovered-source-backlog | oracle-keyword | sqlModel.setOracleView(getDbViewSql(sqlModel.getCurrentDbSql(), "oracle", tableName, inherents, isMain)); |
| backend/modules/com/supcon/supfusion/configuration/configuration-services-service/1.0.0-SNAPSHOT/com/supcon/supfusion/configuration/services/service/impl/SqlModelServiceImpl.java | 99 | recovered-source-backlog | oracle-keyword | sqlModel.setOracleView(getDbViewSql(sqlModel.getOracleSql(), "oracle", tableName, inherents, isMain)); |
| backend/modules/com/supcon/supfusion/configuration/configuration-services-service/1.0.0-SNAPSHOT/com/supcon/supfusion/configuration/services/service/impl/SqlModelServiceImpl.java | 377 | recovered-source-backlog | oracle-keyword | if (sqlLowerCase.startsWith("--oracle") \|\| sqlLowerCase.startsWith("--sqlserver") \|\| sqlLowerCase.startsWith("--mariadb")) { |
| backend/modules/com/supcon/supfusion/configuration/configuration-services-service/1.0.0-SNAPSHOT/com/supcon/supfusion/configuration/services/service/impl/SqlModelServiceImpl.java | 391 | recovered-source-backlog | oracle-keyword | if ("oracle".equals(s3)) { |
| backend/modules/com/supcon/supfusion/configuration/configuration-services-service/1.0.0-SNAPSHOT/com/supcon/supfusion/configuration/services/service/impl/SqlModelServiceImpl.java | 402 | recovered-source-backlog | oracle-keyword | if ("oracle".equals(sqlModel.getCurrentDbType())) { |
| backend/modules/com/supcon/supfusion/configuration/configuration-services-service/1.0.0-SNAPSHOT/com/supcon/supfusion/configuration/services/service/impl/UploadInfoManagerImpl.java | 197 | recovered-source-backlog | oracle-keyword | URL oracle = new URL(url); |
| backend/modules/com/supcon/supfusion/configuration/configuration-services-service/1.0.0-SNAPSHOT/com/supcon/supfusion/configuration/services/service/impl/UploadInfoManagerImpl.java | 198 | recovered-source-backlog | oracle-keyword | yc = (HttpURLConnection) oracle.openConnection();; |
| backend/modules/com/supcon/supfusion/configuration/configuration-services-service/1.0.0-SNAPSHOT/com/supcon/supfusion/configuration/services/service/impl/ViewServiceImpl.java | 292 | recovered-source-backlog | oracle-keyword | } else if (viewDao.getDBType() == IBaseDao.DBTYPE.ORACLE) { |
| backend/modules/com/supcon/supfusion/configuration/configuration-services-service/1.0.0-SNAPSHOT/com/supcon/supfusion/configuration/services/utils/DbUtils.java | 77 | recovered-source-backlog | oracle-keyword | if (dbName.startsWith("oracle")) |
| backend/modules/com/supcon/supfusion/configuration/configuration-services-service/1.0.0-SNAPSHOT/com/supcon/supfusion/configuration/services/utils/DbUtils.java | 78 | recovered-source-backlog | oracle-keyword | dbName = "oracle"; |
| backend/modules/com/supcon/supfusion/configuration/configuration-services-service/1.0.0-SNAPSHOT/com/supcon/supfusion/configuration/services/utils/DbUtils.java | 142 | recovered-source-backlog | rownum | public T mapRow(ResultSet rs, int rowNum) throws SQLException { |
| backend/modules/com/supcon/supfusion/configuration/configuration-services-service/1.0.0-SNAPSHOT/com/supcon/supfusion/configuration/services/utils/FieldSyncDBUtils.java | 25 | recovered-source-backlog | oracle-keyword | if (dbName.startsWith("oracle")) { |
| backend/modules/com/supcon/supfusion/configuration/configuration-services-service/1.0.0-SNAPSHOT/com/supcon/supfusion/configuration/services/utils/FieldSyncDBUtils.java | 33 | recovered-source-backlog | oracle-keyword | if (dbName.startsWith("oracle")) { |
| backend/modules/com/supcon/supfusion/configuration/configuration-services-service/1.0.0-SNAPSHOT/com/supcon/supfusion/configuration/services/utils/FieldSyncDBUtils.java | 45 | recovered-source-backlog | oracle-keyword | if (dbName.startsWith("oracle")) { |
| backend/modules/com/supcon/supfusion/configuration/configuration-services-service/1.0.0-SNAPSHOT/com/supcon/supfusion/configuration/services/utils/FieldSyncDBUtils.java | 57 | recovered-source-backlog | oracle-keyword | if (dbType.startsWith("oracle")) { |
| backend/modules/com/supcon/supfusion/configuration/configuration-services-service/1.0.0-SNAPSHOT/com/supcon/supfusion/configuration/services/utils/FieldSyncDBUtils.java | 86 | recovered-source-backlog | oracle-keyword | if (dbName.startsWith("oracle")) { |
| backend/modules/com/supcon/supfusion/configuration/configuration-services-service/1.0.0-SNAPSHOT/com/supcon/supfusion/configuration/services/utils/FieldSyncDBUtils.java | 111 | recovered-source-backlog | oracle-keyword | }else if (dbName.startsWith("oracle")) { |
| backend/modules/com/supcon/supfusion/configuration/configuration-services-service/1.0.0-SNAPSHOT/com/supcon/supfusion/configuration/services/utils/ModelSyncDBUtils.java | 18 | recovered-source-backlog | oracle-keyword | if (dbName.startsWith("oracle")) { |
| backend/modules/com/supcon/supfusion/configuration/configuration-services-service/1.0.0-SNAPSHOT/com/supcon/supfusion/configuration/services/utils/ModelSyncDBUtils.java | 45 | recovered-source-backlog | oracle-keyword | if (dbName.startsWith("oracle")) { |
| backend/modules/com/supcon/supfusion/configuration/configuration-services-service/1.0.0-SNAPSHOT/com/supcon/supfusion/configuration/services/utils/ModelSyncDBUtils.java | 882 | recovered-source-backlog | oracle-keyword | if (dbName.startsWith("oracle")) { |
| backend/modules/com/supcon/supfusion/custom-property-common/1.0.0-SNAPSHOT/com/supcon/supfusion/custon/property/common/enums/JdbcType.java | 33 | recovered-source-backlog | oracle-keyword | CURSOR(-10), // Oracle |
| backend/modules/com/supcon/supfusion/custom-property-dao/1.0.0-SNAPSHOT/com/supcon/supfusion/custon/property/dao/config/MybatisPlusConfig.java | 47 | recovered-source-backlog | oracle-keyword | } else if ("oracle".equals(dataSourceConnectionProperties.getSystem().getDbType())) { |
| backend/modules/com/supcon/supfusion/custom-property-dao/1.0.0-SNAPSHOT/com/supcon/supfusion/custon/property/dao/config/MybatisPlusConfig.java | 48 | recovered-source-backlog | oracle-keyword | paginationInterceptor.setDbType(DbType.ORACLE); |
| backend/modules/com/supcon/supfusion/custom-property-dao/1.0.0-SNAPSHOT/com/supcon/supfusion/custon/property/dao/entity/Model.java | 69 | recovered-source-backlog | oracle-keyword | private String viewSql; // sql模型数据库视图语句 oracle;sqlserver;... |
| backend/modules/com/supcon/supfusion/custom/propertye/custom-property-common/1.0.0-SNAPSHOT/com/supcon/supfusion/custon/property/common/enums/JdbcType.java | 33 | recovered-source-backlog | oracle-keyword | CURSOR(-10), // Oracle |
| backend/modules/com/supcon/supfusion/custom/propertye/custom-property-dao/1.0.0-SNAPSHOT/com/supcon/supfusion/custon/property/dao/config/MybatisPlusConfig.java | 47 | recovered-source-backlog | oracle-keyword | } else if ("oracle".equals(dataSourceConnectionProperties.getSystem().getDbType())) { |
| backend/modules/com/supcon/supfusion/custom/propertye/custom-property-dao/1.0.0-SNAPSHOT/com/supcon/supfusion/custon/property/dao/config/MybatisPlusConfig.java | 48 | recovered-source-backlog | oracle-keyword | paginationInterceptor.setDbType(DbType.ORACLE); |
| backend/modules/com/supcon/supfusion/custom/propertye/custom-property-dao/1.0.0-SNAPSHOT/com/supcon/supfusion/custon/property/dao/entity/Model.java | 69 | recovered-source-backlog | oracle-keyword | private String viewSql; // sql模型数据库视图语句 oracle;sqlserver;... |
| backend/modules/com/supcon/supfusion/file-server-dao/1.0.0-SNAPSHOT/com/supcon/supfusion/file/server/dao/mapper/oracle/DocumentDao.xml | 347 | legacy-oracle-sql-resource | from-dual | FROM DUAL |

## 处理规则

- `legacy-oracle-sql-resource`：保留作原厂参考，迁移模块时必须补 PostgreSQL 主路径。
- `legacy-ojdbc-dependency`：模块提升到 `backend/source-modules` 时移除直接 ojdbc 依赖。
- `runtime-config-backlog`：Nacos 源配置里的 Oracle 默认值要继续由渲染脚本转成 PostgreSQL，并逐步改源配置。
- `runtime-patch-backlog`：当前 runtime patch 为了兼容老包可存在，源码提升后应删除 Oracle 分支。
- `recovered-source-backlog` / `decompiled-runtime-backlog`：后端专项线程按模块确认是否是真 SQL、枚举、配置还是误报。
