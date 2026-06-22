# 可持续开发仓库说明

## 目标

本仓库从“恢复源码仓库”升级为“可持续开发仓库”的第一步已经完成：根目录提供 Maven 父级 POM、后端/部署聚合入口、统一验证命令和 Docker 编排入口。

当前阶段不把 `backend/modules/**/META-INF/maven/**/pom.xml` 直接纳入 Maven reactor。那些 POM 来自 `sources.jar` 元数据，缺少原厂父工程、私服依赖、构建插件和模块顺序，强行纳入会得到一个表面完整但不可维护的工程。

项目级工作指令见根目录 [`AGENTS.md`](../AGENTS.md)。继续开发、模块接入、测试环境修复和交付验收时，必须先按该指令确认真实前端功能、后端写动作和 PostgreSQL 落库证据；`make ci` 和静态扫描只是仓库门禁，不替代功能验收。

总目标完成状态见 [`project-goal-acceptance.md`](project-goal-acceptance.md)。它把当前目标拆成可持续开发、内容迁移、Oracle 替换、平台功能、生产模块、Nacos/Keycloak/PostgreSQL/runtime patch、缺口治理和生产迁移前置项。只要该总账仍是 `IN_PROGRESS_NOT_COMPLETE`，仓库就不能被描述为整体目标已完成。

当前缺口交接视图见 [`goal-gap-register.md`](goal-gap-register.md) 和
`metadata/goal-gap-register.json`。`make goal-gap-register-check` 会从项目总账、
生产 blocker、生产 backlog、业务依赖 readiness、导出 readiness 和生产迁移账本重新生成该视图，
防止后续线程只看到局部 smoke PASS 就误判整体目标完成。

基础配置专项边界见 [`basic-config-coverage.md`](basic-config-coverage.md) 和
`metadata/basic-config-coverage.json`。`make basic-config-coverage-check`
会校验系统编码、系统配置、内置配置、实体/模型运行时和 Nacos/Keycloak
配置链路的覆盖状态，避免把系统编码/系统配置局部 PASS 误判成 G-012 整体完成。

## 开发分区

```text
pom.xml                         # 仓库父级 POM，统一 Java/依赖/插件基线
backend/pom.xml                 # 后端聚合入口
backend/source-modules/         # 新开发或已提升模块的可编译源码区
backend/modules/                # 恢复源码参考区，保持原 Maven 坐标目录
backend/decompiled-services/    # 反编译启动壳和缺失源码参考
backend/services/               # 运行服务清单
deploy/pom.xml                  # 部署资料聚合入口
deploy/docker/                  # Linux Docker Compose 测试部署
deploy/database/                # 数据库迁移和兼容策略入口
docs/runtime-validation-scope.md # 测试环境验证范围和生产迁移边界
docs/project-goal-acceptance.md # 总目标验收总账
docs/goal-gap-register.md       # 当前未完成缺口交接总账
Makefile                        # 常用开发、验证、部署命令
AGENTS.md                       # 当前项目工作指令和功能验收优先规则
.github/workflows/verify.yml    # GitHub Actions 验证入口
.github/ISSUE_TEMPLATE/         # Oracle 迁移和后端落表排查模板
```

## Maven 基线

父级 POM 默认管理：

- Java 8 编译目标。
- Spring Boot `2.1.5.RELEASE`，来自恢复服务清单。
- Spring Cloud `Greenwich.SR2`。
- Spring Cloud Alibaba `2.1.1.RELEASE`。
- PostgreSQL JDBC `42.2.8`，和当前 Docker 运行补丁保持一致。
- 默认 `dependencyManagement` 不管理 Oracle JDBC；Oracle JDBC 只保留在 `oracle-legacy` profile 中，用于老库对比或显式迁移工具。
- 已提升到 `backend/source-modules` 的可编译模块不得直接声明 Oracle JDBC；默认源码路径也不能带入 Oracle 运行配置或 mapper 资源。

可用 profile：

- `postgres-first`：默认启用，表示新模块优先按 PostgreSQL 适配。
- `oracle-legacy`：仅用于保留老模块回连 Oracle 的兼容构建，默认构建不会启用。
- `business-modules`：给后续业务模块批量构建/测试预留的开关。

## 模块提升规则

后续把恢复源码或新业务包变成可编译模块时，按下面顺序推进：

1. 对来源包运行 `make module-intake-check INTAKE=/path/to/package-or-dir`。
2. 在 `backend/source-modules/<module>` 新建标准 Maven 模块。
3. 从 `backend/modules/<group>/<artifact>/<version>` 复制可信源码，不直接移动原始恢复目录。
4. 新模块继承根 `ft-mes-parent`。
5. 只声明当前模块真实需要的依赖，避免把整套运行包依赖一次性灌入。
6. DAO/Mapper 层必须同时接受 PostgreSQL smoke 或 SQL audit。
7. 业务逻辑和数据库方言分离，禁止在 service 层散落 `if oracle/postgres` 分支。
8. 提升完成后，把模块加入 `backend/source-modules/pom.xml` 的 `<modules>`。
9. Oracle 对照资料只能作为 legacy/backlog 资料保留在默认 `src/main` 外，不能进入默认构建产物。

推荐模块结构：

```text
backend/source-modules/<module>/
  pom.xml
  src/main/java/
  src/main/resources/
  src/test/java/
```

## 常用命令

首次运行浏览器 smoke 前安装 Node 依赖：

```bash
npm install
npx playwright install chromium
```

```bash
make verify
make ci
make verify-pom
make compose-config
make runtime-script-check
make sustainable-check
make ci-required-file-inventory
make ci-required-file-inventory-check
make ci-required-file-strict-check
make project-goal-acceptance-check
make goal-gap-register-check
make persistence-acceptance-check
make production-testcase-check
make production-blocker-check
make production-module-backlog-check
make production-action-map-check
make production-migration-readiness-check
make production-rehearsal-plan
make production-rehearsal-plan-check
make source-module-check
make module-intake-precheck-regression-check
make module-intake-candidate-report-check
make source-module-test
make create-backend-module MODULE=platform-auth
make module-intake-check INTAKE=/path/to/package-or-dir
make inventory
make inventory-check
make backend-dependency-inventory
make backend-dependency-check
make oracle-audit
make oracle-audit-check
make postgres-migration-index
make postgres-migration-check
make oracle-replacement-status
make oracle-replacement-check
make render-config
make up-infra
make up
make runtime-smoke-reports-check
make smoke-platform
make smoke-api
make smoke-menu
make smoke-todo
make business-dependency-readiness-check
make audit-postgres-mappings
make audit-postgres-report
```

`make verify` 只验证 Maven reactor 和 Docker Compose 语法，不会启动容器，也不会修改数据库。`make ci` 会额外检查仓库治理规则、CI/治理依赖文件库存是否新鲜、后端 source module 结构、source module 默认路径是否 Oracle-free、module intake 预检行为回归、真实候选包准入报告、已提升 source module 编译测试、内容库存是否新鲜、恢复后端依赖库存是否新鲜、Oracle 迁移 backlog 是否新鲜、PostgreSQL 初始化脚本索引是否新鲜、Oracle 替换状态总账是否新鲜，以及 PostgreSQL 方言审计。

`make ci-required-file-inventory-check` 校验 `metadata/ci-required-file-inventory.json` 是否与当前 CI、治理账本、runtime smoke 证据、runtime patch、Nacos/Nginx/runtime 静态覆盖和生产迁移门禁依赖的文件集合一致；该报告会显式列出每个依赖文件是 `tracked` 还是 `untracked`，并按顶层目录和二级目录汇总未跟踪项，防止本地 `make ci` 依赖未纳入 Git 的证据资产。`make ci-required-file-strict-check` 是提交/发布前检查，要求库存中没有缺失引用且所有非忽略依赖文件都已经被 Git 跟踪；它不放进默认 CI，是为了允许开发过程中先生成再整理，但提交和交接前必须通过。只要 strict inventory 仍有未跟踪必需资产，`docs/project-goal-acceptance.md` 中的 `G-001` 必须保持 `PARTIAL`，不能宣称可持续开发仓库基础已经完全 `READY`；当前库存数量以 `metadata/ci-required-file-inventory.json` 和 [项目总目标验收总账](project-goal-acceptance.md) 为准，避免在说明文档里手写重复资产数。`make module-intake-candidate-report-check` 校验已提交的真实候选包准入报告 [最新基础模块准入预检报告](module-intake-latest-basic-modules.md) 和 `metadata/module-intake-latest-basic-modules.json`；它不重扫本机外部目录，但会确保阻断项、报告状态、扫描策略、扫描覆盖和处理动作没有从交接证据里丢失。

`make runtime-script-check` 只做 smoke 与 runtime patch 脚本语法检查，不访问远程环境。`make sustainable-check` 会校验父级 POM、PostgreSQL 默认路径、必备文档/账本、二进制和大文件策略，并要求 `.gitignore` 持续忽略 `__pycache__`、`*.pyc`、`node_modules`、`target`、`dist`、本地 `.env`、Nacos 渲染输出、日志和缓存；如果这些生成物被跟踪进仓库会直接失败。少数旧运行包必须注入的 `.class` 补丁只能走显式 allowlist，并且必须保留相邻 `.java` 源文件。`make project-goal-acceptance-check` 校验 [项目总目标验收总账](project-goal-acceptance.md) 和 `metadata/project-goal-acceptance.json` 的覆盖范围，防止把局部 PASS 误报成整体完成；它不替代真实功能验收。`make goal-gap-register-check` 校验 [目标缺口总账](goal-gap-register.md) 和 `metadata/goal-gap-register.json` 是否仍从项目目标、生产 blocker/backlog、依赖 readiness、导出 readiness 和生产迁移账本生成，用于交接当前不能标记完成的真实缺口。`make persistence-acceptance-check` 校验功能验收和落库验收资产的结构，不代表功能已经通过；真实结论必须来自 [功能验收与落库验收规则](functional-persistence-acceptance.md) 要求的浏览器操作、API 记录、后端链路和 PostgreSQL 查询。`make production-testcase-check` 校验 [生产模块功能测试用例矩阵](production-module-functional-test-cases.md)；`make production-blocker-check` 校验 [生产模块阻断项账本](production-module-blockers.md) 与测试矩阵中的 BLOCKED 用例完全一致，并要求每个阻断项有证据、复验入口、PASS 条件和非解法；`make production-module-backlog-check` 校验 [生产模块 Backlog 账本](production-module-backlog.md) 覆盖所有落库验收 `FAIL/BLOCKED` 和生产 `BLOCKED` 用例，尤其防止 public `produceTaskCreated` 这类 HTTP 200 但不落库的接口被误当成 PASS。`make postgres-migration-check` 校验 PostgreSQL init SQL 编号、派生索引、高风险清库语句和 watch 安全规则；`DROP DATABASE/SCHEMA/TABLE` 与 `TRUNCATE` 会失败，允许的 watch 语句必须是 `DROP ... IF EXISTS` 或 `DELETE ... WHERE`，说明见 [PostgreSQL Watch 语句说明](postgres-migration-watch-rationale.md)。`make runtime-smoke-reports-check` 离线校验 `metadata/test-environment-smoke.json`、`metadata/postgres-runtime-smoke.json`、`metadata/nacos-config-drift-smoke.json`、`metadata/keycloak-jwt-runtime-smoke.json` 和 `metadata/minio-runtime-smoke.json` 的结构、PASS 状态、当前测试主机、关键检查和敏感信息卫生；它不访问测试环境，远程状态变化必须通过对应 `make smoke-*` 重新生成报告。`make test-environment-static-bundle-link-check` 离线校验 `metadata/test-environment-static-bundle-link-smoke.json`、WOM 工单 toolbar smoke 报告和相关文档，要求保留当前 `100.99.133.43` 直连浏览器入口因 DERP/静态大包传输失败而阻断、同一测试环境公网入口可完成真实点击/落库但仍有 ProcessAnalysis 503 和二维码 404 blocker 的证据。`make business-dependency-readiness-check` 离线校验 `metadata/business-dependency-readiness-smoke.json` 和相关文档是否覆盖 material/ProcessAnalysis 外部依赖，并强制报告仍指向当前 `100.99.133.43` 测试主机、`prod` Nacos group、Nacos/PostgreSQL 容器和 API base；若依赖被标为 READY，还必须有健康 Nacos 服务、HTTP 2xx 端点和 material/ProcessAnalysis 数据库证据；它不访问测试环境，远程状态变化必须通过 `make smoke-business-dependencies` 刷新报告。`make production-export-readiness-check` 离线校验 `metadata/production-export-readiness-smoke.json` 仍用当前 API base `http://100.99.133.43:18080`，并允许同一测试环境公网浏览器入口 `http://222.88.185.146:18080` 覆盖 6 个生产导出目标的真实浏览器页面、runtime layout、downloadXls 文件响应和目标 sourceAudit 证据。`make production-migration-readiness-check` 校验 [生产迁移就绪账本](production-migration-readiness.md) 和 `metadata/production-migration-readiness.json` 是否覆盖数据迁移、回滚、license、MinIO、Keycloak、Nacos/runtime patch、端口/TLS、安全加固和业务签字；`make production-rehearsal-plan-check` 校验 [生产演练计划](production-migration/rehearsal-plan.md) 和 `metadata/production-rehearsal-plan.json` 是否仍与 readiness、cutover、runtime smoke、生产 blocker/backlog 账本一致；它们都不代表生产迁移已完成。`make production-rollback-evidence-check` 校验回滚证据 manifest 的结构和覆盖面；`make production-rollback-ready-check` 只应用于真实演练 evidence，所有回滚组件未 READY 时必须失败。`make production-license-strategy-check` 校验生产 license 决策 evidence 的结构和覆盖面；`make production-license-ready-check` 只应用于真实生产决策，测试 bypass 被带入生产时必须失败。`make production-network-tls-check` 校验生产域名/TLS/服务暴露模板结构；`make production-network-tls-ready-check` 只应用于真实生产 evidence，域名、证书、代理、防火墙、HSTS 和外部 HTTPS smoke 未 READY 时必须失败。`make production-security-hardening-check` 校验生产安全加固模板结构；`make production-security-hardening-ready-check` 只应用于真实生产 evidence，账号轮换、测试账号清理、secret manager、数据库权限、镜像扫描、容器运行用户、审计日志和告警未 READY 时必须失败。`make production-business-smoke-signoff-check` 校验业务 smoke 签字模板结构；`make production-business-smoke-signoff-ready-check` 只应用于真实生产签字 evidence，平台/基础配置/生产主流程/QCS-LIMS/PostgreSQL gap 和 owner signoff 未 READY 时必须失败。`make smoke-platform` 是测试环境平台验证入口，会串联 API、主页待办和菜单页面 smoke，并输出统一 JSON 报告。`make module-intake-check` 是新业务包和恢复模块的只读准入检查，会扫描目录、源码文件、外层 zip/jar/war/ear，以及常见内层 zip/jar/war/ear；报告必须保留 `scanPolicy` 和 `scanCoverage`，说明文本读取上限、嵌套归档深度、已读取文本、超大文本跳过、嵌套路径保留和不可检查压缩包；发现默认路径 Oracle 残留、`.7z/.rar/.tar.gz` 等不可检查压缩包，或覆盖字段丢失都会返回非 0；`make module-intake-precheck-regression-check` 会用临时干净/污染 fixture、嵌套归档 fixture 和不可检查归档 fixture 回归验证该预检确实能拦住 Oracle POM、`mapper/oracle` 资源和不透明业务包，并验证扫描覆盖字段没有丢失。`make audit-postgres-mappings` 是阻断式审计，发现 Oracle/MySQL/SQL Server 方言会返回非 0。`make audit-postgres-report` 用于生成阶段性报告，不阻断当前工作。

`make production-evidence-ready-gate-regression-check` 是生产迁移证据的负向回归门禁，会伪造 rollback、license、network/TLS、security hardening 和 business smoke 的 READY 文件，并伪造生产迁移 readiness 总账和 cutover 总闸门的 READY 状态，确认 strict-ready 或总账校验器拒绝 `.example`、`template`、`sample` 等模板 evidence。这个检查已纳入 `make ci`，用来防止模板资产被误当成生产演练、签字证据或生产切换许可。

## 后续工作

- 将高频修改的基础模块优先提升到 `backend/source-modules`。
- 提升模块前先用 `make create-backend-module MODULE=<name>` 创建标准结构，提升过程见 [后端模块提升指南](backend-module-promotion-guide.md)。
- 新业务包进入源码区前先用 `make module-intake-check INTAKE=/path/to/package-or-dir` 扫描，并建立独立模块组，不直接污染基础平台模块。
- 将 PostgreSQL 兼容补丁从 runtime patch 逐步前移到源码、Mapper 和迁移脚本。
- 给每个已提升模块补最小 smoke 或集成测试。
- 按 [项目总目标验收总账](project-goal-acceptance.md) 维护 `metadata/project-goal-acceptance.json`，只有全部目标项 `READY` 才能宣称总目标完成。
- 按 [目标缺口总账](goal-gap-register.md) 维护 `metadata/goal-gap-register.json`，让后续业务包、后端落表和生产迁移线程能直接看到仍未 READY 的目标项、生产阻断、backlog、依赖和迁移轨道。
- 按 [后端落表业务排查交接](backend-table-audit-handoff.md) 建立模块级表字段地图。
- 按 [功能验收与落库验收规则](functional-persistence-acceptance.md) 维护 [前端功能测试报告](frontend-functional-test-report.md)、[后端落库验收报告](backend-table-audit/persistence-acceptance.md) 和 `metadata/persistence-acceptance.json`。
- 按 [生产模块阻断项账本](production-module-blockers.md) 维护 `metadata/production-module-blockers.json`，业务包或产品决策补齐后先更新 blocker，再进入真实 marker 验收。
- 按 [生产模块 Backlog 账本](production-module-backlog.md) 维护 `metadata/production-module-backlog.json`，新增落库 `FAIL/BLOCKED` 或生产 `BLOCKED` 时必须同步登记处理方向，避免 HTTP 200 假成功、缺服务、缺外部客户端或缺导出实现被漏掉。
- 按 [生产迁移就绪账本](production-migration-readiness.md) 维护 `metadata/production-migration-readiness.json`，并用 [生产演练计划](production-migration/rehearsal-plan.md) / `metadata/production-rehearsal-plan.json` 串起一次 rehearsal 需要收集的 DB、MinIO、Keycloak、Nacos/runtime、license、TLS、安全和业务签字证据；生产迁移前必须补数据迁移脚本、回滚方案、license 策略、MinIO 文件迁移、Keycloak 生产库策略、端口/域名/TLS evidence、安全加固 evidence 和业务 smoke signoff evidence；业务 smoke signoff 必须交叉校验 `metadata/production-module-blockers.json`，未解决的 `PROD-*` 阻断 case 不能被遗漏。
- 新增或移除运行服务、源码包、前端应用后，运行 `make inventory` 更新 [当前内容迁移清单](current-content-inventory.md)。
- 新增、移除或修改 `backend/modules` 恢复源码 POM 后，运行 `make backend-dependency-inventory` 更新 [后端恢复模块依赖库存](backend-module-dependency-inventory.md)。
- 新增、删除或修改 Oracle/ojdbc/Oracle 方言引用后，运行 `make oracle-audit` 更新 [Oracle 迁移 Backlog](oracle-migration-backlog.md)。
- 新增、删除或修改 `deploy/docker/postgres/init/*.sql` 后，运行 `make postgres-migration-index` 更新 [PostgreSQL 迁移脚本索引](postgres-migration-index.md) 和 [PostgreSQL Watch 语句说明](postgres-migration-watch-rationale.md)。
- 更新任一 Oracle 退场相关清单后，运行 `make oracle-replacement-status` 更新 [Oracle 替换状态总账](oracle-replacement-status.md)。
- 新增、删除或移动 CI 脚本、治理账本、runtime smoke 报告、生产迁移模板或 PostgreSQL init SQL 后，运行 `make ci-required-file-inventory` 更新 `metadata/ci-required-file-inventory.json`；提交前运行 `make ci-required-file-strict-check`，确保关键依赖资产已被 Git 跟踪。
