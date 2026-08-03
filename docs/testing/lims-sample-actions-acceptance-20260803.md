# 样品管理按钮与弹窗恢复验收

## 验收结论

- 验收时间：2026-08-03
- 测试环境：`http://10.11.100.17:18080`
- 数据库：PostgreSQL 15
- 测试代码基线：`fc8ed235a31e7b6751060d00dd1eb6f38379900a` 加本轮工作树
- 总结：24/24 页面可打开，16 个操作页的 29/29 个原产品按钮可见，8 个原产品查询或流程页不追加伪造动作，整体 `PASS`

本轮修复的不只是菜单权限。缺口同时存在于列表视图按钮脚本、标准化按钮
元数据、动作弹窗视图和模块静态多语言资源。四层恢复后，再以 `admin` 的真实
页面、网络请求、浏览器错误和 PostgreSQL 权限记录共同验收。

## 页面与按钮

| 业务阶段 | 页面 | 原产品动作 | 页面结果 |
|---|---|---|---|
| 登记与取样 | 样品登记 | 登记、设置检测项目、取消 | PASS |
| 登记与取样 | 计划登样 | 生成样品 | PASS |
| 登记与取样 | 批量登样 | 查询/批量录入页，无独立列表动作 | NOT_APPLICABLE |
| 登记与取样 | 取样任务分配 | 任务分配 | PASS |
| 登记与取样 | 样品取样 | 取样、取样信息设置 | PASS |
| 收样与制备 | 样品收样 | 收样、设置检测项目 | PASS |
| 收样与制备 | 扫码收样 | 提交、删行 | PASS |
| 收样与制备 | 样品制样 | 制样 | PASS |
| 收样与制备 | 样品交接 | 领用 | PASS |
| 结果录入与复核 | 按项目批量录入 | 结果录入工作台，无独立列表动作 | NOT_APPLICABLE |
| 结果录入与复核 | 单样品录入结果 | 环境条件记录 | PASS |
| 结果录入与复核 | 按项目录入结果 | 结果录入工作台，无独立列表动作 | NOT_APPLICABLE |
| 结果录入与复核 | 按样品录入结果 | 环境条件记录 | PASS |
| 结果录入与复核 | 按样品结果复核 | 结果复核工作台，无独立列表动作 | NOT_APPLICABLE |
| 结果录入与复核 | 按项目结果复核 | 刷新 | PASS |
| 审核与处置 | 样品审核 | 样品查看 | PASS |
| 审核与处置 | 样品拒绝 | 流程任务页，无独立列表动作 | NOT_APPLICABLE |
| 审核与处置 | 样品接受 | 流程任务页，无独立列表动作 | NOT_APPLICABLE |
| 审核与处置 | 样品处理 | 样品信息修改、设置检测项目、取消、恢复、重新取样检验、激活、删除 | PASS |
| 审核与处置 | 剩余样品处理 | 归还、销毁 | PASS |
| 审核与处置 | 样品留样 | 留样 | PASS |
| 台账与报告 | 检验进度查询 | 查询页，无独立列表动作 | NOT_APPLICABLE |
| 台账与报告 | 样品台账 | 样品查看、处理记录查看 | PASS |
| 台账与报告 | 样品检验报告 | 报表页，无独立列表动作 | NOT_APPLICABLE |

`NOT_APPLICABLE` 表示供应商原视图没有独立列表按钮，并非权限遗漏。本轮没有
添加“复制”“部分取样”等未在对应父视图发布的动作，避免制造无法落库或语义
不明的按钮。

## 浏览器验收

自动验收入口：
`deploy/docker/scripts/adp-lims-sample-actions-acceptance.js`

| 检查项 | 实际结果 | 状态 |
|---|---:|---|
| 登录 | `admin` 登录 HTTP 200 | PASS |
| 样品管理分组 | 5 个阶段、24 个页面 | PASS |
| 页面打开 | 24/24 HTTP 200 | PASS |
| 操作页 | 16/16 | PASS |
| 原产品按钮 | 29/29 可见，缺失 0 | PASS |
| 可见系统错误 | 0 | PASS |
| Console / page error | 0 / 0 | PASS |
| XHR/Fetch 4xx/5xx | 0 | PASS |
| 原始多语言 key | 0 | PASS |
| 持续加载遮罩 | 0 | PASS |

安全点击覆盖“样品登记”“任务分配”“剩余样品归还”。样品登记完整弹窗及
“参照样品模板 / 提交 / 取消”均成功渲染，并产生一条只读配置请求：
`POST /msService/LIMSBasic/sampleType/customPropPart/customPropertyNeedDisplay`；
其余两个动作分别正常打开或给出已翻译的“请先选择记录”提示。没有执行提交、
删除、销毁等会改变业务数据的动作。

浏览器原始报告：
`/tmp/adp-lims-sample-actions-20260803-1711/acceptance.json`。

## PostgreSQL 与权限验收

对恢复范围直接查库，结果如下：

| 数据库断言 | 实际结果 | 状态 |
|---|---:|---|
| 样品管理阶段分组 | 5 | PASS |
| 分组下有效页面 | 24 | PASS |
| 已发布、显示且受权限控制的恢复按钮 | 29 | PASS |
| 按钮 `permission_code` 对应有效 `rbac_menuoperate` | 29/29 | PASS |
| admin 角色有效授权 | 29/29 | PASS |
| 恢复父列表视图 | 16 | PASS |
| 恢复动作弹窗视图 | 16 | PASS |

权限核对 SQL 的核心连接关系为：

```sql
SELECT count(DISTINCT button.code)
FROM public.runtime_button button
JOIN public.rbac_menuoperate operation
  ON operation.code = button.permission_code
 AND operation.valid = 1
JOIN public.rbac_rolepermission permission
  ON permission.menuoperate_id = operation.id
 AND permission.role_id = 1
 AND permission.delete_time IS NULL
WHERE button.module_code IN ('LIMSSample_5.0.0.0', 'LIMSBasic_1.0.0')
  AND button.is_published = true
  AND button.is_permission = true
  AND button.is_hide = false;
```

实际返回 `29`。这证明按钮发布、操作定义和 admin 授权三者一致，不是前端
绕过权限强制显示。

## 修复资产

- 父列表动作：`deploy/docker/postgres/init/258-lims-sample-actions-runtime.sql`
- 按钮发布与权限元数据：`deploy/docker/postgres/init/259-lims-sample-actions-button-metadata.sql`
- 动作弹窗视图：`deploy/docker/postgres/init/260-lims-sample-action-dialog-runtime.sql`
- 原包视图生成器：`deploy/docker/scripts/generate-business-view-runtime-sql.py`
- LIMSSample 多语言生成：`deploy/docker/scripts/prepare-lims-sample-static-assets.sh`
- 模块静态资源路由：`deploy/docker/nginx/adp.conf`
- 机器记录：`metadata/lims-sample-actions-acceptance-20260803.json`

多语言问题的根因是旧 GreenDill 页面请求模块级
`/greenDill/static/LIMSSample/.../i18n-value.js`，此前被 Nginx 通用兼容文件
接管。现在由 LIMSSample 原包属性文件生成 1,263 条模块资源，并通过模块专用
路由返回，不再把按钮或弹窗标题显示成 key。

## 部署与回滚

- 父视图/按钮迁移前备份：`/home/v6/adp-backups/lims-sample-actions-20260803-160729/`
- 子弹窗视图迁移前备份：`/home/v6/adp-backups/lims-sample-dialogs-20260803-163959/`
- Nginx 与静态资源备份：`/home/v6/adp-backups/lims-sample-static-20260803-170939/`
- `nginx -t`：PASS
- Nginx reload：PASS
- `adp-mes-newbase-baseService-1`：迁移后已重启并保持运行

迁移均为确定性、幂等更新；回滚时按上述目录恢复对应数据库导出和 Nginx
配置/静态文件。本轮只做按钮显示和安全点击验收，按钮触发的登记、取样、制样、
处置等真实写入仍应在准备业务前置数据后，按 marker 单独完成 PostgreSQL 落库
验收。
