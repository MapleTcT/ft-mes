# printer

- 原始 JAR: `bap-server/base-Server/printer/supfusion-printer.jar`
- Main-Class: `org.springframework.boot.loader.JarLauncher`
- Start-Class: `com.supcon.supfusion.printer.bootstrap.Bootstrap`
- Spring Boot: `2.1.5.RELEASE`
- 嵌入依赖数量: `218`

## 配置入口
- JAR 内: `BOOT-INF/classes/application-dev.yml`
- JAR 内: `BOOT-INF/classes/application.yml`
- JAR 内: `META-INF/maven/com.supcon.supfusion/printer-bootstrap/pom.properties`
- JAR 内: `BOOT-INF/classes/bootstrap.yml`

## Nacos Data IDs
- `supfusion-config-center-system.properties`
- `supfusion-datasource-system.properties`
- `supfusion-i18n-system.properties`
- `supfusion-jwt-common.properties`
- `supfusion-mybatis-common.properties`
- `supfusion-printer.properties`
- `supfusion-registry.properties`

## 端口和应用名
- `supfusion-printer.properties`: `forceChange:30216`
- `supfusion-printer.properties`: `printer`
