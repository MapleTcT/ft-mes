# PostgreSQL 迁移脚本索引

本文件由 `scripts/generate-postgres-migration-inventory.py` 生成，用于跟踪 Docker 测试环境的 PostgreSQL 初始化和兼容 SQL。

## 摘要

- 目录：`deploy/docker/postgres/init`。
- 脚本数量：`74`。
- 编号范围：`001` 到 `074`。
- 缺失编号：`[]`。
- 重复编号：`[]`。
- 高风险语句：`0`。
- 需关注语句：`44`。
- 机器可读清单：`metadata/postgres-migration-inventory.json`。

## 标签统计

| Tag | Count |
| --- | --- |
| auth-rbac-org | 28 |
| business | 9 |
| compatibility | 42 |
| configuration | 18 |
| general | 3 |
| notification | 9 |
| platform | 21 |
| workflow | 6 |

## 语句统计

| Statement | Count |
| --- | --- |
| alter-table | 9181 |
| create-function | 38 |
| create-index | 667 |
| create-table | 482 |
| create-view | 92 |
| insert | 3662 |
| update | 3651 |

## 脚本清单

| No. | File | Tags | Lines | DDL/DML Summary | Watch |
| --- | --- | --- | --- | --- | --- |
| 001 | 001-adp-postgres-compat.sql | compatibility | 108 | create-table:1, create-function:6 | watch:6 |
| 002 | 002-sms-jincang.sql | notification | 26 | create-table:1 | - |
| 003 | 003-core-missing-tables.sql | general | 502 | create-table:24, create-index:30, insert:7, update:4 | - |
| 004 | 004-auth-org-user.sql | auth-rbac-org | 112 | create-table:1, create-view:1, create-index:4, alter-table:2, insert:1, update:1 | - |
| 005 | 005-rbac-init-version.sql | auth-rbac-org | 12 | create-table:1, insert:1 | - |
| 006 | 006-rbac-auth-fixups.sql | auth-rbac-org, compatibility | 410 | create-table:10, create-index:22, alter-table:15, insert:1 | - |
| 007 | 007-rbac-boolean-columns.sql | auth-rbac-org, compatibility | 97 | alter-table:3 | watch:3 |
| 008 | 008-rbac-boolean-aggregate.sql | auth-rbac-org, compatibility | 21 | create-function:1 | watch:1 |
| 009 | 009-rbac-boolean-integer-operators.sql | auth-rbac-org, compatibility | 93 | create-function:6 | watch:4 |
| 010 | 010-rbac-boolean-minmax-aggregates.sql | auth-rbac-org, compatibility | 45 | create-function:2 | watch:2 |
| 011 | 011-rbac-userpermission-boolean-fixups.sql | auth-rbac-org, compatibility | 111 | create-view:1, alter-table:3 | watch:1 |
| 012 | 012-rbac-userpermission-menuoperate-code.sql | platform, auth-rbac-org | 15 | update:1 | - |
| 013 | 013-page-runtime-schema-fixups.sql | platform, compatibility | 409 | create-table:8, create-view:2, create-index:7, alter-table:2, insert:5, update:6 | - |
| 014 | 014-ui-smoke-runtime-fixups.sql | platform, compatibility | 570 | create-table:8, create-view:3, create-function:3, create-index:7, alter-table:3, insert:4, update:7 | watch:3 |
| 015 | 015-i18n-dingtalk-startup.sql | platform, notification | 175 | create-table:9, create-index:6, insert:1, update:4 | - |
| 016 | 016-notification-printer-runtime-fixups.sql | platform, notification, compatibility | 625 | create-table:29, create-index:18, alter-table:2, insert:7, update:2 | - |
| 017 | 017-rbac-menuoperate-url-ref-fixups.sql | platform, auth-rbac-org, compatibility | 241 | create-table:6, create-view:4, create-index:9, alter-table:4, insert:8, update:1 | watch:3 |
| 018 | 018-org-mnecode-fixups.sql | auth-rbac-org, configuration, compatibility | 102 | create-table:4, create-view:4 | watch:4 |
| 019 | 019-auth-online-user-fixups.sql | auth-rbac-org, compatibility | 46 | create-table:1, create-index:3, alter-table:19 | - |
| 020 | 020-iam-account-fixups.sql | compatibility | 55 | create-table:1, create-index:2, alter-table:12, insert:1 | - |
| 021 | 021-notification-postgres-idempotent-fixups.sql | notification, compatibility | 264 | create-table:3, create-function:5, create-index:4, update:6 | watch:5 |
| 022 | 022-notification-topic-rel-type-fixups.sql | notification, compatibility | 42 | create-index:1, alter-table:3 | - |
| 023 | 023-auth-user-role-fixups.sql | auth-rbac-org, compatibility | 63 | create-table:1, create-index:3, insert:1, update:1 | - |
| 024 | 024-auth-login-config-fixups.sql | auth-rbac-org, compatibility | 37 | create-table:1, insert:1, update:1 | - |
| 025 | 025-notification-default-iam-account.sql | notification | 37 | insert:1, update:2 | - |
| 026 | 026-notification-pending-topic-bindings.sql | workflow, notification | 42 | insert:1 | - |
| 027 | 027-login-theme-runtime-fixups.sql | platform, compatibility | 116 | create-table:2, insert:2, update:2 | - |
| 028 | 028-auth-user-directory-fixups.sql | auth-rbac-org, configuration, compatibility | 33 | create-table:1, create-index:2 | - |
| 029 | 029-theme-portal-loginlog-fixups.sql | platform, compatibility | 141 | create-table:6, create-index:2, alter-table:4, insert:1, update:3 | - |
| 030 | 030-rbac-menu-validity-fixups.sql | platform, auth-rbac-org, compatibility | 10 | update:1 | - |
| 031 | 031-rbac-menu-collection-fixups.sql | platform, auth-rbac-org, configuration, compatibility | 30 | create-table:1, create-index:3 | - |
| 032 | 032-organization-base-company-views.sql | auth-rbac-org, configuration | 93 | create-view:2 | - |
| 033 | 033-test-admin-usability-fixups.sql | compatibility | 8 | update:1 | - |
| 034 | 034-workflow-ec-table-info-fixups.sql | workflow, configuration, compatibility | 40 | create-table:1, create-index:3 | - |
| 035 | 035-organization-core-base-views.sql | auth-rbac-org, configuration | 360 | create-view:4 | - |
| 036 | 036-notification-stationletter-config.sql | notification | 32 | insert:1, update:1 | - |
| 037 | 037-platform-page-smoke-fixups.sql | platform, compatibility | 369 | create-table:15, create-view:1, create-index:3, alter-table:15 | watch:1 |
| 038 | 038-i18n-language-name-resource-fixups.sql | platform, compatibility | 31 | insert:1 | - |
| 039 | 039-platform-menu-full-smoke-fixups.sql | platform, compatibility | 586 | create-table:11, create-view:5, create-index:15, alter-table:6, insert:2, update:2 | watch:5 |
| 040 | 040-notification-i18n-resource-fixups.sql | platform, notification, compatibility | 31 | insert:1 | - |
| 041 | 041-workflow-pending-source-fixups.sql | workflow, compatibility | 19 | create-index:1, alter-table:2, update:1 | - |
| 042 | 042-configuration-module-import-fixups.sql | configuration, compatibility | 109 | create-table:3, create-index:3, alter-table:1, insert:1 | - |
| 043 | 043-configuration-design-tables-from-vendor.sql | configuration | 3069 | create-table:121, create-function:1, create-index:1 | - |
| 044 | 044-configuration-design-flag-types.sql | configuration | 220 | alter-table:3 | - |
| 045 | 045-configuration-view-mobile-version.sql | configuration | 14 | alter-table:4 | - |
| 046 | 046-configuration-property-newer-fields.sql | configuration | 16 | alter-table:2 | - |
| 047 | 047-configuration-sql-model-dm-sql.sql | configuration | 4 | alter-table:1 | - |
| 048 | 048-rbac-menu-company-ref-compat.sql | platform, auth-rbac-org, compatibility | 18 | create-index:1, alter-table:2, update:1 | - |
| 049 | 049-rbac-menuoperate-valid-integer.sql | platform, auth-rbac-org | 89 | create-view:1, alter-table:1 | watch:1 |
| 050 | 050-postgres-boolean-smallint-assignment-cast.sql | compatibility | 26 | create-function:1 | - |
| 051 | 051-rbac-deployment-company-ref-ec.sql | auth-rbac-org, configuration | 15 | create-table:1, create-index:2, insert:1 | - |
| 052 | 052-module-company-ref.sql | general | 13 | create-table:1, create-index:2 | - |
| 053 | 053-postgres-boolean-integer-assignment-cast.sql | compatibility | 31 | create-function:1, update:1 | - |
| 054 | 054-scheduler-job-callback-flag.sql | workflow | 10 | alter-table:1, update:1 | - |
| 055 | 055-scheduler-quartz-runtime-compat.sql | platform, workflow, compatibility | 43 | create-table:1, alter-table:4 | - |
| 056 | 056-runtime-metadata-from-ec-compat.sql | platform, configuration, compatibility | 170 | create-function:1, alter-table:3, insert:1, update:2 | - |
| 057 | 057-latest-foundation-newmods-business-schema.sql | business | 3311 | create-table:50, create-index:39, alter-table:1558 | - |
| 058 | 058-sysbase-core-runtime-metadata.sql | platform, business | 183 | insert:4 | - |
| 059 | 059-configuration-script-code-compat.sql | configuration, compatibility | 5 | alter-table:1 | - |
| 060 | 060-sysbase-department-reference-views.sql | configuration, business | 306 | insert:4 | - |
| 061 | 061-sysbase-staff-reference-view.sql | configuration, business | 293 | insert:7 | - |
| 062 | 062-wts-business-schema-from-initxml.sql | business | 2784 | create-table:64, create-index:192, alter-table:2460 | - |
| 063 | 063-disable-legacy-external-scheduler-jobs.sql | workflow | 33 | update:2 | watch:1 |
| 064 | 064-craftgraph-business-schema-from-initxml.sql | business | 1329 | create-table:32, create-index:96, alter-table:1165 | - |
| 065 | 065-business-view-runtime-json.sql | platform, configuration, business | 184083 | create-table:58, create-view:63, create-function:11, create-index:172, alter-table:3868, insert:3591, update:3591 | watch:4 |
| 066 | 066-lims-qcs-test-config.sql | business | 27 | insert:1, update:1 | - |
| 067 | 067-qcs-table-types-postgres.sql | business | 118 | create-table:1, create-index:3, alter-table:2, insert:2 | - |
| 068 | 068-organization-position-role-compat.sql | auth-rbac-org, compatibility | 33 | create-table:1, create-view:1, create-index:3 | - |
| 069 | 069-organization-manager-compat.sql | auth-rbac-org, compatibility | 21 | create-table:1, create-index:3 | - |
| 070 | 070-test-admin-default-password.sql | general | 47 | create-table:1, insert:1, update:1 | - |
| 071 | 071-organization-person-profile-compat.sql | auth-rbac-org, compatibility | 20 | create-index:3, alter-table:7 | - |
| 072 | 072-rbac-authority-page-compat.sql | platform, auth-rbac-org, compatibility | 43 | create-table:1, create-index:2, alter-table:1, insert:1, update:2 | - |
| 073 | 073-auth-user-lock-status-compat.sql | auth-rbac-org, compatibility | 10 | alter-table:1, update:2 | - |
| 074 | 074-rbac-roleuser-valid-default.sql | auth-rbac-org | 11 | alter-table:1 | - |

## 规则

- 文件名必须是 `NNN-lowercase-slug.sql`。
- 编号必须连续、唯一，新增脚本追加到末尾。
- 默认脚本必须可重复执行，优先使用 `IF EXISTS` / `IF NOT EXISTS` / `ON CONFLICT` / `DO $$` 保护。
- `DROP DATABASE`、`DROP SCHEMA`、`DROP TABLE`、`TRUNCATE` 属于高风险语句，会导致 `make postgres-migration-check` 失败。
- `DROP VIEW`、`DROP FUNCTION`、`DROP TRIGGER`、`DROP OPERATOR`、`DELETE FROM` 会进入 watch 清单，必须在 PR 中解释原因。
