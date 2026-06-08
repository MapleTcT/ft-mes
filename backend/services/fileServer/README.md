# fileServer

- 原始 JAR: `bap-server/base-Server/fileServer/supfusion-file-server.jar`
- Main-Class: `org.springframework.boot.loader.JarLauncher`
- Start-Class: `com.supcon.supfusion.file.server.Bootstrap`
- Spring Boot: `2.1.5.RELEASE`
- 嵌入依赖数量: `229`

## 配置入口
- JAR 内: `BOOT-INF/classes/application-dev.yml`
- JAR 内: `BOOT-INF/classes/application.yml`
- JAR 内: `BOOT-INF/classes/bootstrap.yml`
- JAR 内: `META-INF/maven/com.supcon.supfusion/file-server-bootstrap/pom.properties`
- 外部文件: `bootstrap.yml`

## Nacos Data IDs
- `supfusion-config-center-system.properties`
- `supfusion-datasource-system.properties`
- `supfusion-file-server.properties`
- `supfusion-i18n-system.properties`
- `supfusion-jwt-common.properties`
- `supfusion-registry.properties`

## 端口和应用名
- `supfusion-file-server.properties`: `30191`
- `supfusion-file-server.properties`: `file-server`
