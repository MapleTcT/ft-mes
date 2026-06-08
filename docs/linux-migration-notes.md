# Linux 迁移说明

## 不能直接复用的 Windows 组件

原始包中这些组件是 Windows 形态，Linux 迁移时必须替换：

- `Commands/nssm/*`
- `bap-server/assembly/jdk1.8/bin/*.exe`
- `bap-server/assembly/Redis-x64-5.0.9/*.exe`
- `bap-server/base-Server/fileServer/minio.exe`
- `bap-server/base-Server/fileServer/ffmpeg/ffmpeg.exe`
- `nginx/nginx.exe`
- 所有 `.bat` / `.cmd` 服务脚本

## 建议的 Linux 启动顺序

按 Windows `Commands/startServices.bat` 还原出的基础顺序：

1. Nginx
2. Zookeeper
3. Nacos
4. Keycloak
5. Kafka
6. Redis
7. WebSocket
8. i18n
9. sysmanagement / basicmanagement / orgmanagement / iam
10. gateway
11. license / configuration
12. operatetools / baseService / flow
13. notification 系列
14. MinIO / fileServer
15. task-scheduler / customProperty

实际 Linux 编排建议用健康检查替代固定 sleep。

## 端口概览

- Nginx: `8080`
- Gateway: `8008`
- Nacos: `8848`
- Redis: `6379`
- Zookeeper: `2181`
- Kafka: `9092`
- WebSocket: `30135`
- i18n: `30130`
- baseService: `30020`
- configuration: `30000`
- flow: `30150`
- iam: `30160`
- fileServer: `30191`
- task-scheduler: `30195`

完整端口以 `metadata/backend-service-manifest.json` 和 `deploy/nacos-config/*.properties` 为准。

## 待确认事项

- 目标 Linux 发行版、CPU 架构和 JDK 版本。
- 系统数据库采用 Oracle、MariaDB 还是其他数据库。
- Keycloak 是否沿用内置 H2，还是迁移到独立数据库。
- license 服务是否依赖硬件授权狗或 Windows DLL。
- MinIO 数据目录、访问密钥和文件迁移策略。
- 是否需要把所有服务做成 systemd unit，还是优先 Docker Compose。
