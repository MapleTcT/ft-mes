# customProperty

- 原始 JAR: `bap-server/base-Server/customProperty/custom-property.jar`
- Main-Class: `org.springframework.boot.loader.JarLauncher`
- Start-Class: `com.supcon.supfusion.custon.property.CoustomPropertyBootstrap`
- Spring Boot: `2.1.5.RELEASE`
- 嵌入依赖数量: `220`

## 配置入口
- JAR 内: `BOOT-INF/classes/application.yml`
- JAR 内: `BOOT-INF/classes/bootstrap.yml`
- JAR 内: `BOOT-INF/classes/application-dev.yml`
- JAR 内: `META-INF/maven/com.supcon.supfusion/custom-property-bootstrap/pom.properties`
- 外部文件: `application.yml`
- 外部文件: `bootstrap.yml`

## Nacos Data IDs
- `supfusion-coustom-property.properties`
- `supfusion-datasource-system.properties`
- `supfusion-i18n-system.properties`
- `supfusion-jwt-common.properties`
- `supfusion-mybatis-common.properties`
- `supfusion-registry.properties`

## 端口和应用名
- `supfusion-coustom-property.properties`: `30210`
- `supfusion-coustom-property.properties`: `customProperty`
