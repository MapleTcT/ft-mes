# configuration

- 原始 JAR: `bap-server/base-Server/configuration/supfusion-configuration.jar`
- Main-Class: `org.springframework.boot.loader.JarLauncher`
- Start-Class: `com.supcon.supfusion.configuration.services.Bootstrap`
- Spring Boot: `2.1.5.RELEASE`
- 嵌入依赖数量: `246`

## 配置入口
- JAR 内: `BOOT-INF/classes/application-dev.yml`
- JAR 内: `BOOT-INF/classes/application.yml`
- JAR 内: `META-INF/maven/com.supcon.supfusion.configuration/configuration-services-bootstrap/pom.properties`
- JAR 内: `BOOT-INF/classes/bootstrap.yml`
- 外部文件: `bootstrap.yml`

## Nacos Data IDs
- `supfusion-configuration-services.properties`
- `supfusion-datasource-system.properties`
- `supfusion-i18n-system.properties`
- `supfusion-jwt-common.properties`
- `supfusion-mybatis-common.properties`
- `supfusion-redis-system.properties`
- `supfusion-registry.properties`

## 端口和应用名
- `supfusion-configuration-services.properties`: `30000`
