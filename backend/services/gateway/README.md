# gateway

- 原始 JAR: `bap-server/base-Server/gateway/supos-gateway.jar`
- Main-Class: `org.springframework.boot.loader.JarLauncher`
- Start-Class: `com.supcon.supos.suposgateway.SuposGatewayApplication`
- Spring Boot: `2.1.12.RELEASE`
- 嵌入依赖数量: `166`

## 配置入口
- JAR 内: `BOOT-INF/classes/bootstrap.yml`
- 外部文件: `bootstrap.yml`

## Nacos Data IDs
- `supfusion-gateway.properties`
- `supfusion-jwt-common.properties`
- `supfusion-redis-system.properties`
- `supfusion-registry.properties`

## 端口和应用名
- `supfusion-gateway.properties`: `8008`
- `supfusion-gateway.properties`: `supos-gateway`
