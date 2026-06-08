# notification-app

- 原始 JAR: `bap-server/base-Server/notification-app/notification-app.jar`
- Main-Class: `org.springframework.boot.loader.JarLauncher`
- Start-Class: `com.supcon.supfusion.notification.app.bootstrap.NoticeAppBootstrap`
- Spring Boot: `2.1.5.RELEASE`
- 嵌入依赖数量: `154`

## 配置入口
- JAR 内: `BOOT-INF/classes/application.yml`
- JAR 内: `META-INF/maven/com.supcon.supfusion.notification/app-bootstrap/pom.properties`
- JAR 内: `BOOT-INF/classes/bootstrap.yml`

## Nacos Data IDs
- `supfusion-i18n-system.properties`
- `supfusion-jwt-common.properties`
- `supfusion-notification-app.properties`
- `supfusion-registry.properties`

## 端口和应用名
- `supfusion-notification-app.properties`: `forceChange:30235`
- `supfusion-notification-app.properties`: `forceChange:notification-app`
