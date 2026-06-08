# flow

- 原始 JAR: `bap-server/base-Server/flow/supfusion-flow.jar`
- Main-Class: `org.springframework.boot.loader.JarLauncher`
- Start-Class: `com.supcon.supfusion.flow.bootstrap.Bootstrap`
- Spring Boot: `2.1.5.RELEASE`
- 嵌入依赖数量: `304`

## 配置入口
- JAR 内: `BOOT-INF/classes/application-dev.yml`
- JAR 内: `META-INF/maven/com.supcon.supfusion.flow/flow-bootstrap/pom.properties`
- JAR 内: `BOOT-INF/classes/application.yml`
- JAR 内: `BOOT-INF/classes/bootstrap.yml`
- 外部文件: `logback-prod.xml`

## Nacos Data IDs
- `supfusion-datasource-system.properties`
- `supfusion-flow.properties`
- `supfusion-i18n-system.properties`
- `supfusion-jwt-common.properties`
- `supfusion-kafka-system.properties`
- `supfusion-mybatis-common.properties`
- `supfusion-redis-system.properties`
- `supfusion-registry.properties`

## 端口和应用名
- `supfusion-flow.properties`: `30150`
- `supfusion-flow.properties`: `flow-service`
