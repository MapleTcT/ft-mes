# RM 配方管理功能验收

- 验收时间：2026-07-31 14:47-15:31（Asia/Shanghai）
- 测试环境：`http://10.11.100.17:18080`
- 测试账号：`admin`
- 数据库：PostgreSQL
- 代码基线：`14e9d31a40fd0d399ccab2c86aefaa913176d0ae`
- 机器记录：`metadata/rm-formula-management-acceptance-20260731.json`

## 结论

配方管理 9 个可导航入口已恢复 42 个可见操作按钮。配方类型和工序类型完成真实
新增、修改、删除及 PostgreSQL 回查；普通配方完成真实启用和查库；适用产线、检验
部门、产品 BOM 新增表单、Web 编辑、模板下载、导入入口及导出均通过真实页面交互。

本轮最终回归没有新增的 HTTP 4xx/5xx、console error 或空白页。外部 Batch/DCS
现场投递仍是生产切换前置项，不属于本轮页面和 PostgreSQL 功能阻断。

## 页面与操作矩阵

| 模块 | 页面/路由 | 可见操作 | 实际操作 | API / 响应 | 落库 | 状态 |
|---|---|---|---|---|---|---|
| 配方类型 | `/msService/RM/formulaType/formulaType/formualTypeTreeList` | 新增、修改、删除 | marker 新增、改名、删除 | `formualTypeEdit/save`、`formulaType/delete`，均 HTTP 200 | `rm_formula_types`、`rm_formula_types_mc` | PASS |
| 工序类型 | `/msService/RM/processType/processType/processTypeList` | 新增、修改、删除 | marker 新增、改名、删除 | `processTypeEdit/save`、`processType/delete`，均 HTTP 200 | `rm_process_types`、`rm_process_types_mc` | PASS |
| 产品 BOM | `/msService/RM/formulaBOM/formulaBomMain/formulaBomList` | 新增、修改、删除、复制、启用、停用 | 点击新增，完整表单及物料明细“参照/删行”出现 | 编辑页及运行时布局 HTTP 200 | 本轮未保存，NOT_APPLICABLE | PASS |
| 标准配方 | `/msService/RM/formula/formula/commonFormulaList` | 复制、启用、停用、设置默认、设置检验部门、适用产线 | 工具栏、未选行校验和中文提示回归 | 页面和布局请求 HTTP 200 | NOT_APPLICABLE | PASS |
| 普通配方 | `/msService/RM/formula/formula/easyFormulaList` | 复制、启用、停用、设置默认、设置检验部门、适用产线 | 查询 78 条；启用测试配方；打开适用产线和检验部门 | `POST updateFomulaState` HTTP 200；两个配置页 HTTP 200 | `rm_formulas`、`rm_formula_qualities` | PASS |
| 图形化配方 | `/msService/RM/formula/formula/craftFormulaList` | 复制、启用、停用、设置默认、设置检验部门、适用产线 | 工具栏和未选行校验 | 页面和布局请求 HTTP 200 | NOT_APPLICABLE | PASS |
| 配方组态 004 | `/msService/RM/formula/formula/batchFormulaList?system=serviceUrl/004` | Web编辑、下载模板、导入Excel、导出 | 完整工具栏和 247 条列表数据 | 页面、查询均 HTTP 200 | NOT_APPLICABLE | PASS |
| 配方组态 003 | `/msService/RM/formula/formula/batchFormulaList?system=serviceUrl/003` | Web编辑、下载模板、导入Excel、导出 | 完整工具栏和列表数据 | 页面、查询均 HTTP 200 | NOT_APPLICABLE | PASS |
| 配方组态 001 | `/msService/RM/formula/formula/batchFormulaList?system=serviceUrl/001` | Web编辑、下载模板、导入Excel、导出 | Web 编辑读取配方、工序和活动；下载、文件选择、导出 | 编辑器和 API HTTP 200；导出 XLSX 13,027 bytes | 本轮只读，NOT_APPLICABLE | PASS |

## 写操作落库

### 配方类型

Marker：`ADP_E2E_20260731_1500_RM_FORMULA_TYPE`

```sql
SELECT id,version,valid,code,name,parent_id,lay_rec
FROM public.rm_formula_types
WHERE code='ADP_E2E_20260731_1500_RM_FORMULA_TYPE';

SELECT count(*)
FROM public.rm_formula_types_mc
WHERE formula_type=771709741159680;
```

实际结果：`id=771709741159680`，新增后 version 1，修改后名称带 `_UPDATED`，
删除后 `version=3/valid=false`；关联助记码已随删除清理为 0。默认分类根节点
`id=1000` 保持有效。

### 工序类型

Marker：`ADP_E2E_20260731_1447_RM_PROCESS_TYPE`

```sql
SELECT id,version,valid,code,name
FROM public.rm_process_types
WHERE code='ADP_E2E_20260731_1447_RM_PROCESS_TYPE';

SELECT count(*)
FROM public.rm_process_types_mc
WHERE process_type=771706755638528;
```

实际结果：`id=771706755638528`，修改后名称带 `_UPDATED`，删除后
`version=2/valid=false`；关联助记码为 0。

### 普通配方启用

目标记录：`rm_formulas.id=9007187463893766`

```sql
SELECT id,version,valid,state,status,formual_code
FROM public.rm_formulas
WHERE id=9007187463893766;

SELECT id,remark,convert_from(lo_get(remark),'UTF8') AS remark_text
FROM public.rm_formula_qualities
WHERE formula_id=9007187463893766;
```

实际结果：`state=RM_state/enabled`、`version=1`、`valid=true`、`status=99`。
质量备注已按 Hibernate `@Lob` 迁移为有效 PostgreSQL large-object OID
`292131`，读取内容为 `ADP_E2E_20260727_MES_FULL_CLOSED_10`。

## Web 编辑与安全边界

真实用户从配方组态点击“Web编辑”时，浏览器文档导航不会携带保存在 localStorage
中的 Authorization。修复前因此由 Nginx 返回 401；旧自动验收使用全局请求头，
遮住了这一真实用户路径。

修复后：

- `/msService/RM/formula/editor` 文档空壳不含业务数据，普通导航返回 200。
- 页面从现有登录存储读取票据，仅给同源业务 API 增加 Authorization。
- 未认证 `/msService/RM/formula-editor/formulas` 仍返回 401。
- 登录后同一 API 返回 200，页面读取 200 条配方。
- 配方 `ADP_E2E_20260714051133_RM_WEB_FORMULA` 显示版本 2、工序 1、活动 1。
- 导出请求由 401 恢复为 200，文件为
  `RM_batchFormulaList.xlsx`，13,027 bytes。

## 修复资产

- `deploy/docker/postgres/init/241-rm-formula-menu-actions-runtime.sql`
- `deploy/docker/postgres/init/242-rm-type-mne-code-tables.sql`
- `deploy/docker/postgres/init/243-rm-formula-type-root.sql`
- `deploy/docker/postgres/init/244-rm-formula-type-tree-lob-compat.sql`
- `deploy/docker/postgres/init/245-rm-formula-quality-remark-lob-compat.sql`
- `deploy/docker/scripts/patch-rm-batch-formula-list.py`
- `deploy/docker/assets/module-static/RM/formula/editor.html`
- `deploy/docker/assets/module-static/compat/i18n-value.js`
- `deploy/docker/nginx/adp.conf`

页面证据：

- `metadata/process-type-add.png`
- `metadata/formula-type-added.png`
- `metadata/bom-add-form.png`
- `metadata/normal-formula-actions-and-data.png`
- `metadata/normal-formula-suitline.png`
- `metadata/normal-formula-quality-department.png`
- `metadata/batch-formula-actions-final.png`
- `metadata/batch-formula-web-editor-viewport.png`
