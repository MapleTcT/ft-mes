# notification-admin

- 原始 JAR: `bap-server/base-Server/notification-admin/notification-admin.jar`
- Main-Class: `org.springframework.boot.loader.JarLauncher`
- Start-Class: `com.supcon.supfusion.notification.admin.bootstrap.Bootstrap`
- Spring Boot: `2.1.5.RELEASE`
- 嵌入依赖数量: `228`

## 配置入口
- JAR 内: `BOOT-INF/classes/application-dev.yml`
- JAR 内: `META-INF/maven/com.supcon.supfusion.notification/admin-bootstrap/pom.properties`
- JAR 内: `BOOT-INF/classes/application.yml`
- JAR 内: `BOOT-INF/classes/bootstrap.yml`
- 外部文件: `bootstrap.yml`

## Nacos Data IDs
- `supfusion-datasource-system.properties`
- `supfusion-i18n-system.properties`
- `supfusion-jwt-common.properties`
- `supfusion-kafka-system.properties`
- `supfusion-mybatis-common.properties`
- `supfusion-notification-admin.properties`
- `supfusion-registry.properties`

## 端口和应用名
- `supfusion-notification-admin.properties`: `30102`
- `supfusion-notification-admin.properties`: `notification-admin`
