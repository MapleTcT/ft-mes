# 当前内容迁移清单

本文件由 `scripts/generate-current-content-inventory.py` 生成，用于说明当前恢复内容已经迁移到可持续开发仓库中的哪些位置。

## 总览

- 后端 sources.jar：`250` 个。
- 后端 Java 源码：`4807` 个文件。
- 后端 XML：`398` 个文件。
- 反编译服务 Java：`259` 个文件。
- 前端 source map：`366` 个。
- 前端恢复源码：`991` 个文件。
- Docker Compose 服务：`53` 个。
- 默认数据库：`postgresql`。
- Oracle 模式：`legacy-template-only`。

## 后端源码分组

下表按 Maven group 汇总恢复出来的 sources.jar。完整机器可读清单见 `metadata/current-content-inventory.json`。

| Group | Modules | Java | XML |
| --- | --- | --- | --- |
| com.supcon.supfusion | 147 | 2275 | 223 |
| com.supcon.supfusion.notification | 42 | 428 | 54 |
| com.supcon.supfusion.framework.boot | 10 | 20 | 10 |
| com.supcon.supfusion.module.registry | 10 | 34 | 10 |
| com.supcon.supfusion.flow | 9 | 256 | 16 |
| com.supcon.supfusion.configuration | 7 | 1353 | 41 |
| com.supcon.supfusion.framework1.boot | 6 | 16 | 6 |
| com.supcon.supfusion.signature | 6 | 146 | 14 |
| com.supcon.supfusion.framework | 5 | 101 | 5 |
| com.supcon.supfusion.custom.propertye | 4 | 133 | 14 |
| com.supcon.supfusion.framework1 | 3 | 36 | 3 |
| com.supcon.msgcenter | 1 | 9 | 2 |

## 平台运行服务

| Service | Spring Boot | Start Class |
| --- | --- | --- |
| auditlog | 2.1.5.RELEASE | com.supcon.supfusion.auditlog.bootstrap.Bootstrap |
| baseService | 2.1.5.RELEASE | com.supcon.greendill.BaseServiceApp |
| basicmanagement | 2.1.5.RELEASE | com.supcon.supfusion.basicmanagement.BasicmanagementApp |
| configuration | 2.1.5.RELEASE | com.supcon.supfusion.configuration.services.Bootstrap |
| customProperty | 2.1.5.RELEASE | com.supcon.supfusion.custon.property.CoustomPropertyBootstrap |
| fileServer | 2.1.5.RELEASE | com.supcon.supfusion.file.server.Bootstrap |
| flow | 2.1.5.RELEASE | com.supcon.supfusion.flow.bootstrap.Bootstrap |
| gateway | 2.1.12.RELEASE | com.supcon.supos.suposgateway.SuposGatewayApplication |
| i18n | 2.1.5.RELEASE | com.supcon.supfusion.i18n.bootstrap.Bootstrap |
| iam | 2.1.5.RELEASE | com.supcon.supfusion.iam.Bootstrap |
| license | 2.1.5.RELEASE | com.supcon.supfusion.license.bootstrap.LicenseApplication |
| notification-admin | 2.1.5.RELEASE | com.supcon.supfusion.notification.admin.bootstrap.Bootstrap |
| notification-apiserver | 2.1.5.RELEASE | com.supcon.supfusion.notification.apiserver.bootstrap.Bootstrap |
| notification-app | 2.1.5.RELEASE | com.supcon.supfusion.notification.app.bootstrap.NoticeAppBootstrap |
| notification-engine | 2.1.5.RELEASE | com.supcon.supfusion.notification.engine.bootstrap.Bootstrap |
| notification-mobile | 2.1.5.RELEASE | com.supcon.supfusion.notification.mobile.MobileBootstrap |
| notification-sms-jincang | 2.1.5.RELEASE | com.supcon.supfusion.notification.sms.SmsBootstrap |
| operatetools | 2.1.5.RELEASE | com.supcon.orchid.entityconf.MicroService |
| orgmanagement | 2.1.5.RELEASE | com.supcon.supfusion.orgmanagement.OrgManagementApp |
| printer | 2.1.5.RELEASE | com.supcon.supfusion.printer.bootstrap.Bootstrap |
| sysmanagement | 2.1.5.RELEASE | com.supcon.supfusion.sysmanagement.SysmanagementApp |
| task-scheduler | 2.1.5.RELEASE | com.supcon.supfusion.scheduler.Bootstrap |
| websocket |  |  |

## 前端应用

| App | Recovered Files | Source Maps |
| --- | --- | --- |
| auth | 57 | 24 |
| bap | 185 | 19 |
| greenDill | 226 | 9 |
| i18n | 16 | 6 |
| menu | 21 | 6 |
| notification | 43 | 15 |
| organization | 47 | 36 |
| print | 78 | 24 |
| signature-static | 19 | 10 |
| supplant | 97 | 108 |
| supplant-test | 87 | 88 |
| systemcode | 26 | 6 |
| systemconfig | 48 | 8 |
| taskscheduler | 22 | 2 |
| theme | 19 | 5 |

## Docker 编排服务

| Service | Kind | Jar Path |
| --- | --- | --- |
| postgres | infrastructure | - |
| redis | infrastructure | - |
| test-license-seed | infrastructure | - |
| zookeeper | infrastructure | - |
| kafka | infrastructure | - |
| mongo | infrastructure | - |
| nacos | infrastructure | - |
| nacos-config | infrastructure | - |
| keycloak | infrastructure | - |
| minio | infrastructure | - |
| websocket | platform-runtime | bap-server/base-Server/websocket/supfusion-websocket.jar |
| i18n | platform-runtime | bap-server/base-Server/i18n/supfusion-i18n.jar |
| sysmanagement | platform-runtime | bap-server/base-Server/sysmanagement/supfusion-sysmanagement.jar |
| basicmanagement | platform-runtime | bap-server/base-Server/basicmanagement/supfusion-basicmanagement.jar |
| orgmanagement | platform-runtime | bap-server/base-Server/orgmanagement/supfusion-orgmanagement.jar |
| iam | platform-runtime | bap-server/base-Server/iam/supfusion-iam.jar |
| gateway | platform-runtime | bap-server/base-Server/gateway/supos-gateway.jar |
| license | platform-runtime | bap-server/base-Server/license/supfusion-license.jar |
| configuration | platform-runtime | bap-server/base-Server/configuration/supfusion-configuration.jar |
| operatetools | platform-runtime | bap-server/base-Server/operatetools/supfusion-operatetools.jar |
| baseService | platform-runtime | bap-server/base-Server/baseService/supfusion-baseservice.jar |
| flow | platform-runtime | bap-server/base-Server/flow/supfusion-flow.jar |
| notification-admin | platform-runtime | bap-server/base-Server/notification-admin/notification-admin.jar |
| notification-apiserver | platform-runtime | bap-server/base-Server/notification-apiserver/notification-apiserver.jar |
| notification-engine | platform-runtime | bap-server/base-Server/notification-engine/notification-engine.jar |
| fileServer | platform-runtime | bap-server/base-Server/fileServer/supfusion-file-server.jar |
| task-scheduler | platform-runtime | bap-server/base-Server/task-scheduler/supfusion-task-scheduler-service.jar |
| customProperty | platform-runtime | bap-server/base-Server/customProperty/custom-property.jar |
| adp-admin | platform-runtime | bap-server/base-Server/adp-admin/adp-admin.jar |
| excel-server | platform-runtime | bap-server/base-Server/excel-server/excel-server.jar |
| msgmanagement | platform-runtime | bap-server/base-Server/msgmanagement/supfusion-msgmanagement.jar |
| notification-app | platform-runtime | bap-server/base-Server/notification-app/notification-app.jar |
| notification-dingtalk | platform-runtime | bap-server/base-Server/notification-dingtalk/notification-dingtalk.jar |
| notification-mobile | platform-runtime | bap-server/base-Server/notification-mobile/notification-mobile.jar |
| notification-sms-jincang | platform-runtime | bap-server/base-Server/notification-sms-jincang/notification-sms-jincang.jar |
| notification-wechat | platform-runtime | bap-server/base-Server/notification-wechat/notification-wechat.jar |
| printer | platform-runtime | bap-server/base-Server/printer/supfusion-printer.jar |
| auditlog | platform-runtime | bap-server/base-Server/auditlog/supfusion-auditlog.jar |
| craftGraphMs | business-runtime | bap-server/module-Server/craftGraphMs/manual/craftGraphMs-1.0.0.jar |
| RMMs | business-runtime | bap-server/module-Server/RMMs/manual/RMMs-1.0.0.jar |
| WOMMs | business-runtime | bap-server/module-Server/WOMMs/manual/WOMMs-1.0.0.jar |
| FoundationMs | business-runtime | bap-server/module-Server/FoundationMs/manual/FoundationMs-1.0.0.jar |
| EamMs | business-runtime | bap-server/module-Server/EamMs/manual/EamMs-1.0.0.jar |
| SpecialMs | business-runtime | bap-server/module-Server/SpecialMs/manual/SpecialMs-1.0.0.jar |
| OEEMs | business-runtime | bap-server/module-Server/OEEMs/manual/OEEMs-1.0.0.jar |
| ToolMs | business-runtime | bap-server/module-Server/ToolMs/manual/ToolMs-1.0.0.jar |
| LIMS | business-runtime | bap-server/module-Server/LIMS/manual/LIMS-1.0.0.jar |
| LIMSDCMan | business-runtime | bap-server/module-Server/LIMSDCMan/manual/LIMSDCMan-1.0.0.jar |
| LIMSINT | business-runtime | bap-server/module-Server/LIMSINT/manual/LIMSINT-1.0.0.jar |
| LIMSMatStds | business-runtime | bap-server/module-Server/LIMSMatStds/manual/LIMSMatStds-1.0.0.jar |
| WTSs | business-runtime | bap-server/module-Server/WTSs/manual/WTSs-1.0.0.jar |
| WAPS | business-runtime | bap-server/module-Server/WAPS/manual/WAPS-1.0.0.jar |
| nginx | infrastructure | - |

## 使用方式

- 新业务包进来后，先更新 runtime/部署编排，再运行 `make inventory` 刷新本清单。
- 如果清单变化涉及后端表结构，继续补 `docs/backend-table-audit/` 下的落表报告。
- 如果清单变化引入 Oracle 配置，必须进入 `oracle-legacy` 路径并补迁移 issue。
