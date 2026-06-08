# task-scheduler

- 原始 JAR: `bap-server/base-Server/task-scheduler/supfusion-task-scheduler-service.jar`
- Main-Class: `org.springframework.boot.loader.JarLauncher`
- Start-Class: `com.supcon.supfusion.scheduler.Bootstrap`
- Spring Boot: `2.1.5.RELEASE`
- 嵌入依赖数量: `194`

## 配置入口
- JAR 内: `BOOT-INF/classes/application.yml`
- JAR 内: `META-INF/maven/com.supcon.supfusion/cloud-task-scheduler-server-bootstrap/pom.properties`
- JAR 内: `BOOT-INF/classes/bootstrap.yml`
- 外部文件: `application.yml`
- 外部文件: `bootstrap.yml`

## Nacos Data IDs
- `supfusion-datasource-system.properties`
- `supfusion-i18n-system.properties`
- `supfusion-jwt-common.properties`
- `supfusion-mybatis-common.properties`
- `supfusion-registry.properties`
- `supfusion-task-scheduler.properties`

## 端口和应用名
- `supfusion-task-scheduler.properties`: `30195`
- `supfusion-task-scheduler.properties`: `task-scheduler`
