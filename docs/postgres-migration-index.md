# PostgreSQL 迁移脚本索引

本文件由 `scripts/generate-postgres-migration-inventory.py` 生成，用于跟踪 Docker 测试环境的 PostgreSQL 初始化和兼容 SQL。

## 摘要

- 目录：`deploy/docker/postgres/init`。
- 脚本数量：`170`。
- 编号范围：`001` 到 `170`。
- 缺失编号：`[]`。
- 重复编号：`[]`。
- 高风险语句：`0`。
- 未保护结构语句：`0`。
- 需关注语句：`60`。
- 需关注语句安全问题：`0`。
- 机器可读清单：`metadata/postgres-migration-inventory.json`。
- 需关注语句说明：`docs/postgres-migration-watch-rationale.md`。

## 标签统计

| Tag | Count |
| --- | --- |
| auth-rbac-org | 36 |
| business | 33 |
| compatibility | 73 |
| configuration | 41 |
| general | 22 |
| notification | 9 |
| platform | 48 |
| workflow | 19 |

## 语句统计

| Statement | Count |
| --- | --- |
| alter-table | 9324 |
| create-function | 67 |
| create-index | 840 |
| create-table | 523 |
| create-view | 107 |
| insert | 3874 |
| update | 3931 |

## 幂等信号统计

| Signal | Count |
| --- | --- |
| create-or-replace | 171 |
| do-block | 272 |
| if-exists | 88 |
| if-not-exists | 11246 |
| on-conflict | 3838 |
| to-regclass | 152 |
| where-not-exists | 26 |

## 脚本清单

| No. | File | Tags | Lines | DDL/DML Summary | Idempotency | Watch |
| --- | --- | --- | --- | --- | --- | --- |
| 001 | 001-adp-postgres-compat.sql | compatibility | 108 | create-table:1, create-function:6 | 20 | watch:6 |
| 002 | 002-sms-jincang.sql | notification | 26 | create-table:1 | 1 | - |
| 003 | 003-core-missing-tables.sql | general | 502 | create-table:24, create-index:30, insert:7, update:4 | 61 | - |
| 004 | 004-auth-org-user.sql | auth-rbac-org | 112 | create-table:1, create-view:1, create-index:4, alter-table:2, insert:1, update:1 | 9 | - |
| 005 | 005-rbac-init-version.sql | auth-rbac-org | 12 | create-table:1, insert:1 | 2 | - |
| 006 | 006-rbac-auth-fixups.sql | auth-rbac-org, compatibility | 410 | create-table:10, create-index:22, alter-table:15, insert:1 | 40 | - |
| 007 | 007-rbac-boolean-columns.sql | auth-rbac-org, compatibility | 97 | alter-table:3 | 4 | watch:3 |
| 008 | 008-rbac-boolean-aggregate.sql | auth-rbac-org, compatibility | 21 | create-function:1 | 2 | watch:1 |
| 009 | 009-rbac-boolean-integer-operators.sql | auth-rbac-org, compatibility | 93 | create-function:6 | 10 | watch:4 |
| 010 | 010-rbac-boolean-minmax-aggregates.sql | auth-rbac-org, compatibility | 45 | create-function:2 | 4 | watch:2 |
| 011 | 011-rbac-userpermission-boolean-fixups.sql | auth-rbac-org, compatibility | 111 | create-view:1, alter-table:3 | 3 | watch:1 |
| 012 | 012-rbac-userpermission-menuoperate-code.sql | platform, auth-rbac-org | 15 | update:1 | 3 | - |
| 013 | 013-page-runtime-schema-fixups.sql | platform, compatibility | 409 | create-table:8, create-view:2, create-index:7, alter-table:2, insert:5, update:6 | 27 | - |
| 014 | 014-ui-smoke-runtime-fixups.sql | platform, compatibility | 574 | create-table:8, create-view:3, create-function:3, create-index:7, alter-table:3, insert:4, update:7 | 47 | watch:3 |
| 015 | 015-i18n-dingtalk-startup.sql | platform, notification | 175 | create-table:9, create-index:6, insert:1, update:4 | 16 | - |
| 016 | 016-notification-printer-runtime-fixups.sql | platform, notification, compatibility | 628 | create-table:29, create-index:18, alter-table:2, insert:7, update:2 | 60 | - |
| 017 | 017-rbac-menuoperate-url-ref-fixups.sql | platform, auth-rbac-org, compatibility | 241 | create-table:6, create-view:4, create-index:9, alter-table:4, insert:8, update:1 | 42 | watch:3 |
| 018 | 018-org-mnecode-fixups.sql | auth-rbac-org, configuration, compatibility | 102 | create-table:4, create-view:4 | 12 | watch:4 |
| 019 | 019-auth-online-user-fixups.sql | auth-rbac-org, compatibility | 46 | create-table:1, create-index:3, alter-table:19 | 23 | - |
| 020 | 020-iam-account-fixups.sql | compatibility | 55 | create-table:1, create-index:2, alter-table:12, insert:1 | 16 | - |
| 021 | 021-notification-postgres-idempotent-fixups.sql | notification, compatibility | 265 | create-table:3, create-function:5, create-index:4, update:6 | 24 | watch:5 |
| 022 | 022-notification-topic-rel-type-fixups.sql | notification, compatibility | 42 | create-index:1, alter-table:3 | 7 | - |
| 023 | 023-auth-user-role-fixups.sql | auth-rbac-org, compatibility | 63 | create-table:1, create-index:3, insert:1, update:1 | 5 | - |
| 024 | 024-auth-login-config-fixups.sql | auth-rbac-org, compatibility | 37 | create-table:1, insert:1, update:1 | 2 | - |
| 025 | 025-notification-default-iam-account.sql | notification | 37 | insert:1, update:2 | 1 | - |
| 026 | 026-notification-pending-topic-bindings.sql | workflow, notification | 42 | insert:1 | 1 | - |
| 027 | 027-login-theme-runtime-fixups.sql | platform, compatibility | 116 | create-table:2, insert:2, update:2 | 4 | - |
| 028 | 028-auth-user-directory-fixups.sql | auth-rbac-org, configuration, compatibility | 33 | create-table:1, create-index:2 | 3 | - |
| 029 | 029-theme-portal-loginlog-fixups.sql | platform, compatibility | 141 | create-table:6, create-index:2, alter-table:4, insert:1, update:3 | 18 | - |
| 030 | 030-rbac-menu-validity-fixups.sql | platform, auth-rbac-org, compatibility | 10 | update:1 | 0 | - |
| 031 | 031-rbac-menu-collection-fixups.sql | platform, auth-rbac-org, configuration, compatibility | 30 | create-table:1, create-index:3 | 4 | - |
| 032 | 032-organization-base-company-views.sql | auth-rbac-org, configuration | 93 | create-view:2 | 2 | - |
| 033 | 033-test-admin-usability-fixups.sql | compatibility | 8 | update:1 | 0 | - |
| 034 | 034-workflow-ec-table-info-fixups.sql | workflow, configuration, compatibility | 40 | create-table:1, create-index:3 | 4 | - |
| 035 | 035-organization-core-base-views.sql | auth-rbac-org, configuration | 360 | create-view:4 | 4 | - |
| 036 | 036-notification-stationletter-config.sql | notification | 32 | insert:1, update:1 | 1 | - |
| 037 | 037-platform-page-smoke-fixups.sql | platform, compatibility | 369 | create-table:15, create-view:1, create-index:3, alter-table:15 | 21 | watch:1 |
| 038 | 038-i18n-language-name-resource-fixups.sql | platform, compatibility | 31 | insert:1 | 1 | - |
| 039 | 039-platform-menu-full-smoke-fixups.sql | platform, compatibility | 586 | create-table:11, create-view:5, create-index:15, alter-table:6, insert:2, update:2 | 81 | watch:5 |
| 040 | 040-notification-i18n-resource-fixups.sql | platform, notification, compatibility | 31 | insert:1 | 1 | - |
| 041 | 041-workflow-pending-source-fixups.sql | workflow, compatibility | 19 | create-index:1, alter-table:2, update:1 | 2 | - |
| 042 | 042-configuration-module-import-fixups.sql | configuration, compatibility | 109 | create-table:3, create-index:3, alter-table:1, insert:1 | 11 | - |
| 043 | 043-configuration-design-tables-from-vendor.sql | configuration | 3069 | create-table:121, create-function:1, create-index:1 | 126 | - |
| 044 | 044-configuration-design-flag-types.sql | configuration | 220 | alter-table:3 | 1 | - |
| 045 | 045-configuration-view-mobile-version.sql | configuration | 14 | alter-table:4 | 4 | - |
| 046 | 046-configuration-property-newer-fields.sql | configuration | 16 | alter-table:2 | 12 | - |
| 047 | 047-configuration-sql-model-dm-sql.sql | configuration | 4 | alter-table:1 | 1 | - |
| 048 | 048-rbac-menu-company-ref-compat.sql | platform, auth-rbac-org, compatibility | 18 | create-index:1, alter-table:2, update:1 | 3 | - |
| 049 | 049-rbac-menuoperate-valid-integer.sql | platform, auth-rbac-org | 89 | create-view:1, alter-table:1 | 4 | watch:1 |
| 050 | 050-postgres-boolean-smallint-assignment-cast.sql | compatibility | 26 | create-function:1 | 3 | - |
| 051 | 051-rbac-deployment-company-ref-ec.sql | auth-rbac-org, configuration | 15 | create-table:1, create-index:2, insert:1 | 3 | - |
| 052 | 052-module-company-ref.sql | general | 13 | create-table:1, create-index:2 | 3 | - |
| 053 | 053-postgres-boolean-integer-assignment-cast.sql | compatibility | 31 | create-function:1, update:1 | 3 | - |
| 054 | 054-scheduler-job-callback-flag.sql | workflow | 10 | alter-table:1, update:1 | 2 | - |
| 055 | 055-scheduler-quartz-runtime-compat.sql | platform, workflow, compatibility | 43 | create-table:1, alter-table:4 | 7 | - |
| 056 | 056-runtime-metadata-from-ec-compat.sql | platform, configuration, compatibility | 170 | create-function:1, alter-table:3, insert:1, update:2 | 8 | - |
| 057 | 057-latest-foundation-newmods-business-schema.sql | business | 3311 | create-table:50, create-index:39, alter-table:1558 | 1647 | - |
| 058 | 058-sysbase-core-runtime-metadata.sql | platform, business | 183 | insert:4 | 4 | - |
| 059 | 059-configuration-script-code-compat.sql | configuration, compatibility | 5 | alter-table:1 | 2 | - |
| 060 | 060-sysbase-department-reference-views.sql | configuration, business | 306 | insert:4 | 4 | - |
| 061 | 061-sysbase-staff-reference-view.sql | configuration, business | 293 | insert:7 | 7 | - |
| 062 | 062-wts-business-schema-from-initxml.sql | business | 2784 | create-table:64, create-index:192, alter-table:2460 | 2716 | - |
| 063 | 063-disable-legacy-external-scheduler-jobs.sql | workflow | 33 | update:2 | 0 | watch:1 |
| 064 | 064-craftgraph-business-schema-from-initxml.sql | business | 1329 | create-table:32, create-index:96, alter-table:1165 | 1293 | - |
| 065 | 065-business-view-runtime-json.sql | platform, configuration, business | 184083 | create-table:58, create-view:63, create-function:11, create-index:172, alter-table:3868, insert:3591, update:3591 | 8013 | watch:4 |
| 066 | 066-lims-qcs-test-config.sql | business | 27 | insert:1, update:1 | 1 | - |
| 067 | 067-qcs-table-types-postgres.sql | business | 118 | create-table:1, create-index:3, alter-table:2, insert:2 | 12 | - |
| 068 | 068-organization-position-role-compat.sql | auth-rbac-org, compatibility | 33 | create-table:1, create-view:1, create-index:3 | 5 | - |
| 069 | 069-organization-manager-compat.sql | auth-rbac-org, compatibility | 21 | create-table:1, create-index:3 | 4 | - |
| 070 | 070-test-admin-default-password.sql | general | 47 | create-table:1, insert:1, update:1 | 2 | - |
| 071 | 071-organization-person-profile-compat.sql | auth-rbac-org, compatibility | 20 | create-index:3, alter-table:7 | 10 | - |
| 072 | 072-rbac-authority-page-compat.sql | platform, auth-rbac-org, compatibility | 43 | create-table:1, create-index:2, alter-table:1, insert:1, update:2 | 7 | - |
| 073 | 073-auth-user-lock-status-compat.sql | auth-rbac-org, compatibility | 10 | alter-table:1, update:2 | 1 | - |
| 074 | 074-rbac-roleuser-valid-default.sql | auth-rbac-org | 11 | alter-table:1 | 2 | - |
| 075 | 075-rbac-data-resource-permission-tables.sql | auth-rbac-org | 198 | create-table:4, create-function:1, create-index:5, alter-table:4, insert:1, update:3 | 12 | watch:1 |
| 076 | 076-wom-action-view-runtime-json.sql | platform, configuration | 160 | insert:10, update:10 | 15 | - |
| 077 | 077-wom-reference-view-runtime-json.sql | platform, configuration | 222 | insert:14, update:14 | 21 | - |
| 078 | 078-wom-list-button-runtime-json.sql | platform | 67 | insert:4, update:4 | 6 | - |
| 079 | 079-wom-wait-put-records-table.sql | configuration | 167 | create-table:1, create-index:5, alter-table:2 | 10 | - |
| 080 | 080-wom-wait-record-state-sync-trigger.sql | configuration | 84 | create-function:2, update:3 | 4 | watch:2 |
| 081 | 081-wom-task-act-itemss-table.sql | general | 86 | create-table:1, create-index:4, alter-table:2, update:1 | 9 | - |
| 082 | 082-wom-pro-check-details-table.sql | configuration | 104 | create-table:1, create-index:3, alter-table:2 | 8 | - |
| 083 | 083-postgres-legacy-boolean-assignment-cast.sql | compatibility | 16 | update:2 | 0 | - |
| 084 | 084-wom-output-common-task-edit-runtime-json.sql | platform | 36 | insert:2, update:2 | 3 | - |
| 085 | 085-wom-output-common-task-edit-view-linkage.sql | configuration | 38 | update:3 | 0 | - |
| 086 | 086-wom-prepare-need-ref-table.sql | general | 105 | create-table:1, create-index:4, alter-table:2 | 9 | - |
| 087 | 087-wom-prepare-need-deal-info-table.sql | general | 97 | create-table:1, create-index:4, alter-table:2 | 9 | - |
| 088 | 088-wom-workflow-current-version.sql | workflow | 37 | create-index:1, update:1 | 1 | - |
| 089 | 089-wf-deployment-clob-oid-compat.sql | compatibility | 85 | alter-table:4, update:3 | 4 | - |
| 090 | 090-wom-preneed-flow-task-transition.sql | workflow | 156 | create-index:2, insert:2, update:2 | 4 | - |
| 091 | 091-jbpm4-lob-oid-compat.sql | compatibility | 29 | alter-table:1 | 1 | - |
| 092 | 092-wf-task-runtime-column-compat.sql | platform, compatibility | 8 | alter-table:1 | 1 | - |
| 093 | 093-rbac-modify-permission-log-table.sql | auth-rbac-org | 53 | create-table:1, create-index:2, alter-table:1 | 21 | - |
| 094 | 094-rbac-menuoperate-foundation-boolean-casts.sql | platform, auth-rbac-org, business, compatibility | 86 | create-view:1, alter-table:1 | 5 | watch:1 |
| 095 | 095-rbac-flow-permission-foundation-boolean-casts.sql | auth-rbac-org, workflow, business, compatibility | 55 | create-view:2 | 5 | watch:2 |
| 096 | 096-workflow-task-pending-memo-column.sql | workflow | 6 | alter-table:1 | 1 | - |
| 097 | 097-jbpm-hasvars-boolean-compat.sql | compatibility | 47 | alter-table:2 | 1 | - |
| 098 | 098-wom-active-execsort-convert-compat.sql | configuration, compatibility | 19 | create-function:1, alter-table:1 | 3 | - |
| 099 | 099-wom-producetask-supervision-view-compat.sql | configuration, compatibility | 40 | create-view:1 | 4 | - |
| 100 | 100-wom-batch-active-runtime-buttons.sql | platform | 261 | create-function:1, update:2 | 1 | - |
| 101 | 101-wom-output-finish-num-sync.sql | general | 76 | create-function:3, update:3 | 5 | watch:2 |
| 102 | 102-wom-easy-active-runtime-report-button.sql | platform | 187 | create-function:1, update:2 | 1 | - |
| 103 | 103-wom-batch-process-runtime-buttons.sql | platform | 318 | update:2 | 0 | - |
| 104 | 104-rm-process-actives-table.sql | general | 180 | create-table:1, create-index:5, alter-table:2 | 10 | - |
| 105 | 105-wom-process-procsort-convert-compat.sql | compatibility | 20 | create-function:1, alter-table:1 | 3 | - |
| 106 | 106-wom-process-wait-end-time-sync.sql | general | 61 | create-function:1, update:3 | 3 | watch:2 |
| 107 | 107-qcs-inspect-detail-tables.sql | configuration, business | 280 | create-table:4, create-view:1, create-index:21, alter-table:18 | 229 | - |
| 108 | 108-rm-formula-qualities-table.sql | general | 110 | create-table:1, create-index:5, alter-table:2 | 10 | - |
| 109 | 109-limsbasic-qcs-inspection-support-tables.sql | configuration, business | 179 | create-table:1, create-index:16, alter-table:6 | 97 | - |
| 110 | 110-qcs-manu-inspect-workflow-config.sql | workflow, configuration, business | 105 | create-index:1, insert:1, update:2 | 2 | - |
| 111 | 111-limsbasic-quality-std-custom-columns.sql | business | 19 | alter-table:1 | 9 | - |
| 112 | 112-qcs-table-types-lob-oid-compat.sql | business, compatibility | 51 | alter-table:2, update:1 | 5 | - |
| 113 | 113-qcs-manu-inspect-jbpm-definition.sql | configuration, business | 668 | create-index:3, insert:9, update:10 | 12 | - |
| 114 | 114-qcs-deal-info-column-compat.sql | business, compatibility | 45 | create-index:3, alter-table:19 | 23 | - |
| 115 | 115-wom-wait-put-records-lob-oid-compat.sql | configuration, compatibility | 38 | alter-table:2, update:1 | 2 | - |
| 116 | 116-wom-produce-task-lob-oid-compat.sql | compatibility | 51 | alter-table:2, update:1 | 2 | - |
| 117 | 117-qcs-report-generation-support.sql | business | 374 | create-table:3, create-index:15, alter-table:4, insert:4, update:5 | 174 | - |
| 118 | 118-postgres-legacy-text-bytea-operator.sql | general | 85 | create-function:2 | 5 | - |
| 119 | 119-qcs-inspect-report-edit-runtime-json.sql | platform, configuration, business | 36 | insert:2, update:2 | 3 | - |
| 120 | 120-baseset-act-bat-state-seed.sql | general | 116 | insert:1, update:1 | 1 | - |
| 121 | 121-qcs-unqualified-deal-workflow-config.sql | workflow, business | 337 | create-index:3, insert:4, update:5 | 7 | - |
| 122 | 122-qcs-unqualified-deal-runtime-compat.sql | platform, business, compatibility | 108 | create-view:1, insert:4, update:4 | 10 | - |
| 123 | 123-wom-qcs-null-boolean-compat.sql | business, compatibility | 10 | alter-table:1, update:1 | 0 | - |
| 124 | 124-qcs-inspect-release-runtime-json.sql | platform, configuration, business | 67 | insert:4, update:4 | 6 | - |
| 125 | 125-qcs-inspect-release-action-compat.sql | configuration, business, compatibility | 455 | create-table:1, create-view:1, create-index:10, alter-table:1, insert:4, update:5 | 85 | - |
| 126 | 126-business-edit-view-runtime-json.sql | platform, configuration, business | 129 | insert:8, update:8 | 12 | - |
| 127 | 127-workappointment-action-edit-runtime-json.sql | platform | 36 | insert:2, update:2 | 3 | - |
| 128 | 128-rm-batch-sync-test-system.sql | general | 72 | insert:1, update:2 | 1 | - |
| 129 | 129-rm-batch-system-config.sql | general | 133 | insert:5, update:5 | 5 | - |
| 130 | 130-rm-formula-child-tables.sql | general | 220 | create-table:2, create-index:4, alter-table:4 | 13 | - |
| 131 | 131-rm-formula-supervision-dealinfo-compat.sql | compatibility | 115 | create-table:2, create-view:1, create-index:4, alter-table:12 | 21 | - |
| 132 | 132-rm-formula-mne-code-table.sql | general | 50 | create-table:1, create-index:2, alter-table:2 | 7 | - |
| 133 | 133-rm-batch-workflow-jbpm-definition.sql | workflow | 486 | create-index:3, insert:7, update:8 | 10 | - |
| 134 | 134-rm-batch-workflow-permission.sql | auth-rbac-org, workflow | 96 | create-index:1, insert:1, update:1 | 2 | - |
| 135 | 135-rm-formula-dealinfo-column-compat.sql | compatibility | 59 | create-index:3, alter-table:2 | 5 | - |
| 136 | 136-workappointment-workplan-workflow-config.sql | workflow | 597 | create-table:1, create-view:1, create-function:1, create-index:10, alter-table:12, insert:6, update:8 | 34 | watch:3 |
| 137 | 137-wom-reject-material-workflow-config.sql | workflow, configuration | 656 | create-table:1, create-view:1, create-function:1, create-index:11, alter-table:12, insert:6, update:8 | 35 | watch:3 |
| 138 | 138-wom-reject-material-edit-runtime-json.sql | platform, configuration | 129 | insert:8, update:8 | 12 | - |
| 139 | 139-baseset-qcs-deal-way-state-seed.sql | business | 117 | insert:1, update:1 | 1 | - |
| 140 | 140-qcs-unqlf-deal-flag-default.sql | business | 26 | alter-table:1, update:1 | 2 | - |
| 141 | 141-rm-formula-import-template.sql | general | 218 | insert:10, update:12 | 14 | - |
| 142 | 142-postgres-legacy-text-boolean-assignment-cast.sql | compatibility | 39 | update:1 | 2 | - |
| 143 | 143-teaminfo-runtime-json.sql | platform | 253 | insert:16, update:16 | 24 | - |
| 144 | 144-teaminfo-admin-role-permissions.sql | auth-rbac-org | 83 | insert:1 | 1 | - |
| 145 | 145-teaminfo-runtime-button-permission-compat.sql | platform, auth-rbac-org, compatibility | 57 | update:2 | 1 | - |
| 146 | 146-teaminfo-smoke-prerequisites.sql | platform | 149 | insert:5, update:4 | 1 | - |
| 147 | 147-wom-pro-check-details-lob-oid-compat.sql | configuration, compatibility | 37 | alter-table:2, update:1 | 2 | - |
| 148 | 148-runtime-visible-business-buttons.sql | platform, business | 298 | create-function:2, update:3 | 3 | - |
| 149 | 149-workappointment-i18n-resource-fixups.sql | platform, compatibility | 28 | insert:1 | 1 | - |
| 150 | 150-craftgraph-visible-modify-button.sql | business | 198 | create-function:2, update:4 | 3 | - |
| 151 | 151-workappointment-workplan-list-runtime-compat.sql | platform, compatibility | 339 | create-table:5, insert:10, update:9 | 11 | - |
| 152 | 152-wts-firework-workflow-config.sql | workflow, business | 472 | create-index:3, insert:6, update:7 | 9 | - |
| 153 | 153-wts-firework-runtime-json.sql | platform, business | 439 | insert:28, update:28 | 42 | - |
| 154 | 154-runtime-view-is-shadow-integer-compat.sql | platform, configuration, compatibility | 30 | alter-table:1 | 1 | - |
| 155 | 155-postgres-legacy-text-bigint-like-operator.sql | general | 71 | create-function:2 | 5 | - |
| 156 | 156-workappointment-work-actions-compat.sql | compatibility | 111 | create-table:1, create-index:2, alter-table:1 | 49 | - |
| 157 | 157-workappointment-workplan-approve-runtime-json.sql | platform | 87 | insert:5, update:5 | 6 | - |
| 158 | 158-workappointment-workplan-picture-upload-compat.sql | compatibility | 99 | create-function:1, update:3 | 2 | - |
| 159 | 159-wom-process-unit-edit-runtime-json.sql | platform | 64 | insert:2, update:5 | 3 | - |
| 160 | 160-rm-batch-formula-edit-view-runtime-json.sql | platform, configuration | 67 | insert:4, update:4 | 6 | - |
| 161 | 161-wom-make-task-flow-workflow-config.sql | workflow | 258 | create-index:3, insert:3, update:4 | 6 | - |
| 162 | 162-wom-task-material-quality-tables.sql | general | 229 | create-table:2, create-index:6, alter-table:4 | 15 | - |
| 163 | 163-wom-make-task-flow-jbpm-definition.sql | workflow | 296 | create-index:1, insert:4, update:6 | 5 | - |
| 164 | 164-wom-produce-task-deal-info-table.sql | general | 129 | create-table:1, create-index:4, alter-table:3 | 9 | - |
| 165 | 165-wts-workpermit-list-runtime-compat.sql | platform, business, compatibility | 294 | insert:5, update:8 | 8 | - |
| 166 | 166-postgres-legacy-bigint-text-like-operator.sql | general | 116 | create-function:4 | 9 | - |
| 167 | 167-postgres-legacy-varchar-text-like-operator.sql | general | 65 | create-function:2 | 5 | - |
| 168 | 168-wom-maketasklist-toolbar-interaction-compat.sql | compatibility | 252 | create-function:1, insert:1, update:4 | 3 | - |
| 169 | 169-custom-property-project-property-compat.sql | configuration, compatibility | 10 | create-view:1 | 1 | - |
| 170 | 170-rbac-pstaff-pposition-compat.sql | auth-rbac-org, compatibility | 104 | create-table:4, create-view:4, create-index:4 | 12 | - |

## 规则

- 文件名必须是 `NNN-lowercase-slug.sql`。
- 编号必须连续、唯一，新增脚本追加到末尾。
- 默认脚本必须可重复执行，优先使用 `IF EXISTS` / `IF NOT EXISTS` / `ON CONFLICT` / `DO $$` 保护。
- `CREATE TABLE`、`CREATE INDEX`、`ALTER TABLE ... ADD COLUMN` 必须使用 `IF NOT EXISTS`，或位于显式 catalog guard 中；未保护结构语句会导致 `make postgres-migration-check` 失败。
- `DROP DATABASE`、`DROP SCHEMA`、`DROP TABLE`、`TRUNCATE` 属于高风险语句，会导致 `make postgres-migration-check` 失败。
- `DROP VIEW`、`DROP FUNCTION`、`DROP TRIGGER`、`DROP OPERATOR`、`DROP AGGREGATE`、`DELETE FROM` 会进入 watch 清单；drop-watch 必须带 `IF EXISTS`，delete-watch 必须带 `WHERE`，说明见 `docs/postgres-migration-watch-rationale.md`。
