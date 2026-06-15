# 后端落表业务排查交接

## 交接目标

给后续专门线程一个清晰任务：把 ADP/MES 后端从“能跑的恢复包”拆成可理解的业务落表地图，支撑 Oracle 到 PostgreSQL 替换和产品说明书编写。

最终要回答：

- 每个菜单/页面调用哪些 API。
- 每个 API 进入哪个 Controller、Service、Mapper。
- 每个 Mapper 操作哪些表和字段。
- 表字段代表什么业务含义。
- 哪些表来自平台元数据，哪些表来自业务模块。
- PostgreSQL 迁移缺口在哪里，是否需要保留 Oracle legacy SQL。
- 哪些前端业务动作已经通过真实页面操作和 PostgreSQL 查询证明落库。

## 输入资料

本仓库内：

- `backend/modules/`：sources.jar 恢复源码，优先可信。
- `backend/decompiled-services/`：反编译服务壳和缺失类。
- `backend/services/`：服务清单、启动类、Nacos Data ID。
- `deploy/docker/postgres/init/`：当前 PostgreSQL 兼容 SQL。
- `deploy/docker/scripts/audit-postgres-mappings.py`：SQL 方言审计。
- `docs/functional-persistence-acceptance.md`：功能验收和落库验收硬规则。
- `docs/frontend-functional-test-report.md`：前端功能验收报告。
- `docs/backend-table-audit/persistence-acceptance.md`：后端落库验收报告。
- `metadata/persistence-acceptance.json`：机器可读落库验收记录。
- `metadata/backend-service-manifest.json`：运行服务清单。
- `metadata/backend-source-summary.json`：恢复模块清单。
- `docs/adp-system-design-logic.md`：平台/业务边界判断。

运行环境：

- 测试环境前端：`http://10.11.100.17:18080`
- 默认测试账号：`admin / 123456`
- PostgreSQL 数据库来自 Docker profile，优先在测试环境只读审计。

## 输出目录

建议后续线程把模块报告放到：

```text
docs/backend-table-audit/
  00-index.md
  platform-auth-rbac-org.md
  platform-entity-config.md
  platform-flow-todo.md
  business-quality-lims-qcs.md
  business-production.md
  business-equipment-energy-ehs.md
```

## 标准报告模板

每个模块报告建议包含：

```text
# 模块名称

## 结论
- 已验证：
- 未验证：
- 阻断项：

## 菜单和页面
| 菜单 | 前端路由 | 页面文件 | 备注 |

## API 入口
| 方法 | URL | Controller | Service | 权限 |

## 落表路径
| 业务动作 | Mapper/XML | 表 | 字段 | 读/写 | PostgreSQL 风险 |

## 表说明
| 表 | 来源 | 业务含义 | 主键 | 关键字段 | 初始化脚本 |

## Oracle 替换项
| 文件 | Oracle/MySQL 方言 | PostgreSQL 改法 | 状态 |

## Smoke 证据
- API smoke：
- 页面 smoke：
- SQL/日志证据：
```

## 排查方法

### 1. 从页面到 API

- 用浏览器打开菜单。
- 记录 Network 中的 `/api`、`/inter-api`、`/open-api`、`/msService` 请求。
- 对照前端恢复源码搜索 URL、菜单名、国际化 key。

### 2. 从 API 到 Java

```bash
rg -n "接口路径|Controller|RequestMapping|GetMapping|PostMapping" backend/modules backend/decompiled-services
```

确认：

- Controller 所属服务。
- Service/Manager 调用链。
- Mapper/XML 位置。
- Nacos 配置是否影响数据源、租户、权限。

### 3. 从 Mapper 到表

重点看：

- XML 中的 `select`、`insert`、`update`、`delete`。
- Java 注解 SQL。
- 动态 SQL 分支。
- 是否引用 Oracle/MySQL 方言。
- 是否引用实体配置元数据表。

### 4. 从表到业务含义

优先来源：

1. 原厂 init.xml / SQL / 菜单资源。
2. Java 实体类、DTO、枚举、字段注释。
3. 前端列名、表头、表单 label、国际化 key。
4. 运行库中已有数据和页面行为。
5. 用户业务确认。

## PostgreSQL 专项注意

不要把“缺表/缺列/字段类型不兼容”当成前端问题。当前 ADP 包的典型 PostgreSQL 缺口包括：

- 元数据表缺失：`ec_entity`、`ec_model`、`ec_field`、`ec_view`。
- 平台字段缺失：组织、权限、待办、菜单相关扩展字段。
- Boolean 与 `0/1` 混用。
- Oracle 函数：`nvl`、`decode`、`sysdate`、`rownum`、`from dual`。
- `to_char` 这类 PostgreSQL 也支持的函数按 warning 处理，需要确认格式语义是否和 Oracle 原逻辑一致。
- 表名大小写或 legacy view 兼容。

每个修复都应落成幂等 SQL，不要通过清库重建绕过。

## 功能与落库验收专项注意

后续落表排查不能只停留在 Mapper 或表字段地图。凡是报告为 `PASS` 的写业务动作，都必须满足：

- 来自真实前端页面或等效 E2E 操作。
- 请求带唯一 marker，且记录 method、URL、payload、response。
- 追踪到 Controller、Service、Mapper/DAO 和目标表。
- 直接查询 PostgreSQL，记录 SQL 和结果摘要。

完整规则见 [功能验收与落库验收规则](functional-persistence-acceptance.md)。

## 第一批建议排查模块

1. `auth` / `iam` / `rbac` / `organization`
   - 目标：登录、用户、组织、角色、菜单、权限。
   - 价值：解决所有业务模块的权限和菜单根基。

2. `configuration` / `system-config` / `systemcode` / `ec_*`
   - 目标：实体配置、元数据表、动态页面。
   - 价值：业务模块页面和表结构大量依赖这里。

3. `flow` / `task-scheduler`
   - 目标：流程、待办、后台任务。
   - 价值：生产/质量/设备流程类业务都会依赖。

4. `LIMS` / `QCS` / `Qualify`
   - 目标：质量域边界，区分 QC/LIMS/QCS 和完整 QA/QMS。
   - 价值：用户已经明确关注质量管理类设计。

5. 生产、设备、能源、安环模块
   - 目标：业务闭环和产品说明书。
   - 价值：真正决定 MES 产品形态。

## 完成定义

某个模块可认为排查完成，需要同时满足：

- 菜单、API、服务、Mapper、表字段链路完整。
- PostgreSQL 迁移缺口列清楚，已有修复或有明确 backlog。
- 业务动作的读写表有证据。
- 页面/API smoke 或等价手工验证可复现。
- 会改变业务数据的动作有 PostgreSQL 落库验收证据。
- 文档标注已验证、未验证和风险，不把猜测写成事实。
