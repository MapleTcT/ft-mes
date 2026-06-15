# 项目总目标验收总账

## 结论

当前总目标仍是 `IN_PROGRESS_NOT_COMPLETE`。

本总账用于把“可持续开发仓库、Oracle 替换、平台功能验证、生产模块完整验证、PostgreSQL 缺口治理、生产迁移前置项”放在同一张可复验账本里。它不替代真实前端测试、后端落库验收或生产迁移演练；它负责防止局部 smoke 通过后误判为整体完成。

机器可读记录见 `metadata/project-goal-acceptance.json`，校验命令：

```bash
make project-goal-acceptance-check
```

## 状态口径

| 状态 | 含义 |
| --- | --- |
| `READY` | 当前证据足以证明该目标项已完成。 |
| `PARTIAL` | 有可复验证据，但覆盖范围不足或仍有明确缺口。 |
| `BLOCKED` | 有上游阻断，无法完成验收闭环。 |
| `FAIL` | 已执行验证但结果与预期不符。 |
| `NOT_STARTED` | 尚未形成可复验证据。 |

## 当前总览

| 指标 | 数量 |
| --- | ---: |
| 目标项 | 20 |
| READY | 8 |
| PARTIAL | 10 |
| BLOCKED | 2 |
| FAIL | 0 |
| NOT_STARTED | 0 |

## 目标项明细

| ID | 目标项 | 状态 | 当前证据 | 缺口 |
| --- | --- | --- | --- | --- |
| G-001 | 可持续开发仓库基础 | READY | `make ci` 通过；父 POM、Makefile、CI、库存和门禁已建立 | 继续随模块提升补测试 |
| G-002 | 当前内容迁移 | PARTIAL | `docs/current-content-inventory.md` 和源码恢复目录 | 业务模块动作级源码/表关系仍需继续排查 |
| G-003 | Oracle 替换为 PostgreSQL 默认路径 | PARTIAL | Docker/POM 默认 PostgreSQL；Oracle 只允许 legacy/profile/backlog；用户锁定/解锁暴露的 `auth_user.error_count` 缺列已用 `073-auth-user-lock-status-compat.sql` 纳入 PostgreSQL init；角色用户绑定暴露的 `rbac_roleuser.valid` 默认值缺口已用 `074-rbac-roleuser-valid-default.sql` 纳入 PostgreSQL init；RBAC 数据资源权限缺表和角色资源权限继承落库缺口已用 `075-rbac-data-resource-permission-tables.sql` 纳入 PostgreSQL init | Oracle backlog 仍有引用，需要逐模块迁移 |
| G-004 | 项目目标与交接说明 | READY | `docs/project-objectives.md`、`docs/sustainable-development.md` | 随真实验收继续更新 |
| G-005 | 后端落表排查交接入口 | READY | `docs/backend-table-audit-handoff.md`、`docs/backend-table-audit/00-index.md` | 表级业务含义仍需专门线程执行 |
| G-006 | 平台登录/认证 | READY | `metadata/persistence-acceptance.json` 平台 smoke PASS | 保持回归 smoke |
| G-007 | 用户/人员/岗位/公司/组管理 | READY | 组织部门/组/岗位/公司/人员 CRUD 已落库；人员勾选创建账号已落库；独立用户管理账号新增/编辑/锁定/解锁/删除已落库；角色新增/编辑/删除、角色绑定/解绑用户已落库 | 保持回归 smoke |
| G-008 | 组织管理 | PARTIAL | 部门、组、岗位、公司、人员新增/编辑/删除已用 marker 查 PostgreSQL；人员 `createUser=true` 已证明 `org_person` 和 `auth_user` 绑定、同步和软删除 | 权限关联关系仍需动作级验收 |
| G-009 | 权限/RBAC | READY | RBAC authority smoke PASS；角色权限页和用户权限页已用 marker 验收 `personmanage/addPerson` 菜单操作权限新增/删除，`rbac_rolepermission`、`rbac_userpermission` 均有 PostgreSQL 证据；数据资源权限 `saveRoleSourceAuth/saveUserSourceAuth` 已用 marker `ADP_E2E_20260615155455_RBAC` 验收 `rbac_role_data_permission`、`rbac_role_data_permission_ctrl`、`rbac_user_data_permission`、`rbac_user_data_permission_ctrl` | 保持回归 smoke |
| G-010 | 菜单导航 | READY | 菜单抽样真实浏览器 smoke PASS | 继续扩大菜单覆盖 |
| G-011 | 待办 | READY | 首页待办/top-nav 待办 smoke 已有证据 | 继续保留回归 |
| G-012 | 基础配置 | PARTIAL | 配置类 init SQL 和业务页面 smoke 有覆盖 | 基础配置页面 CRUD/落库验收未形成完整闭环 |
| G-013 | 生产模块完整功能 | BLOCKED | 生产页面 smoke 和动作源码地图已建立 | WOM 动作页 React #130；生产写动作无 marker 落库证据 |
| G-014 | Nacos 配置链路 | PARTIAL | render/publish 脚本和测试环境配置路径存在 | 生产差异清单、漂移检查和回退演练未补 |
| G-015 | Keycloak/JWT 链路 | PARTIAL | realm 初始化、JWT public key 同步脚本和登录 smoke | 生产 realm/用户/client/secret 迁移演练未补 |
| G-016 | PostgreSQL 运行与迁移治理 | PARTIAL | 75 个 init SQL、PostgreSQL migration index、mapper audit 0 error；`073-auth-user-lock-status-compat.sql` 已覆盖 `auth_user.error_count` 兼容缺口；`074-rbac-roleuser-valid-default.sql` 已覆盖 `rbac_roleuser.valid` 默认值兼容缺口；`075-rbac-data-resource-permission-tables.sql` 已覆盖 RBAC 数据资源权限缺表和继承同步兼容缺口 | 生产数据迁移和每个业务动作落库仍未完成 |
| G-017 | runtime patch | PARTIAL | runtime patch 脚本和业务 view JSON SQL 已建立 | WOM 动作页面仍阻断，生产 patch 版本/回退演练未补 |
| G-018 | 业务模块完整测试用例 | PARTIAL | 生产模块测试矩阵和 53 个业务页面 smoke | 动作级测试和业务签字未完成 |
| G-019 | PostgreSQL 缺口进入幂等 SQL/backlog | PARTIAL | Oracle/PostgreSQL 审计、init SQL、backlog 已纳入 CI；本轮 RBAC 验收发现并闭合 `rbac_roleuser.valid` 默认值缺口、RBAC 数据资源权限缺表、角色资源权限继承用户行未落库缺口 | 不能证明所有未来发现项已闭环，需要持续记录 |
| G-020 | 生产迁移前置项 | BLOCKED | 生产迁移模板和 readiness 账本已建立 | 数据迁移演练、回滚、license、MinIO、Keycloak、TLS、安全加固、业务 smoke 签字均未 READY |

## 当前最高优先级缺口

1. 恢复 WOM 真实 edit/view component metadata，解决生产动作页 React #130。
2. 补基础配置页面的写动作落库验收。
3. 对生产主流程执行 marker 写动作，并查询 `WOM_*` 相关表证明状态、报工、备料、退料等字段变化。
4. 把生产迁移模板升级为 rehearsal 证据，但在业务 smoke 签字前保持 `NOT_READY_FOR_PRODUCTION_MIGRATION`。

## 更新规则

- 任何目标项改为 `READY` 前，必须有当前仓库内 artifact 或真实运行证据说明。
- 平台、生产和基础配置功能不能只用源码推断，必须回写 `docs/frontend-functional-test-report.md`、`docs/backend-table-audit/persistence-acceptance.md` 和 `metadata/persistence-acceptance.json`。
- 生产模块完整功能未通过前，`G-013` 必须保持 `BLOCKED` 或 `FAIL`。
- 生产迁移签字未完成前，`G-020` 必须保持 `BLOCKED` 或 `PARTIAL`。
- 总状态只有在全部目标项 `READY` 后才能改为 `COMPLETE`。
