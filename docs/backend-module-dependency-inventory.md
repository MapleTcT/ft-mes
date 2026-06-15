# 后端恢复模块依赖库存

本文件由 `scripts/generate-backend-dependency-inventory.py` 生成，用于把恢复源码中的 Maven POM 变成可审计的模块依赖清单。

## 摘要

- 源目录：`backend/modules`。
- POM 数量：`250`。
- 可解析模块：`250`。
- 解析失败：`0`。
- 依赖边：`1173`。
- 内部依赖边：`329`。
- 外部依赖边：`844`。
- JDBC 依赖：`4`。
- Oracle 依赖：`2`。
- 重复模块坐标：`27`。
- 机器可读清单：`metadata/backend-module-dependency-inventory.json`。

## 模块层级统计

| Layer | Modules |
| --- | --- |
| api | 62 |
| common | 27 |
| dao | 28 |
| manager | 17 |
| other | 51 |
| resources | 17 |
| service | 27 |
| upgrade | 1 |
| webapi | 20 |

## 提升优先域统计

| Bucket | Modules |
| --- | --- |
| other | 96 |
| platform-auth | 42 |
| platform-config | 41 |
| platform-io | 55 |
| workflow | 16 |

## Top 内部依赖

| Name | Count |
| --- | --- |
| com.supcon.supfusion.framework.boot:scaffold-boot-starter-kafka | 17 |
| com.supcon.supfusion:i18n-service-api | 13 |
| com.supcon.supfusion:organization-api | 12 |
| com.supcon.supfusion:systemcode-api | 10 |
| com.supcon.supfusion:rbac-api | 10 |
| com.supcon.supfusion.module.registry:module-registry-api | 9 |
| com.supcon.supfusion:auth-api | 7 |
| com.supcon.supfusion.notification:admin-api | 6 |
| com.supcon.supfusion:system-config-common | 6 |
| com.supcon.supfusion:rbac-common | 5 |
| com.supcon.supfusion:file-server-api | 4 |
| com.supcon.supfusion:websocket-service-api | 4 |
| com.supcon.supfusion.framework:scaffold-auditlog | 4 |
| com.supcon.supfusion.framework.boot:scaffold-boot-auditlog-autoconfigure | 4 |
| com.supcon.supfusion.notification:apiserver-api | 3 |
| com.supcon.supfusion:auth-service | 3 |
| com.supcon.supfusion:printer-service | 3 |
| com.supcon.supfusion:cloud-task-scheduler-server-service | 3 |
| com.supcon.supfusion:counter-api | 3 |
| com.supcon.supfusion:license-api | 3 |

## Top 外部依赖

| Name | Count |
| --- | --- |
| com.supcon.supfusion.framework:cloud-rpc | 61 |
| com.supcon.supfusion.framework:cloud-common | 49 |
| org.springframework.cloud:spring-cloud-openfeign-core | 45 |
| com.supcon.supfusion.framework.boot:cloud-boot-starter | 37 |
| org.springframework.boot:spring-boot-starter-web | 31 |
| com.supcon.supfusion.framework.boot:scaffold-boot-starter-dbp | 30 |
| org.springframework:spring-web | 29 |
| com.supcon.supfusion.framework.boot:scaffold-boot-starter-mybatis | 27 |
| io.springfox:springfox-swagger2 | 24 |
| com.supcon.supfusion:tenant-manager-api | 23 |
| io.springfox:springfox-swagger-ui | 21 |
| com.supcon.supfusion.framework.boot:cloud-boot-security-autoconfigure | 19 |
| io.swagger:swagger-annotations | 19 |
| com.alibaba:fastjson | 17 |
| org.powermock:powermock-api-mockito2 | 16 |
| org.powermock:powermock-module-junit4 | 16 |
| org.powermock:powermock-module-junit4-rule-agent | 16 |
| org.powermock:powermock-module-javaagent | 16 |
| org.springframework.boot:spring-boot-autoconfigure | 15 |
| org.apache.commons:commons-lang3 | 13 |
| org.springframework.boot:spring-boot-starter-test | 12 |
| org.mockito:mockito-core | 12 |
| com.supcon.supfusion.notification:notification-protocol | 12 |
| org.projectlombok:lombok | 10 |
| org.apache.poi:poi-ooxml | 8 |
| junit:junit | 8 |
| com.supcon.supfusion.framework:cloud-i18n | 8 |
| org.springframework.boot:spring-boot-starter-freemarker | 8 |
| com.supcon.supfusion.notification:notification-common | 7 |
| net.sf.flexjson:flexjson | 6 |

## Oracle/JDBC 风险

| Module | Oracle deps | Path |
| --- | --- | --- |
| com.supcon.supfusion.flow:flow-common | 1 | backend/modules/com/supcon/supfusion/flow/flow-common/1.0.0-RELEASE/META-INF/maven/com.supcon.supfusion.flow/flow-common/pom.xml |
| com.supcon.supfusion:auth-upgrade | 1 | backend/modules/com/supcon/supfusion/auth-upgrade/1.0.1.RELEASE/META-INF/maven/com.supcon.supfusion/auth-upgrade/pom.xml |

## 重复模块坐标

| Module | Count | Versions |
| --- | --- | --- |
| com.supcon.supfusion.flow:flow-api | 2 | 1.0.0-RELEASE, 1.0.0.RELEASE |
| com.supcon.supfusion.framework.boot:scaffold-boot-auditlog-autoconfigure | 4 | 1.0.4.RELEASE, 1.0.5.RELEASE, 1.0.6.RELEASE |
| com.supcon.supfusion.framework.boot:scaffold-boot-kafka-autoconfigure | 2 | 1.0.3.RELEASE |
| com.supcon.supfusion.framework.boot:scaffold-boot-redis-autoconfigure | 2 | 1.0.3.RELEASE |
| com.supcon.supfusion.framework.boot:scaffold-boot-starter-auditlog | 4 | 1.0.4.RELEASE, 1.0.5.RELEASE, 1.0.6.RELEASE |
| com.supcon.supfusion.framework.boot:scaffold-boot-starter-kafka | 2 | 1.0.3.RELEASE |
| com.supcon.supfusion.framework.boot:scaffold-boot-starter-redis | 2 | 1.0.3.RELEASE |
| com.supcon.supfusion.framework:scaffold-auditlog | 4 | 1.0.4.RELEASE, 1.0.5.RELEASE, 1.0.6.RELEASE |
| com.supcon.supfusion.framework:scaffold-kafka | 2 | 1.0.3.RELEASE |
| com.supcon.supfusion.framework:scaffold-redis | 2 | 1.0.3.RELEASE |
| com.supcon.supfusion.module.registry:module-registry-api | 2 | ${module-registry-api.version} |
| com.supcon.supfusion.module.registry:module-registry-dao | 2 | 1.0.0-RELEASE, 1.0.1.RELEASE |
| com.supcon.supfusion.module.registry:module-registry-resources | 2 | 1.0.0-RELEASE, 1.0.1.RELEASE |
| com.supcon.supfusion.module.registry:module-registry-server | 2 | 1.0.0-RELEASE, 1.0.1.RELEASE |
| com.supcon.supfusion.module.registry:module-registry-webapi | 2 | 1.0.0-RELEASE, 1.0.1.RELEASE |
| com.supcon.supfusion.notification:admin-api | 2 | 1.0.0-SNAPSHOT, 1.0.3-RELEASE |
| com.supcon.supfusion.notification:apiserver-api | 3 | ${apiserver-api.version}, 1.0.0-SNAPSHOT |
| com.supcon.supfusion:auth-api | 2 | 1.0.0.RELEASE, 1.0.1.RELEASE |
| com.supcon.supfusion:auth-keycloak-client-api | 2 | 1.0.0.RELEASE, 1.0.1.RELEASE |
| com.supcon.supfusion:auth-resources | 2 | 1.0.0-SNAPSHOT, 1.0.0.RELEASE |
| com.supcon.supfusion:cloud-task-scheduler-server-api | 2 | 1.0.0-SNAPSHOT, 1.0.0.RELEASE |
| com.supcon.supfusion:counter-common | 2 | ${counter-common.version}, 1.0.0-SNAPSHOT |
| com.supcon.supfusion:organization-api | 3 | ${organization-api.version}, 1.2.0-SNAPSHOT |
| com.supcon.supfusion:rbac-api | 3 | 1.0.0.RELEASE, 1.0.2.RELEASE, 1.0.3.RELEASE |
| com.supcon.supfusion:system-config-api | 3 | ${systemconfig-api.version}, 1.0.0-SNAPSHOT, 1.0.0.RELEASE |
| com.supcon.supfusion:system-config-common | 2 | ${systemconfig-common.version}, 1.0.0-SNAPSHOT |
| com.supcon.supfusion:systemcode-api | 5 | ${syscode-api.version}, 1.0.0-SNAPSHOT |

## 模块清单

| Module | Family | Layer | Deps | Internal | External | Oracle | Path |
| --- | --- | --- | --- | --- | --- | --- | --- |
| com.supcon.supfusion.notification:admin-api | admin | api | 8 | 0 | 8 | 0 | backend/modules/com/supcon/supfusion/notification/admin-api/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion.notification/admin-api/pom.xml |
| com.supcon.supfusion.notification:admin-api | admin | api | 8 | 0 | 8 | 0 | backend/modules/com/supcon/supfusion/notification/admin-api/1.0.3-RELEASE/META-INF/maven/com.supcon.supfusion.notification/admin-api/pom.xml |
| com.supcon.supfusion.notification:admin-openapi | admin | api | 5 | 3 | 2 | 0 | backend/modules/com/supcon/supfusion/notification/admin-openapi/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion.notification/admin-openapi/pom.xml |
| com.supcon.supfusion.notification:admin-common | admin | common | 4 | 0 | 4 | 0 | backend/modules/com/supcon/supfusion/notification/admin-common/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion.notification/admin-common/pom.xml |
| com.supcon.supfusion.notification:admin-dao | admin | dao | 6 | 0 | 6 | 0 | backend/modules/com/supcon/supfusion/notification/admin-dao/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion.notification/admin-dao/pom.xml |
| com.supcon.supfusion.notification:admin-manager | admin | manager | 5 | 3 | 2 | 0 | backend/modules/com/supcon/supfusion/notification/admin-manager/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion.notification/admin-manager/pom.xml |
| com.supcon.supfusion.notification:admin-resources | admin | resources | 0 | 0 | 0 | 0 | backend/modules/com/supcon/supfusion/notification/admin-resources/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion.notification/admin-resources/pom.xml |
| com.supcon.supfusion.notification:admin-service | admin | service | 9 | 4 | 5 | 0 | backend/modules/com/supcon/supfusion/notification/admin-service/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion.notification/admin-service/pom.xml |
| com.supcon.supfusion.notification:admin-webapi | admin | webapi | 5 | 2 | 3 | 0 | backend/modules/com/supcon/supfusion/notification/admin-webapi/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion.notification/admin-webapi/pom.xml |
| com.supcon.supfusion.notification:apiserver-api | apiserver | api | 16 | 0 | 16 | 0 | backend/modules/com/supcon/supfusion/notification/apiserver-api/1.0.0-RELEASE/META-INF/maven/com.supcon.supfusion.notification/apiserver-api/pom.xml |
| com.supcon.supfusion.notification:apiserver-api | apiserver | api | 16 | 0 | 16 | 0 | backend/modules/com/supcon/supfusion/notification/apiserver-api/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion.notification/apiserver-api/pom.xml |
| com.supcon.supfusion.notification:apiserver-api | apiserver | api | 16 | 0 | 16 | 0 | backend/modules/com/supcon/supfusion/notification/apiserver-api/1.0.0.RELEASE/META-INF/maven/com.supcon.supfusion.notification/apiserver-api/pom.xml |
| com.supcon.supfusion.notification:apiserver-openapi | apiserver | api | 8 | 2 | 6 | 0 | backend/modules/com/supcon/supfusion/notification/apiserver-openapi/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion.notification/apiserver-openapi/pom.xml |
| com.supcon.supfusion.notification:apiserver-common | apiserver | common | 4 | 0 | 4 | 0 | backend/modules/com/supcon/supfusion/notification/apiserver-common/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion.notification/apiserver-common/pom.xml |
| com.supcon.supfusion.notification:apiserver-dao | apiserver | dao | 5 | 0 | 5 | 0 | backend/modules/com/supcon/supfusion/notification/apiserver-dao/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion.notification/apiserver-dao/pom.xml |
| com.supcon.supfusion.notification:apiserver-manager | apiserver | manager | 13 | 3 | 10 | 0 | backend/modules/com/supcon/supfusion/notification/apiserver-manager/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion.notification/apiserver-manager/pom.xml |
| com.supcon.supfusion.notification:apiserver-resources | apiserver | resources | 0 | 0 | 0 | 0 | backend/modules/com/supcon/supfusion/notification/apiserver-resources/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion.notification/apiserver-resources/pom.xml |
| com.supcon.supfusion.notification:apiserver-service | apiserver | service | 12 | 5 | 7 | 0 | backend/modules/com/supcon/supfusion/notification/apiserver-service/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion.notification/apiserver-service/pom.xml |
| com.supcon.supfusion.notification:apiserver-webapi | apiserver | webapi | 0 | 0 | 0 | 0 | backend/modules/com/supcon/supfusion/notification/apiserver-webapi/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion.notification/apiserver-webapi/pom.xml |
| com.supcon.supfusion.notification:app-openapi | app | api | 1 | 1 | 0 | 0 | backend/modules/com/supcon/supfusion/notification/app-openapi/1.0.0-RELEASE/META-INF/maven/com.supcon.supfusion.notification/app-openapi/pom.xml |
| com.supcon.supfusion.notification:app-common | app | common | 0 | 0 | 0 | 0 | backend/modules/com/supcon/supfusion/notification/app-common/1.0.0-RELEASE/META-INF/maven/com.supcon.supfusion.notification/app-common/pom.xml |
| com.supcon.supfusion.notification:app-dao | app | dao | 2 | 0 | 2 | 0 | backend/modules/com/supcon/supfusion/notification/app-dao/1.0.0-RELEASE/META-INF/maven/com.supcon.supfusion.notification/app-dao/pom.xml |
| com.supcon.supfusion.notification:app-manager | app | manager | 6 | 1 | 5 | 0 | backend/modules/com/supcon/supfusion/notification/app-manager/1.0.0-RELEASE/META-INF/maven/com.supcon.supfusion.notification/app-manager/pom.xml |
| com.supcon.supfusion.notification:app-resources | app | resources | 0 | 0 | 0 | 0 | backend/modules/com/supcon/supfusion/notification/app-resources/1.0.0-RELEASE/META-INF/maven/com.supcon.supfusion.notification/app-resources/pom.xml |
| com.supcon.supfusion.notification:app-service | app | service | 3 | 2 | 1 | 0 | backend/modules/com/supcon/supfusion/notification/app-service/1.0.0-RELEASE/META-INF/maven/com.supcon.supfusion.notification/app-service/pom.xml |
| com.supcon.supfusion.notification:app-config | app-config | other | 4 | 2 | 2 | 0 | backend/modules/com/supcon/supfusion/notification/app-config/1.0.0-RELEASE/META-INF/maven/com.supcon.supfusion.notification/app-config/pom.xml |
| com.supcon.supfusion:auditlog-api | auditlog | api | 3 | 0 | 3 | 0 | backend/modules/com/supcon/supfusion/auditlog-api/1.0.0.RELEASE/META-INF/maven/com.supcon.supfusion/auditlog-api/pom.xml |
| com.supcon.supfusion:auditlog-openapi | auditlog | api | 1 | 1 | 0 | 0 | backend/modules/com/supcon/supfusion/auditlog-openapi/1.0.0.RELEASE/META-INF/maven/com.supcon.supfusion/auditlog-openapi/pom.xml |
| com.supcon.supfusion:auditlog-common | auditlog | common | 5 | 0 | 5 | 0 | backend/modules/com/supcon/supfusion/auditlog-common/1.0.0.RELEASE/META-INF/maven/com.supcon.supfusion/auditlog-common/pom.xml |
| com.supcon.supfusion:auditlog-dao | auditlog | dao | 2 | 0 | 2 | 0 | backend/modules/com/supcon/supfusion/auditlog-dao/1.0.0.RELEASE/META-INF/maven/com.supcon.supfusion/auditlog-dao/pom.xml |
| com.supcon.supfusion:auditlog-manager | auditlog | manager | 3 | 3 | 0 | 0 | backend/modules/com/supcon/supfusion/auditlog-manager/1.0.0.RELEASE/META-INF/maven/com.supcon.supfusion/auditlog-manager/pom.xml |
| com.supcon.supfusion:auditlog-service | auditlog | service | 6 | 4 | 2 | 0 | backend/modules/com/supcon/supfusion/auditlog-service/1.0.0.RELEASE/META-INF/maven/com.supcon.supfusion/auditlog-service/pom.xml |
| com.supcon.supfusion:auditlog-webapi | auditlog | webapi | 2 | 1 | 1 | 0 | backend/modules/com/supcon/supfusion/auditlog-webapi/1.0.0.RELEASE/META-INF/maven/com.supcon.supfusion/auditlog-webapi/pom.xml |
| com.supcon.supfusion:auth-api | auth | api | 6 | 0 | 6 | 0 | backend/modules/com/supcon/supfusion/auth-api/1.0.0.RELEASE/META-INF/maven/com.supcon.supfusion/auth-api/pom.xml |
| com.supcon.supfusion:auth-api | auth | api | 6 | 0 | 6 | 0 | backend/modules/com/supcon/supfusion/auth-api/1.0.1.RELEASE/META-INF/maven/com.supcon.supfusion/auth-api/pom.xml |
| com.supcon.supfusion:auth-openapi | auth | api | 7 | 1 | 6 | 0 | backend/modules/com/supcon/supfusion/auth-openapi/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/auth-openapi/pom.xml |
| com.supcon.supfusion:auth-common | auth | common | 13 | 0 | 13 | 0 | backend/modules/com/supcon/supfusion/auth-common/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/auth-common/pom.xml |
| com.supcon.supfusion:auth-dao | auth | dao | 3 | 1 | 2 | 0 | backend/modules/com/supcon/supfusion/auth-dao/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/auth-dao/pom.xml |
| com.supcon.supfusion:auth-manager | auth | manager | 13 | 10 | 3 | 0 | backend/modules/com/supcon/supfusion/auth-manager/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/auth-manager/pom.xml |
| com.supcon.supfusion:auth-resources | auth | resources | 0 | 0 | 0 | 0 | backend/modules/com/supcon/supfusion/auth-resources/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/auth-resources/pom.xml |
| com.supcon.supfusion:auth-resources | auth | resources | 0 | 0 | 0 | 0 | backend/modules/com/supcon/supfusion/auth-resources/1.0.0.RELEASE/META-INF/maven/com.supcon.supfusion/auth-resources/pom.xml |
| com.supcon.supfusion:auth-service | auth | service | 19 | 5 | 14 | 0 | backend/modules/com/supcon/supfusion/auth-service/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/auth-service/pom.xml |
| com.supcon.supfusion:auth-upgrade | auth | upgrade | 9 | 0 | 9 | 1 | backend/modules/com/supcon/supfusion/auth-upgrade/1.0.1.RELEASE/META-INF/maven/com.supcon.supfusion/auth-upgrade/pom.xml |
| com.supcon.supfusion:auth-webapi | auth | webapi | 7 | 1 | 6 | 0 | backend/modules/com/supcon/supfusion/auth-webapi/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/auth-webapi/pom.xml |
| com.supcon.supfusion:auth-keycloak-client-api | auth-keycloak | api | 6 | 0 | 6 | 0 | backend/modules/com/supcon/supfusion/auth-keycloak-client-api/1.0.0.RELEASE/META-INF/maven/com.supcon.supfusion/auth-keycloak-client-api/pom.xml |
| com.supcon.supfusion:auth-keycloak-client-api | auth-keycloak | api | 6 | 0 | 6 | 0 | backend/modules/com/supcon/supfusion/auth-keycloak-client-api/1.0.1.RELEASE/META-INF/maven/com.supcon.supfusion/auth-keycloak-client-api/pom.xml |
| com.supcon.supfusion:auth-keycloak | auth-keycloak | other | 9 | 0 | 9 | 0 | backend/modules/com/supcon/supfusion/auth-keycloak/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/auth-keycloak/pom.xml |
| com.supcon.supfusion:basicmanagement-auditlog | basicmanagement-auditlog | other | 0 | 0 | 0 | 0 | backend/modules/com/supcon/supfusion/basicmanagement-auditlog/1.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/basicmanagement-auditlog/pom.xml |
| com.supcon.supfusion:basicmanagement-portal | basicmanagement-portal | other | 7 | 4 | 3 | 0 | backend/modules/com/supcon/supfusion/basicmanagement-portal/1.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/basicmanagement-portal/pom.xml |
| com.supcon.supfusion:basicmanagement-printer | basicmanagement-printer | other | 10 | 6 | 4 | 0 | backend/modules/com/supcon/supfusion/basicmanagement-printer/1.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/basicmanagement-printer/pom.xml |
| com.supcon.supfusion:basicmanagement-signature | basicmanagement-signature | other | 5 | 3 | 2 | 0 | backend/modules/com/supcon/supfusion/basicmanagement-signature/1.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/basicmanagement-signature/pom.xml |
| com.supcon.supfusion:basicmanagement-theme | basicmanagement-theme | other | 8 | 3 | 5 | 0 | backend/modules/com/supcon/supfusion/basicmanagement-theme/1.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/basicmanagement-theme/pom.xml |
| com.supcon.supfusion:cloud-task-scheduler-server-api | cloud-task-scheduler-server | api | 5 | 1 | 4 | 0 | backend/modules/com/supcon/supfusion/cloud-task-scheduler-server-api/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/cloud-task-scheduler-server-api/pom.xml |
| com.supcon.supfusion:cloud-task-scheduler-server-api | cloud-task-scheduler-server | api | 5 | 1 | 4 | 0 | backend/modules/com/supcon/supfusion/cloud-task-scheduler-server-api/1.0.0.RELEASE/META-INF/maven/com.supcon.supfusion/cloud-task-scheduler-server-api/pom.xml |
| com.supcon.supfusion:cloud-task-scheduler-server-openapi | cloud-task-scheduler-server | api | 1 | 1 | 0 | 0 | backend/modules/com/supcon/supfusion/cloud-task-scheduler-server-openapi/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/cloud-task-scheduler-server-openapi/pom.xml |
| com.supcon.supfusion:cloud-task-scheduler-server-common | cloud-task-scheduler-server | common | 2 | 0 | 2 | 0 | backend/modules/com/supcon/supfusion/cloud-task-scheduler-server-common/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/cloud-task-scheduler-server-common/pom.xml |
| com.supcon.supfusion:cloud-task-scheduler-server-dao | cloud-task-scheduler-server | dao | 3 | 1 | 2 | 0 | backend/modules/com/supcon/supfusion/cloud-task-scheduler-server-dao/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/cloud-task-scheduler-server-dao/pom.xml |
| com.supcon.supfusion:cloud-task-scheduler-server-manager | cloud-task-scheduler-server | manager | 6 | 3 | 3 | 0 | backend/modules/com/supcon/supfusion/cloud-task-scheduler-server-manager/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/cloud-task-scheduler-server-manager/pom.xml |
| com.supcon.supfusion:cloud-task-scheduler-server-service | cloud-task-scheduler-server | service | 8 | 3 | 5 | 0 | backend/modules/com/supcon/supfusion/cloud-task-scheduler-server-service/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/cloud-task-scheduler-server-service/pom.xml |
| com.supcon.supfusion:cloud-task-scheduler-server-webapi | cloud-task-scheduler-server | webapi | 3 | 1 | 2 | 0 | backend/modules/com/supcon/supfusion/cloud-task-scheduler-server-webapi/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/cloud-task-scheduler-server-webapi/pom.xml |
| com.supcon.supfusion.configuration:configuration-services-api | configuration-services | api | 2 | 0 | 2 | 0 | backend/modules/com/supcon/supfusion/configuration/configuration-services-api/1.0.0.RELEASE/META-INF/maven/com.supcon.supfusion.configuration/configuration-services-api/pom.xml |
| com.supcon.supfusion.configuration:configuration-services-open-api | configuration-services | api | 4 | 2 | 2 | 0 | backend/modules/com/supcon/supfusion/configuration/configuration-services-open-api/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion.configuration/configuration-services-open-api/pom.xml |
| com.supcon.supfusion.configuration:configuration-services-project-api | configuration-services | api | 6 | 3 | 3 | 0 | backend/modules/com/supcon/supfusion/configuration/configuration-services-project-api/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion.configuration/configuration-services-project-api/pom.xml |
| com.supcon.supfusion.configuration:configuration-services-base | configuration-services | common | 12 | 8 | 4 | 0 | backend/modules/com/supcon/supfusion/configuration/configuration-services-base/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion.configuration/configuration-services-base/pom.xml |
| com.supcon.supfusion.configuration:configuration-services-dao | configuration-services | dao | 5 | 1 | 4 | 0 | backend/modules/com/supcon/supfusion/configuration/configuration-services-dao/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion.configuration/configuration-services-dao/pom.xml |
| com.supcon.supfusion.configuration:configuration-services-service | configuration-services | service | 7 | 3 | 4 | 0 | backend/modules/com/supcon/supfusion/configuration/configuration-services-service/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion.configuration/configuration-services-service/pom.xml |
| com.supcon.supfusion.configuration:configuration-workflow | configuration-workflow | other | 10 | 1 | 9 | 0 | backend/modules/com/supcon/supfusion/configuration/configuration-workflow/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion.configuration/configuration-workflow/pom.xml |
| com.supcon.supfusion:counter-api | counter | api | 3 | 1 | 2 | 0 | backend/modules/com/supcon/supfusion/counter-api/1.0.0.RELEASE/META-INF/maven/com.supcon.supfusion/counter-api/pom.xml |
| com.supcon.supfusion:counter-common | counter | common | 8 | 0 | 8 | 0 | backend/modules/com/supcon/supfusion/counter-common/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/counter-common/pom.xml |
| com.supcon.supfusion:counter-common | counter | common | 8 | 0 | 8 | 0 | backend/modules/com/supcon/supfusion/counter-common/1.0.0.RELEASE/META-INF/maven/com.supcon.supfusion/counter-common/pom.xml |
| com.supcon.supfusion:counter-dao | counter | dao | 4 | 1 | 3 | 0 | backend/modules/com/supcon/supfusion/counter-dao/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/counter-dao/pom.xml |
| com.supcon.supfusion:counter-service | counter | service | 4 | 2 | 2 | 0 | backend/modules/com/supcon/supfusion/counter-service/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/counter-service/pom.xml |
| com.supcon.supfusion:counter-webapi | counter | webapi | 3 | 1 | 2 | 0 | backend/modules/com/supcon/supfusion/counter-webapi/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/counter-webapi/pom.xml |
| com.supcon.supfusion:counter-sdk | counter-sdk | other | 2 | 1 | 1 | 0 | backend/modules/com/supcon/supfusion/counter-sdk/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/counter-sdk/pom.xml |
| com.supcon.supfusion:custom-property-api | custom-property | api | 0 | 0 | 0 | 0 | backend/modules/com/supcon/supfusion/custom-property-api/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/custom-property-api/pom.xml |
| com.supcon.supfusion.custom.propertye:custom-property-common | custom-property | common | 6 | 2 | 4 | 0 | backend/modules/com/supcon/supfusion/custom/propertye/custom-property-common/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion.custom.propertye/custom-property-common/pom.xml |
| com.supcon.supfusion:custom-property-common | custom-property | common | 10 | 3 | 7 | 0 | backend/modules/com/supcon/supfusion/custom-property-common/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/custom-property-common/pom.xml |
| com.supcon.supfusion.custom.propertye:custom-property-dao | custom-property | dao | 7 | 1 | 6 | 0 | backend/modules/com/supcon/supfusion/custom/propertye/custom-property-dao/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion.custom.propertye/custom-property-dao/pom.xml |
| com.supcon.supfusion:custom-property-dao | custom-property | dao | 7 | 1 | 6 | 0 | backend/modules/com/supcon/supfusion/custom-property-dao/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/custom-property-dao/pom.xml |
| com.supcon.supfusion.custom.propertye:custom-property-service | custom-property | service | 2 | 1 | 1 | 0 | backend/modules/com/supcon/supfusion/custom/propertye/custom-property-service/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion.custom.propertye/custom-property-service/pom.xml |
| com.supcon.supfusion:custom-property-service | custom-property | service | 2 | 1 | 1 | 0 | backend/modules/com/supcon/supfusion/custom-property-service/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/custom-property-service/pom.xml |
| com.supcon.supfusion.custom.propertye:custom-property-webapi | custom-property | webapi | 3 | 1 | 2 | 0 | backend/modules/com/supcon/supfusion/custom/propertye/custom-property-webapi/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion.custom.propertye/custom-property-webapi/pom.xml |
| com.supcon.supfusion:custom-property-webapi | custom-property | webapi | 5 | 1 | 4 | 0 | backend/modules/com/supcon/supfusion/custom-property-webapi/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/custom-property-webapi/pom.xml |
| com.supcon.supfusion.notification:engine-common | engine | common | 0 | 0 | 0 | 0 | backend/modules/com/supcon/supfusion/notification/engine-common/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion.notification/engine-common/pom.xml |
| com.supcon.supfusion.notification:engine-dao | engine | dao | 4 | 0 | 4 | 0 | backend/modules/com/supcon/supfusion/notification/engine-dao/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion.notification/engine-dao/pom.xml |
| com.supcon.supfusion.notification:engine-dispatcher | engine-dispatcher | other | 13 | 2 | 11 | 0 | backend/modules/com/supcon/supfusion/notification/engine-dispatcher/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion.notification/engine-dispatcher/pom.xml |
| com.supcon.supfusion:file-server-api | file-server | api | 5 | 0 | 5 | 0 | backend/modules/com/supcon/supfusion/file-server-api/1.0.0.RELEASE/META-INF/maven/com.supcon.supfusion/file-server-api/pom.xml |
| com.supcon.supfusion:file-server-openapi | file-server | api | 1 | 1 | 0 | 0 | backend/modules/com/supcon/supfusion/file-server-openapi/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/file-server-openapi/pom.xml |
| com.supcon.supfusion:file-server-common | file-server | common | 11 | 0 | 11 | 0 | backend/modules/com/supcon/supfusion/file-server-common/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/file-server-common/pom.xml |
| com.supcon.supfusion:file-server-dao | file-server | dao | 3 | 1 | 2 | 0 | backend/modules/com/supcon/supfusion/file-server-dao/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/file-server-dao/pom.xml |
| com.supcon.supfusion:file-server-manager | file-server | manager | 5 | 2 | 3 | 0 | backend/modules/com/supcon/supfusion/file-server-manager/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/file-server-manager/pom.xml |
| com.supcon.supfusion:file-server-service | file-server | service | 10 | 5 | 5 | 0 | backend/modules/com/supcon/supfusion/file-server-service/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/file-server-service/pom.xml |
| com.supcon.supfusion:file-server-webapi | file-server | webapi | 5 | 2 | 3 | 0 | backend/modules/com/supcon/supfusion/file-server-webapi/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/file-server-webapi/pom.xml |
| com.supcon.supfusion.flow:flow-api | flow | api | 6 | 0 | 6 | 0 | backend/modules/com/supcon/supfusion/flow/flow-api/1.0.0-RELEASE/META-INF/maven/com.supcon.supfusion.flow/flow-api/pom.xml |
| com.supcon.supfusion.flow:flow-api | flow | api | 6 | 0 | 6 | 0 | backend/modules/com/supcon/supfusion/flow/flow-api/1.0.0.RELEASE/META-INF/maven/com.supcon.supfusion.flow/flow-api/pom.xml |
| com.supcon.supfusion.flow:flow-openapi | flow | api | 2 | 1 | 1 | 0 | backend/modules/com/supcon/supfusion/flow/flow-openapi/1.0.0-RELEASE/META-INF/maven/com.supcon.supfusion.flow/flow-openapi/pom.xml |
| com.supcon.supfusion.flow:flow-common | flow | common | 11 | 1 | 10 | 1 | backend/modules/com/supcon/supfusion/flow/flow-common/1.0.0-RELEASE/META-INF/maven/com.supcon.supfusion.flow/flow-common/pom.xml |
| com.supcon.supfusion.flow:flow-dao | flow | dao | 1 | 1 | 0 | 0 | backend/modules/com/supcon/supfusion/flow/flow-dao/1.0.0-RELEASE/META-INF/maven/com.supcon.supfusion.flow/flow-dao/pom.xml |
| com.supcon.supfusion.flow:flow-resources | flow | resources | 0 | 0 | 0 | 0 | backend/modules/com/supcon/supfusion/flow/flow-resources/1.0.0-RELEASE/META-INF/maven/com.supcon.supfusion.flow/flow-resources/pom.xml |
| com.supcon.supfusion.flow:flow-webapi | flow | webapi | 2 | 1 | 1 | 0 | backend/modules/com/supcon/supfusion/flow/flow-webapi/1.0.0-RELEASE/META-INF/maven/com.supcon.supfusion.flow/flow-webapi/pom.xml |
| com.supcon.supfusion.flow:flow-engine | flow-engine | other | 8 | 1 | 7 | 0 | backend/modules/com/supcon/supfusion/flow/flow-engine/1.0.0-RELEASE/META-INF/maven/com.supcon.supfusion.flow/flow-engine/pom.xml |
| com.supcon.supfusion:i18n-open-api | i18n | api | 2 | 2 | 0 | 0 | backend/modules/com/supcon/supfusion/i18n-open-api/1.0.1-SNAPSHOT/META-INF/maven/com.supcon.supfusion/i18n-open-api/pom.xml |
| com.supcon.supfusion:i18n-common | i18n | common | 17 | 0 | 17 | 0 | backend/modules/com/supcon/supfusion/i18n-common/1.0.1-SNAPSHOT/META-INF/maven/com.supcon.supfusion/i18n-common/pom.xml |
| com.supcon.supfusion:i18n-dao | i18n | dao | 3 | 1 | 2 | 0 | backend/modules/com/supcon/supfusion/i18n-dao/1.0.1-SNAPSHOT/META-INF/maven/com.supcon.supfusion/i18n-dao/pom.xml |
| com.supcon.supfusion:i18n-manager | i18n | manager | 5 | 2 | 3 | 0 | backend/modules/com/supcon/supfusion/i18n-manager/1.0.1-SNAPSHOT/META-INF/maven/com.supcon.supfusion/i18n-manager/pom.xml |
| com.supcon.supfusion:i18n-resources | i18n | resources | 0 | 0 | 0 | 0 | backend/modules/com/supcon/supfusion/i18n-resources/1.0.1-SNAPSHOT/META-INF/maven/com.supcon.supfusion/i18n-resources/pom.xml |
| com.supcon.supfusion:i18n-service | i18n | service | 10 | 4 | 6 | 0 | backend/modules/com/supcon/supfusion/i18n-service/1.0.1-SNAPSHOT/META-INF/maven/com.supcon.supfusion/i18n-service/pom.xml |
| com.supcon.supfusion:i18n-inter-api | i18n-inter | api | 11 | 2 | 9 | 0 | backend/modules/com/supcon/supfusion/i18n-inter-api/1.0.1-SNAPSHOT/META-INF/maven/com.supcon.supfusion/i18n-inter-api/pom.xml |
| com.supcon.supfusion:i18n-service-api | i18n-service | api | 6 | 0 | 6 | 0 | backend/modules/com/supcon/supfusion/i18n-service-api/1.0.0.RELEASE/META-INF/maven/com.supcon.supfusion/i18n-service-api/pom.xml |
| com.supcon.supfusion:i18n-service-sdk | i18n-service-sdk | other | 0 | 0 | 0 | 0 | backend/modules/com/supcon/supfusion/i18n-service-sdk/1.0.1-SNAPSHOT/META-INF/maven/com.supcon.supfusion/i18n-service-sdk/pom.xml |
| com.supcon.supfusion:iam-api | iam | api | 3 | 0 | 3 | 0 | backend/modules/com/supcon/supfusion/iam-api/1.0.2.RELEASE/META-INF/maven/com.supcon.supfusion/iam-api/pom.xml |
| com.supcon.supfusion:iam-openapi | iam | api | 2 | 1 | 1 | 0 | backend/modules/com/supcon/supfusion/iam-openapi/1.0.6-SNAPSHOT/META-INF/maven/com.supcon.supfusion/iam-openapi/pom.xml |
| com.supcon.supfusion:iam-common | iam | common | 3 | 0 | 3 | 0 | backend/modules/com/supcon/supfusion/iam-common/1.0.6-SNAPSHOT/META-INF/maven/com.supcon.supfusion/iam-common/pom.xml |
| com.supcon.supfusion:iam-dao | iam | dao | 4 | 1 | 3 | 0 | backend/modules/com/supcon/supfusion/iam-dao/1.0.6-SNAPSHOT/META-INF/maven/com.supcon.supfusion/iam-dao/pom.xml |
| com.supcon.supfusion:iam-manager | iam | manager | 3 | 2 | 1 | 0 | backend/modules/com/supcon/supfusion/iam-manager/1.0.6-SNAPSHOT/META-INF/maven/com.supcon.supfusion/iam-manager/pom.xml |
| com.supcon.supfusion:iam-service | iam | service | 6 | 5 | 1 | 0 | backend/modules/com/supcon/supfusion/iam-service/1.0.6-SNAPSHOT/META-INF/maven/com.supcon.supfusion/iam-service/pom.xml |
| com.supcon.supfusion:iam-webapi | iam | webapi | 5 | 3 | 2 | 0 | backend/modules/com/supcon/supfusion/iam-webapi/1.0.6-SNAPSHOT/META-INF/maven/com.supcon.supfusion/iam-webapi/pom.xml |
| com.supcon.supfusion:license-api | license | api | 3 | 0 | 3 | 0 | backend/modules/com/supcon/supfusion/license-api/1.0.0.RELEASE/META-INF/maven/com.supcon.supfusion/license-api/pom.xml |
| com.supcon.supfusion:license-openapi | license | api | 0 | 0 | 0 | 0 | backend/modules/com/supcon/supfusion/license-openapi/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/license-openapi/pom.xml |
| com.supcon.supfusion:license-common | license | common | 3 | 0 | 3 | 0 | backend/modules/com/supcon/supfusion/license-common/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/license-common/pom.xml |
| com.supcon.supfusion:license-dao | license | dao | 3 | 1 | 2 | 0 | backend/modules/com/supcon/supfusion/license-dao/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/license-dao/pom.xml |
| com.supcon.supfusion:license-manager | license | manager | 1 | 1 | 0 | 0 | backend/modules/com/supcon/supfusion/license-manager/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/license-manager/pom.xml |
| com.supcon.supfusion:license-service | license | service | 13 | 3 | 10 | 0 | backend/modules/com/supcon/supfusion/license-service/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/license-service/pom.xml |
| com.supcon.supfusion:license-webapi | license | webapi | 8 | 2 | 6 | 0 | backend/modules/com/supcon/supfusion/license-webapi/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/license-webapi/pom.xml |
| com.supcon.supfusion.notification:mobile-openapi | mobile | api | 1 | 1 | 0 | 0 | backend/modules/com/supcon/supfusion/notification/mobile-openapi/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion.notification/mobile-openapi/pom.xml |
| com.supcon.supfusion.notification:mobile-dao | mobile | dao | 2 | 0 | 2 | 0 | backend/modules/com/supcon/supfusion/notification/mobile-dao/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion.notification/mobile-dao/pom.xml |
| com.supcon.supfusion.notification:mobile-manager | mobile | manager | 4 | 1 | 3 | 0 | backend/modules/com/supcon/supfusion/notification/mobile-manager/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion.notification/mobile-manager/pom.xml |
| com.supcon.supfusion.notification:mobile-resources | mobile | resources | 0 | 0 | 0 | 0 | backend/modules/com/supcon/supfusion/notification/mobile-resources/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion.notification/mobile-resources/pom.xml |
| com.supcon.supfusion.notification:mobile-service | mobile | service | 2 | 1 | 1 | 0 | backend/modules/com/supcon/supfusion/notification/mobile-service/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion.notification/mobile-service/pom.xml |
| com.supcon.supfusion.notification:mobile-config | mobile-config | other | 4 | 1 | 3 | 0 | backend/modules/com/supcon/supfusion/notification/mobile-config/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion.notification/mobile-config/pom.xml |
| com.supcon.supfusion.module.registry:module-registry-api | module-registry | api | 6 | 0 | 6 | 0 | backend/modules/com/supcon/supfusion/module/registry/module-registry-api/1.0.0.RELEASE/META-INF/maven/com.supcon.supfusion.module.registry/module-registry-api/pom.xml |
| com.supcon.supfusion.module.registry:module-registry-api | module-registry | api | 6 | 0 | 6 | 0 | backend/modules/com/supcon/supfusion/module/registry/module-registry-api/1.0.1.RELEASE/META-INF/maven/com.supcon.supfusion.module.registry/module-registry-api/pom.xml |
| com.supcon.supfusion.module.registry:module-registry-dao | module-registry | dao | 4 | 0 | 4 | 0 | backend/modules/com/supcon/supfusion/module/registry/module-registry-dao/1.0.0-RELEASE/META-INF/maven/com.supcon.supfusion.module.registry/module-registry-dao/pom.xml |
| com.supcon.supfusion.module.registry:module-registry-dao | module-registry | dao | 4 | 0 | 4 | 0 | backend/modules/com/supcon/supfusion/module/registry/module-registry-dao/1.0.1.RELEASE/META-INF/maven/com.supcon.supfusion.module.registry/module-registry-dao/pom.xml |
| com.supcon.supfusion.module.registry:module-registry-resources | module-registry | resources | 0 | 0 | 0 | 0 | backend/modules/com/supcon/supfusion/module/registry/module-registry-resources/1.0.0-RELEASE/META-INF/maven/com.supcon.supfusion.module.registry/module-registry-resources/pom.xml |
| com.supcon.supfusion.module.registry:module-registry-resources | module-registry | resources | 0 | 0 | 0 | 0 | backend/modules/com/supcon/supfusion/module/registry/module-registry-resources/1.0.1.RELEASE/META-INF/maven/com.supcon.supfusion.module.registry/module-registry-resources/pom.xml |
| com.supcon.supfusion.module.registry:module-registry-server | module-registry | service | 22 | 6 | 16 | 0 | backend/modules/com/supcon/supfusion/module/registry/module-registry-server/1.0.0-RELEASE/META-INF/maven/com.supcon.supfusion.module.registry/module-registry-server/pom.xml |
| com.supcon.supfusion.module.registry:module-registry-server | module-registry | service | 22 | 6 | 16 | 0 | backend/modules/com/supcon/supfusion/module/registry/module-registry-server/1.0.1.RELEASE/META-INF/maven/com.supcon.supfusion.module.registry/module-registry-server/pom.xml |
| com.supcon.supfusion.module.registry:module-registry-webapi | module-registry | webapi | 6 | 2 | 4 | 0 | backend/modules/com/supcon/supfusion/module/registry/module-registry-webapi/1.0.0-RELEASE/META-INF/maven/com.supcon.supfusion.module.registry/module-registry-webapi/pom.xml |
| com.supcon.supfusion.module.registry:module-registry-webapi | module-registry | webapi | 6 | 2 | 4 | 0 | backend/modules/com/supcon/supfusion/module/registry/module-registry-webapi/1.0.1.RELEASE/META-INF/maven/com.supcon.supfusion.module.registry/module-registry-webapi/pom.xml |
| com.supcon.msgcenter:msgcenter | msgcenter | other | 7 | 0 | 7 | 0 | backend/modules/com/supcon/msgcenter/msgcenter/5.0.0.00/META-INF/maven/com.supcon.msgcenter/msgcenter/pom.xml |
| com.supcon.supfusion:organization-api | organization | api | 10 | 1 | 9 | 0 | backend/modules/com/supcon/supfusion/organization-api/1.0.0.RELEASE/META-INF/maven/com.supcon.supfusion/organization-api/pom.xml |
| com.supcon.supfusion:organization-api | organization | api | 10 | 1 | 9 | 0 | backend/modules/com/supcon/supfusion/organization-api/1.0.1.RELEASE/META-INF/maven/com.supcon.supfusion/organization-api/pom.xml |
| com.supcon.supfusion:organization-api | organization | api | 10 | 1 | 9 | 0 | backend/modules/com/supcon/supfusion/organization-api/1.2.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/organization-api/pom.xml |
| com.supcon.supfusion:organization-openapi | organization | api | 1 | 1 | 0 | 0 | backend/modules/com/supcon/supfusion/organization-openapi/1.0.0.RELEASE/META-INF/maven/com.supcon.supfusion/organization-openapi/pom.xml |
| com.supcon.supfusion:organization-common | organization | common | 8 | 1 | 7 | 0 | backend/modules/com/supcon/supfusion/organization-common/1.0.0.RELEASE/META-INF/maven/com.supcon.supfusion/organization-common/pom.xml |
| com.supcon.supfusion:organization-dao | organization | dao | 3 | 0 | 3 | 0 | backend/modules/com/supcon/supfusion/organization-dao/1.0.0.RELEASE/META-INF/maven/com.supcon.supfusion/organization-dao/pom.xml |
| com.supcon.supfusion:organization-manager | organization | manager | 5 | 4 | 1 | 0 | backend/modules/com/supcon/supfusion/organization-manager/1.0.0.RELEASE/META-INF/maven/com.supcon.supfusion/organization-manager/pom.xml |
| com.supcon.supfusion:organization-resources | organization | resources | 0 | 0 | 0 | 0 | backend/modules/com/supcon/supfusion/organization-resources/1.0.0.RELEASE/META-INF/maven/com.supcon.supfusion/organization-resources/pom.xml |
| com.supcon.supfusion:organization-service | organization | service | 7 | 4 | 3 | 0 | backend/modules/com/supcon/supfusion/organization-service/1.0.0.RELEASE/META-INF/maven/com.supcon.supfusion/organization-service/pom.xml |
| com.supcon.supfusion:organization-webapi | organization | webapi | 2 | 1 | 1 | 0 | backend/modules/com/supcon/supfusion/organization-webapi/1.0.0.RELEASE/META-INF/maven/com.supcon.supfusion/organization-webapi/pom.xml |
| com.supcon.supfusion:orgmanagement-auth | orgmanagement-auth | other | 7 | 7 | 0 | 0 | backend/modules/com/supcon/supfusion/orgmanagement-auth/1.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/orgmanagement-auth/pom.xml |
| com.supcon.supfusion:orgmanagement-organization | orgmanagement-organization | other | 11 | 8 | 3 | 0 | backend/modules/com/supcon/supfusion/orgmanagement-organization/1.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/orgmanagement-organization/pom.xml |
| com.supcon.supfusion:orgmanagement-rbac | orgmanagement-rbac | other | 14 | 8 | 6 | 0 | backend/modules/com/supcon/supfusion/orgmanagement-rbac/1.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/orgmanagement-rbac/pom.xml |
| com.supcon.supfusion:portal-common | portal | common | 2 | 0 | 2 | 0 | backend/modules/com/supcon/supfusion/portal-common/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/portal-common/pom.xml |
| com.supcon.supfusion:portal-dao | portal | dao | 4 | 1 | 3 | 0 | backend/modules/com/supcon/supfusion/portal-dao/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/portal-dao/pom.xml |
| com.supcon.supfusion:portal-manager | portal | manager | 3 | 3 | 0 | 0 | backend/modules/com/supcon/supfusion/portal-manager/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/portal-manager/pom.xml |
| com.supcon.supfusion:portal-service | portal | service | 6 | 2 | 4 | 0 | backend/modules/com/supcon/supfusion/portal-service/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/portal-service/pom.xml |
| com.supcon.supfusion:portal-webapi | portal | webapi | 8 | 2 | 6 | 0 | backend/modules/com/supcon/supfusion/portal-webapi/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/portal-webapi/pom.xml |
| com.supcon.supfusion:printer-api | printer | api | 3 | 0 | 3 | 0 | backend/modules/com/supcon/supfusion/printer-api/1.0.0.RELEASE/META-INF/maven/com.supcon.supfusion/printer-api/pom.xml |
| com.supcon.supfusion:printer-openapi | printer | api | 2 | 1 | 1 | 0 | backend/modules/com/supcon/supfusion/printer-openapi/1.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/printer-openapi/pom.xml |
| com.supcon.supfusion:printer-common | printer | common | 3 | 0 | 3 | 0 | backend/modules/com/supcon/supfusion/printer-common/1.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/printer-common/pom.xml |
| com.supcon.supfusion:printer-dao | printer | dao | 4 | 0 | 4 | 0 | backend/modules/com/supcon/supfusion/printer-dao/1.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/printer-dao/pom.xml |
| com.supcon.supfusion:printer-resources | printer | resources | 0 | 0 | 0 | 0 | backend/modules/com/supcon/supfusion/printer-resources/1.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/printer-resources/pom.xml |
| com.supcon.supfusion:printer-service | printer | service | 8 | 5 | 3 | 0 | backend/modules/com/supcon/supfusion/printer-service/1.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/printer-service/pom.xml |
| com.supcon.supfusion:printer-interapi | printer-interapi | other | 2 | 1 | 1 | 0 | backend/modules/com/supcon/supfusion/printer-interapi/1.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/printer-interapi/pom.xml |
| com.supcon.supfusion:protal-resources | protal | resources | 0 | 0 | 0 | 0 | backend/modules/com/supcon/supfusion/protal-resources/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/protal-resources/pom.xml |
| com.supcon.supfusion:rbac-api | rbac | api | 9 | 0 | 9 | 0 | backend/modules/com/supcon/supfusion/rbac-api/1.0.0.RELEASE/META-INF/maven/com.supcon.supfusion/rbac-api/pom.xml |
| com.supcon.supfusion:rbac-api | rbac | api | 9 | 0 | 9 | 0 | backend/modules/com/supcon/supfusion/rbac-api/1.0.2.RELEASE/META-INF/maven/com.supcon.supfusion/rbac-api/pom.xml |
| com.supcon.supfusion:rbac-api | rbac | api | 9 | 0 | 9 | 0 | backend/modules/com/supcon/supfusion/rbac-api/1.0.3.RELEASE/META-INF/maven/com.supcon.supfusion/rbac-api/pom.xml |
| com.supcon.supfusion:rbac-openapi | rbac | api | 1 | 1 | 0 | 0 | backend/modules/com/supcon/supfusion/rbac-openapi/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/rbac-openapi/pom.xml |
| com.supcon.supfusion:rbac-common | rbac | common | 7 | 0 | 7 | 0 | backend/modules/com/supcon/supfusion/rbac-common/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/rbac-common/pom.xml |
| com.supcon.supfusion:rbac-dao | rbac | dao | 4 | 1 | 3 | 0 | backend/modules/com/supcon/supfusion/rbac-dao/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/rbac-dao/pom.xml |
| com.supcon.supfusion:rbac-manager | rbac | manager | 6 | 5 | 1 | 0 | backend/modules/com/supcon/supfusion/rbac-manager/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/rbac-manager/pom.xml |
| com.supcon.supfusion:rbac-resources | rbac | resources | 0 | 0 | 0 | 0 | backend/modules/com/supcon/supfusion/rbac-resources/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/rbac-resources/pom.xml |
| com.supcon.supfusion:rbac-service | rbac | service | 19 | 6 | 13 | 0 | backend/modules/com/supcon/supfusion/rbac-service/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/rbac-service/pom.xml |
| com.supcon.supfusion:rbac-webapi | rbac | webapi | 8 | 2 | 6 | 0 | backend/modules/com/supcon/supfusion/rbac-webapi/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/rbac-webapi/pom.xml |
| com.supcon.supfusion:rbac-urlscan | rbac-urlscan | other | 6 | 0 | 6 | 0 | backend/modules/com/supcon/supfusion/rbac-urlscan/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/rbac-urlscan/pom.xml |
| com.supcon.supfusion.framework:scaffold-auditlog | scaffold-auditlog | other | 24 | 0 | 24 | 0 | backend/modules/com/supcon/supfusion/framework/scaffold-auditlog/1.0.4.RELEASE/META-INF/maven/com.supcon.supfusion.framework/scaffold-auditlog/pom.xml |
| com.supcon.supfusion.framework:scaffold-auditlog | scaffold-auditlog | other | 24 | 0 | 24 | 0 | backend/modules/com/supcon/supfusion/framework/scaffold-auditlog/1.0.5.RELEASE/META-INF/maven/com.supcon.supfusion.framework/scaffold-auditlog/pom.xml |
| com.supcon.supfusion.framework:scaffold-auditlog | scaffold-auditlog | other | 24 | 0 | 24 | 0 | backend/modules/com/supcon/supfusion/framework/scaffold-auditlog/1.0.6.RELEASE/META-INF/maven/com.supcon.supfusion.framework/scaffold-auditlog/pom.xml |
| com.supcon.supfusion.framework:scaffold-auditlog | scaffold-auditlog | other | 24 | 0 | 24 | 0 | backend/modules/com/supcon/supfusion/framework1/scaffold-auditlog/1.0.4.RELEASE/META-INF/maven/com.supcon.supfusion.framework/scaffold-auditlog/pom.xml |
| com.supcon.supfusion.framework.boot:scaffold-boot-auditlog-autoconfigure | scaffold-boot-auditlog-autoconfigure | other | 8 | 4 | 4 | 0 | backend/modules/com/supcon/supfusion/framework/boot/scaffold-boot-auditlog-autoconfigure/1.0.4.RELEASE/META-INF/maven/com.supcon.supfusion.framework.boot/scaffold-boot-auditlog-autoconfigure/pom.xml |
| com.supcon.supfusion.framework.boot:scaffold-boot-auditlog-autoconfigure | scaffold-boot-auditlog-autoconfigure | other | 8 | 4 | 4 | 0 | backend/modules/com/supcon/supfusion/framework/boot/scaffold-boot-auditlog-autoconfigure/1.0.5.RELEASE/META-INF/maven/com.supcon.supfusion.framework.boot/scaffold-boot-auditlog-autoconfigure/pom.xml |
| com.supcon.supfusion.framework.boot:scaffold-boot-auditlog-autoconfigure | scaffold-boot-auditlog-autoconfigure | other | 8 | 4 | 4 | 0 | backend/modules/com/supcon/supfusion/framework/boot/scaffold-boot-auditlog-autoconfigure/1.0.6.RELEASE/META-INF/maven/com.supcon.supfusion.framework.boot/scaffold-boot-auditlog-autoconfigure/pom.xml |
| com.supcon.supfusion.framework.boot:scaffold-boot-auditlog-autoconfigure | scaffold-boot-auditlog-autoconfigure | other | 8 | 4 | 4 | 0 | backend/modules/com/supcon/supfusion/framework1/boot/scaffold-boot-auditlog-autoconfigure/1.0.4.RELEASE/META-INF/maven/com.supcon.supfusion.framework.boot/scaffold-boot-auditlog-autoconfigure/pom.xml |
| com.supcon.supfusion.framework.boot:scaffold-boot-kafka-autoconfigure | scaffold-boot-kafka-autoconfigure | other | 16 | 2 | 14 | 0 | backend/modules/com/supcon/supfusion/framework/boot/scaffold-boot-kafka-autoconfigure/1.0.3.RELEASE/META-INF/maven/com.supcon.supfusion.framework.boot/scaffold-boot-kafka-autoconfigure/pom.xml |
| com.supcon.supfusion.framework.boot:scaffold-boot-kafka-autoconfigure | scaffold-boot-kafka-autoconfigure | other | 16 | 2 | 14 | 0 | backend/modules/com/supcon/supfusion/framework1/boot/scaffold-boot-kafka-autoconfigure/1.0.3.RELEASE/META-INF/maven/com.supcon.supfusion.framework.boot/scaffold-boot-kafka-autoconfigure/pom.xml |
| com.supcon.supfusion.framework.boot:scaffold-boot-redis-autoconfigure | scaffold-boot-redis-autoconfigure | other | 10 | 2 | 8 | 0 | backend/modules/com/supcon/supfusion/framework/boot/scaffold-boot-redis-autoconfigure/1.0.3.RELEASE/META-INF/maven/com.supcon.supfusion.framework.boot/scaffold-boot-redis-autoconfigure/pom.xml |
| com.supcon.supfusion.framework.boot:scaffold-boot-redis-autoconfigure | scaffold-boot-redis-autoconfigure | other | 10 | 2 | 8 | 0 | backend/modules/com/supcon/supfusion/framework1/boot/scaffold-boot-redis-autoconfigure/1.0.3.RELEASE/META-INF/maven/com.supcon.supfusion.framework.boot/scaffold-boot-redis-autoconfigure/pom.xml |
| com.supcon.supfusion.framework.boot:scaffold-boot-starter-auditlog | scaffold-boot-starter-auditlog | other | 4 | 4 | 0 | 0 | backend/modules/com/supcon/supfusion/framework/boot/scaffold-boot-starter-auditlog/1.0.4.RELEASE/META-INF/maven/com.supcon.supfusion.framework.boot/scaffold-boot-starter-auditlog/pom.xml |
| com.supcon.supfusion.framework.boot:scaffold-boot-starter-auditlog | scaffold-boot-starter-auditlog | other | 4 | 4 | 0 | 0 | backend/modules/com/supcon/supfusion/framework/boot/scaffold-boot-starter-auditlog/1.0.5.RELEASE/META-INF/maven/com.supcon.supfusion.framework.boot/scaffold-boot-starter-auditlog/pom.xml |
| com.supcon.supfusion.framework.boot:scaffold-boot-starter-auditlog | scaffold-boot-starter-auditlog | other | 4 | 4 | 0 | 0 | backend/modules/com/supcon/supfusion/framework/boot/scaffold-boot-starter-auditlog/1.0.6.RELEASE/META-INF/maven/com.supcon.supfusion.framework.boot/scaffold-boot-starter-auditlog/pom.xml |
| com.supcon.supfusion.framework.boot:scaffold-boot-starter-auditlog | scaffold-boot-starter-auditlog | other | 4 | 4 | 0 | 0 | backend/modules/com/supcon/supfusion/framework1/boot/scaffold-boot-starter-auditlog/1.0.4.RELEASE/META-INF/maven/com.supcon.supfusion.framework.boot/scaffold-boot-starter-auditlog/pom.xml |
| com.supcon.supfusion.framework.boot:scaffold-boot-starter-kafka | scaffold-boot-starter-kafka | other | 2 | 2 | 0 | 0 | backend/modules/com/supcon/supfusion/framework/boot/scaffold-boot-starter-kafka/1.0.3.RELEASE/META-INF/maven/com.supcon.supfusion.framework.boot/scaffold-boot-starter-kafka/pom.xml |
| com.supcon.supfusion.framework.boot:scaffold-boot-starter-kafka | scaffold-boot-starter-kafka | other | 2 | 2 | 0 | 0 | backend/modules/com/supcon/supfusion/framework1/boot/scaffold-boot-starter-kafka/1.0.3.RELEASE/META-INF/maven/com.supcon.supfusion.framework.boot/scaffold-boot-starter-kafka/pom.xml |
| com.supcon.supfusion.framework.boot:scaffold-boot-starter-redis | scaffold-boot-starter-redis | other | 8 | 2 | 6 | 0 | backend/modules/com/supcon/supfusion/framework/boot/scaffold-boot-starter-redis/1.0.3.RELEASE/META-INF/maven/com.supcon.supfusion.framework.boot/scaffold-boot-starter-redis/pom.xml |
| com.supcon.supfusion.framework.boot:scaffold-boot-starter-redis | scaffold-boot-starter-redis | other | 8 | 2 | 6 | 0 | backend/modules/com/supcon/supfusion/framework1/boot/scaffold-boot-starter-redis/1.0.3.RELEASE/META-INF/maven/com.supcon.supfusion.framework.boot/scaffold-boot-starter-redis/pom.xml |
| com.supcon.supfusion.framework:scaffold-kafka | scaffold-kafka | other | 2 | 0 | 2 | 0 | backend/modules/com/supcon/supfusion/framework/scaffold-kafka/1.0.3.RELEASE/META-INF/maven/com.supcon.supfusion.framework/scaffold-kafka/pom.xml |
| com.supcon.supfusion.framework:scaffold-kafka | scaffold-kafka | other | 2 | 0 | 2 | 0 | backend/modules/com/supcon/supfusion/framework1/scaffold-kafka/1.0.3.RELEASE/META-INF/maven/com.supcon.supfusion.framework/scaffold-kafka/pom.xml |
| com.supcon.supfusion.framework:scaffold-redis | scaffold-redis | other | 8 | 0 | 8 | 0 | backend/modules/com/supcon/supfusion/framework/scaffold-redis/1.0.3.RELEASE/META-INF/maven/com.supcon.supfusion.framework/scaffold-redis/pom.xml |
| com.supcon.supfusion.framework:scaffold-redis | scaffold-redis | other | 8 | 0 | 8 | 0 | backend/modules/com/supcon/supfusion/framework1/scaffold-redis/1.0.3.RELEASE/META-INF/maven/com.supcon.supfusion.framework/scaffold-redis/pom.xml |
| com.supcon.supfusion.signature:signature-base | signature | common | 12 | 5 | 7 | 0 | backend/modules/com/supcon/supfusion/signature/signature-base/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion.signature/signature-base/pom.xml |
| com.supcon.supfusion.signature:signature-dao | signature | dao | 7 | 1 | 6 | 0 | backend/modules/com/supcon/supfusion/signature/signature-dao/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion.signature/signature-dao/pom.xml |
| com.supcon.supfusion.signature:signature-service | signature | service | 10 | 2 | 8 | 0 | backend/modules/com/supcon/supfusion/signature/signature-service/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion.signature/signature-service/pom.xml |
| com.supcon.supfusion.signature:signature-inter-api | signature-inter | api | 5 | 1 | 4 | 0 | backend/modules/com/supcon/supfusion/signature/signature-inter-api/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion.signature/signature-inter-api/pom.xml |
| com.supcon.supfusion.signature:signature-resouces | signature-resouces | other | 0 | 0 | 0 | 0 | backend/modules/com/supcon/supfusion/signature/signature-resouces/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion.signature/signature-resouces/pom.xml |
| com.supcon.supfusion.signature:signature-service-openapi | signature-service | api | 3 | 1 | 2 | 0 | backend/modules/com/supcon/supfusion/signature/signature-service-openapi/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion.signature/signature-service-openapi/pom.xml |
| com.supcon.supfusion.notification:sms-openapi | sms | api | 1 | 1 | 0 | 0 | backend/modules/com/supcon/supfusion/notification/sms-openapi/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion.notification/sms-openapi/pom.xml |
| com.supcon.supfusion.notification:sms-common | sms | common | 3 | 0 | 3 | 0 | backend/modules/com/supcon/supfusion/notification/sms-common/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion.notification/sms-common/pom.xml |
| com.supcon.supfusion.notification:sms-dao | sms | dao | 2 | 0 | 2 | 0 | backend/modules/com/supcon/supfusion/notification/sms-dao/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion.notification/sms-dao/pom.xml |
| com.supcon.supfusion.notification:sms-manager | sms | manager | 5 | 1 | 4 | 0 | backend/modules/com/supcon/supfusion/notification/sms-manager/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion.notification/sms-manager/pom.xml |
| com.supcon.supfusion.notification:sms-resources | sms | resources | 0 | 0 | 0 | 0 | backend/modules/com/supcon/supfusion/notification/sms-resources/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion.notification/sms-resources/pom.xml |
| com.supcon.supfusion.notification:sms-service | sms | service | 6 | 4 | 2 | 0 | backend/modules/com/supcon/supfusion/notification/sms-service/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion.notification/sms-service/pom.xml |
| com.supcon.supfusion.notification:sms-config | sms-config | other | 3 | 1 | 2 | 0 | backend/modules/com/supcon/supfusion/notification/sms-config/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion.notification/sms-config/pom.xml |
| com.supcon.supfusion:sysmanagement-counter | sysmanagement-counter | other | 4 | 2 | 2 | 0 | backend/modules/com/supcon/supfusion/sysmanagement-counter/1.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/sysmanagement-counter/pom.xml |
| com.supcon.supfusion:sysmanagement-module-registry | sysmanagement-module-registry | other | 5 | 3 | 2 | 0 | backend/modules/com/supcon/supfusion/sysmanagement-module-registry/1.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/sysmanagement-module-registry/pom.xml |
| com.supcon.supfusion:sysmanagement-systemcode | sysmanagement-systemcode | other | 10 | 8 | 2 | 0 | backend/modules/com/supcon/supfusion/sysmanagement-systemcode/1.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/sysmanagement-systemcode/pom.xml |
| com.supcon.supfusion:sysmanagement-systemconfig | sysmanagement-systemconfig | other | 7 | 5 | 2 | 0 | backend/modules/com/supcon/supfusion/sysmanagement-systemconfig/1.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/sysmanagement-systemconfig/pom.xml |
| com.supcon.supfusion:system-config-api | system-config | api | 17 | 3 | 14 | 0 | backend/modules/com/supcon/supfusion/system-config-api/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/system-config-api/pom.xml |
| com.supcon.supfusion:system-config-api | system-config | api | 17 | 3 | 14 | 0 | backend/modules/com/supcon/supfusion/system-config-api/1.0.0.RELEASE/META-INF/maven/com.supcon.supfusion/system-config-api/pom.xml |
| com.supcon.supfusion:system-config-api | system-config | api | 17 | 3 | 14 | 0 | backend/modules/com/supcon/supfusion/system-config-api/1.1.1.RELEASE/META-INF/maven/com.supcon.supfusion/system-config-api/pom.xml |
| com.supcon.supfusion:system-config-openapi | system-config | api | 12 | 2 | 10 | 0 | backend/modules/com/supcon/supfusion/system-config-openapi/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/system-config-openapi/pom.xml |
| com.supcon.supfusion:system-config-common | system-config | common | 2 | 0 | 2 | 0 | backend/modules/com/supcon/supfusion/system-config-common/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/system-config-common/pom.xml |
| com.supcon.supfusion:system-config-common | system-config | common | 2 | 0 | 2 | 0 | backend/modules/com/supcon/supfusion/system-config-common/1.1.1.RELEASE/META-INF/maven/com.supcon.supfusion/system-config-common/pom.xml |
| com.supcon.supfusion:system-config-dao | system-config | dao | 4 | 1 | 3 | 0 | backend/modules/com/supcon/supfusion/system-config-dao/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/system-config-dao/pom.xml |
| com.supcon.supfusion:system-config-manager | system-config | manager | 3 | 1 | 2 | 0 | backend/modules/com/supcon/supfusion/system-config-manager/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/system-config-manager/pom.xml |
| com.supcon.supfusion:system-config-resources | system-config | resources | 0 | 0 | 0 | 0 | backend/modules/com/supcon/supfusion/system-config-resources/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/system-config-resources/pom.xml |
| com.supcon.supfusion:system-config-service | system-config | service | 20 | 3 | 17 | 0 | backend/modules/com/supcon/supfusion/system-config-service/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/system-config-service/pom.xml |
| com.supcon.supfusion:system-config-webapi | system-config | webapi | 15 | 2 | 13 | 0 | backend/modules/com/supcon/supfusion/system-config-webapi/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/system-config-webapi/pom.xml |
| com.supcon.supfusion:system-config-client | system-config-client | other | 5 | 0 | 5 | 0 | backend/modules/com/supcon/supfusion/system-config-client/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/system-config-client/pom.xml |
| com.supcon.supfusion:systemcode-api | systemcode | api | 17 | 2 | 15 | 0 | backend/modules/com/supcon/supfusion/systemcode-api/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/systemcode-api/pom.xml |
| com.supcon.supfusion:systemcode-api | systemcode | api | 17 | 2 | 15 | 0 | backend/modules/com/supcon/supfusion/systemcode-api/1.0.0.RELEASE/META-INF/maven/com.supcon.supfusion/systemcode-api/pom.xml |
| com.supcon.supfusion:systemcode-api | systemcode | api | 17 | 2 | 15 | 0 | backend/modules/com/supcon/supfusion/systemcode-api/1.0.1.RELEASE/META-INF/maven/com.supcon.supfusion/systemcode-api/pom.xml |
| com.supcon.supfusion:systemcode-api | systemcode | api | 17 | 2 | 15 | 0 | backend/modules/com/supcon/supfusion/systemcode-api/1.0.4.RELEASE/META-INF/maven/com.supcon.supfusion/systemcode-api/pom.xml |
| com.supcon.supfusion:systemcode-api | systemcode | api | 17 | 2 | 15 | 0 | backend/modules/com/supcon/supfusion/systemcode-api/1.0.5.RELEASE/META-INF/maven/com.supcon.supfusion/systemcode-api/pom.xml |
| com.supcon.supfusion:systemcode-openapi | systemcode | api | 1 | 1 | 0 | 0 | backend/modules/com/supcon/supfusion/systemcode-openapi/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/systemcode-openapi/pom.xml |
| com.supcon.supfusion:systemcode-common | systemcode | common | 2 | 0 | 2 | 0 | backend/modules/com/supcon/supfusion/systemcode-common/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/systemcode-common/pom.xml |
| com.supcon.supfusion:systemcode-dao | systemcode | dao | 3 | 0 | 3 | 0 | backend/modules/com/supcon/supfusion/systemcode-dao/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/systemcode-dao/pom.xml |
| com.supcon.supfusion:systemcode-manager | systemcode | manager | 6 | 4 | 2 | 0 | backend/modules/com/supcon/supfusion/systemcode-manager/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/systemcode-manager/pom.xml |
| com.supcon.supfusion:systemcode-resources | systemcode | resources | 0 | 0 | 0 | 0 | backend/modules/com/supcon/supfusion/systemcode-resources/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/systemcode-resources/pom.xml |
| com.supcon.supfusion:systemcode-service | systemcode | service | 7 | 4 | 3 | 0 | backend/modules/com/supcon/supfusion/systemcode-service/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/systemcode-service/pom.xml |
| com.supcon.supfusion:systemcode-webapi | systemcode | webapi | 9 | 1 | 8 | 0 | backend/modules/com/supcon/supfusion/systemcode-webapi/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/systemcode-webapi/pom.xml |
| com.supcon.supfusion.flow:task-center | task-center | other | 21 | 10 | 11 | 0 | backend/modules/com/supcon/supfusion/flow/task-center/1.0.0-RELEASE/META-INF/maven/com.supcon.supfusion.flow/task-center/pom.xml |
| com.supcon.supfusion:theme-common | theme | common | 3 | 0 | 3 | 0 | backend/modules/com/supcon/supfusion/theme-common/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/theme-common/pom.xml |
| com.supcon.supfusion:theme-dao | theme | dao | 3 | 0 | 3 | 0 | backend/modules/com/supcon/supfusion/theme-dao/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/theme-dao/pom.xml |
| com.supcon.supfusion:theme-service | theme | service | 6 | 2 | 4 | 0 | backend/modules/com/supcon/supfusion/theme-service/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/theme-service/pom.xml |
| com.supcon.supfusion:theme-webapi | theme | webapi | 6 | 1 | 5 | 0 | backend/modules/com/supcon/supfusion/theme-webapi/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/theme-webapi/pom.xml |
| com.supcon.supfusion:websocket-service | websocket | service | 14 | 0 | 14 | 0 | backend/modules/com/supcon/supfusion/websocket-service/1.0.0-SNAPSHOT/META-INF/maven/com.supcon.supfusion/websocket-service/pom.xml |
| com.supcon.supfusion:websocket-service-api | websocket-service | api | 3 | 0 | 3 | 0 | backend/modules/com/supcon/supfusion/websocket-service-api/1.0.0.RELEASE/META-INF/maven/com.supcon.supfusion/websocket-service-api/pom.xml |

## 使用规则

- `backend/modules` 是恢复源码参考区，不直接纳入 Maven reactor。
- 提升模块前先查看本清单中的 family、layer、内部依赖和 Oracle/JDBC 风险。
- 新模块复制到 `backend/source-modules/<module>` 后，使用根父 POM 重新声明最小依赖。
- Oracle JDBC 只能保留在 legacy profile 或迁移说明中，不能进入默认 PostgreSQL 路径。
- 修改恢复 POM、提升模块或新增来源包后，运行 `make backend-dependency-inventory`。
