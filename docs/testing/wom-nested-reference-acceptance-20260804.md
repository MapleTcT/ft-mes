# WOM 备料与配料参照页恢复验收

## 验收结论

- 验收日期：2026-08-04
- 测试环境：`http://10.11.100.17:18080`
- 账号：`admin`（报告不保存密码或 token）
- 数据库：PostgreSQL
- 代码基线：`5cc67dc7827d5b6070c2a1a2eba8e4890799e7d1` 加本轮工作树
- 结论：29/29 个直接参照页通过，28/28 个适用页的“查询”请求通过，1 个详情页不适用查询，真实嵌套弹窗交互通过

本轮针对备料、配料、退料、报工、投料、产出和工序执行页中的
“参照”按钮。修复前，子弹窗可能只显示空白内容，标题和按钮会显示
`WOM.viewtitle.*` 或 `Button.text.*` 原始键。

## 根因与修复

| 根因 | 修复 | 资产 |
|---|---|---|
| 29 个业务参照视图的运行时 JSON 不完整 | 从已检出模块 `module.xml` 确定性恢复，连同依赖共生成 33 个视图 | `263-wom-nested-reference-views-runtime.sql` |
| 旧 WOM 页面自带的 `i18n-value.js` 存在但资源不完整 | 对 WOM 动态 HTML 追加公共兼容资源，不覆盖模块已有翻译 | `adp.conf` 和 `compat/i18n-value.js` |
| 旧视图查询仍指向已退役的备料实体、旧产出字段和非空父条件 | 以 PostgreSQL 兼容视图、字段映射和可选客户条件恢复只读查询 | `264-wom-nested-reference-query-compat.sql` |

本轮没有将 Oracle 改回默认路径，也没有使用 mock 替代真实参照接口。

## 页面覆盖

| 范围 | 页面数 | 实际结果 |
|---|---:|---|
| 仓库、货位、工厂架构 | 3 | PASS |
| 备料需求、备料指令和备料记录 | 6 | PASS |
| 配料指令、配料记录和配料退料 | 6 | PASS |
| 报工、投料、产出和完工入库参照 | 5 | PASS |
| 指令、工序和活动执行参照 | 8 | PASS |
| 尾料参照 | 1 | PASS |

`batchMatOrderView` 是配料指令详情/编辑视图，原产品不包含列表查询按钮，
因此其查询点击标记为 `NOT_APPLICABLE`，页面结构和内容仍已验收。

## 真实嵌套交互

业务入口：
`/msService/WOM/rejectMaterilal/rejectMaterial/batchRejectEdit`

复验步骤：

1. 打开配料退料编辑页。
2. 点击“参照配料记录”。
3. 确认“配料记录参照”标题、筛选区、表格、分页、“选择/取消”可见。
4. 点击“查询”，捕获
   `POST /msService/WOM/batchMaterial/batMaterilPart/recodRefForReject-query`，返回 HTTP 200。
5. 点击“取消”关闭，再次打开并关闭。

该链路连续执行 5 次均通过，最终全量回归再次通过。

## 错误与网络验收

| 检查项 | 实际结果 | 状态 |
|---|---:|---|
| 直接页面 | 29/29 | PASS |
| 适用页查询请求 | 28/28 | PASS |
| 真实嵌套弹窗 | 1/1 | PASS |
| 原始 i18n key | 0 | PASS |
| 可见数据库/系统错误 | 0 | PASS |
| XHR/Fetch 4xx/5xx | 0 | PASS |
| Console error | 0 | PASS |
| Page error | 0 | PASS |
| Request failure | 0 | PASS |

参照页及本轮查询动作均为只读，不应产生业务写入，数据库落库验收为
`NOT_APPLICABLE`。本轮未点击“选择”向退料单增加明细，避免在没有准备专用
marker 业务前置数据时制造污染数据。

## 复验入口

```bash
ADP_PASSWORD='<test-password>' \
  node deploy/docker/scripts/adp-wom-nested-reference-view-acceptance.js
```

机器可读记录：
`metadata/wom-nested-reference-acceptance-20260804.json`。
