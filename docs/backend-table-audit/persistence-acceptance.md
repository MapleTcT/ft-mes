# 后端落库验收报告

## 当前状态

本文件是后端“业务动作是否真实落库”的验收入口。当前已记录组织部门 CRUD 的第一轮 PostgreSQL 落库验收，并记录 WOM 制造任务创建入口阻断；未列入动作仍未完成验收。

落库验收必须来自真实前端操作或等效 E2E 操作，并且必须直接查询 PostgreSQL 证明数据变化。

## 总览

| 指标 | 数量 |
| --- | ---: |
| 验收动作 | 5 |
| PASS | 3 |
| FAIL | 1 |
| BLOCKED | 1 |
| NOT_APPLICABLE | 0 |

## 落库验收明细

| 业务动作 | 前端入口 | API endpoint | 后端入口 | 目标表 | 验收 SQL | 实际结果 | 状态 |
|---|---|---|---|---|---|---|---|
| 新增部门 | `/organization/#/organizationmanage` 新增部门弹窗 | `POST /inter-api/organization/v1/department` | `DepartmentInterController.addDepartment -> DepartmentService.addDepartment -> addDepartmentWithoutKafka -> MyBatis Plus save` | `org_department` | `select id, code, name, dept_type, coalesce(description,''), company_id, coalesce(parent_id::text,''), valid, leaf, coalesce(full_path,''), coalesce(lay_rec,'') from public.org_department where code = 'ADP_E2E_20260615123939_ORGDEP_DEP' order by create_time desc nulls last, id desc;` | 返回 `200`；id `6587704459641360`；新增后 `valid=1`，名称 `ADP_E2E_20260615123939_ORGDEP`，描述为 create marker | PASS |
| 编辑部门 | 已登录组织页面上下文同源请求 | `PUT /inter-api/organization/v1/department` | `DepartmentInterController.updateDepartment -> DepartmentService.updateDepartment -> updateDepartmentWithoutKafka -> MyBatis Plus updateById` | `org_department` | 同上，按 code 查询同一 id | 返回 `200`；同一 id 名称变为 `ADP_E2E_20260615123939_ORGDEP_EDIT`，描述变为 update marker，`valid=1` | PASS |
| 删除部门 | 已登录组织页面上下文同源请求 | `DELETE /inter-api/organization/v1/department/6587704459641360` | `DepartmentInterController.deleteDep -> DepartmentService.deleteDepById -> updateBatchById` | `org_department` | 同上，按 code 查询同一 id | 返回 `200`；同一 id 保留但 `valid=0`，证明为软删除 | PASS |
| 新建生产工单或制造任务 | `/msService/WOM/produceTask/produceTask/makeTaskList` | 未发现创建接口；仅捕获 `POST /msService/WOM/produceTask/produceTask/makeTaskList-pending` 列表查询 | 未进入后端写链路；运行时视图 `buttons` 为空 | 待确认写表，读模型为 `WOM_PRODUCE_TASKS` / `wfm_task_pending` | 未执行。当前页面无新增入口，不能生成 marker 写动作 | 真实浏览器页面 `200`，无错误；可见按钮只有“查询 / 仅查待办 / 清空”；兼容运行时视图按钮为空；无法执行落库验收 | BLOCKED |
| 生产状态流转、报工、活动执行 | WOM `makeTaskList`、`prepareMakeTaskList`、`makeTaskView`、`makeTaskBatchView`、`easyTaskOperateView` | 源码定位到 `updateTaskState`、`addOutputByOutPutDetails`、`generatePrepareNeed`、`startActive/endActive`、`endEasyActive`；当前页面按钮缺失，动作页 `layoutJson` 500 已修复为 200，但前端仍 React #130 崩溃 | `WOMProduceTaskController -> WOMProduceTaskServiceImpl` 对应方法已在 WOM 6.1.3.4 源码定位；`/baseService/view/layoutJson` 后端链路已不再抛 500 | `WOM_PRODUCE_TASKS`、`WOM_TASK_PROCESSES`、`WOM_TASK_ACTIVES`、`WOM_PROC_REPORTS`、`WOM_TASK_MATERIALS` | 未执行。动作视图仍无法渲染出按钮或表单，不能从真实前端提交 marker 写动作 | runtime JSON 补丁后 `makeTaskEdit/Submit/View/Batch/EasyOperate` 的 `layoutJson` 返回 200，pageType 为 `EDIT/VIEW`；真实浏览器仍 React #130；`makeTaskGraphList` 404；当前只能记录源链路，不能认定落库成功 | FAIL |

## 本轮请求和落库摘要

- Marker：`ADP_E2E_20260615123939_ORGDEP`
- 新增 payload：`{"name":"ADP_E2E_20260615123939_ORGDEP","code":"ADP_E2E_20260615123939_ORGDEP_DEP","type":"sys_department_type/general","managers":[],"description":"ADP_E2E_20260615123939_ORGDEP create via organization UI","companyId":1000}`
- 编辑 payload：`{"id":6587704459641360,"name":"ADP_E2E_20260615123939_ORGDEP_EDIT","type":"sys_department_type/general","description":"ADP_E2E_20260615123939_ORGDEP update via browser-context API","managerIds":[]}`
- 删除 endpoint：`DELETE /inter-api/organization/v1/department/6587704459641360`
- 最终 PostgreSQL 行：`6587704459641360|ADP_E2E_20260615123939_ORGDEP_DEP|ADP_E2E_20260615123939_ORGDEP_EDIT|sys_department_type/general|ADP_E2E_20260615123939_ORGDEP update via browser-context API|1000||0|1|/默认公司/ADP_E2E_20260615123939_ORGDEP_EDIT|6587704459641360`
- 原始脚本报告：`/tmp/adp-organization-persistence-final-20260615203939.json`
- WOM 制造任务动作发现：`/tmp/adp-production-action-discovery-final8-20260615204438/production-action-discovery.json`
- WOM 生产动作候选页探测：`/tmp/adp-production-action-discovery-candidates-20260615193213/production-action-discovery.json`
- WOM runtime JSON 修复后复验：`/tmp/adp-production-action-discovery-final8-20260615204438/production-action-discovery.json`
- WOM layoutJson 直连复验：5 个动作 viewCode 均 `200`，pageType 为 `EDIT/VIEW`
- WOM 生产动作源地图：`metadata/production-module-source-action-map.json`

## 证据要求

- 每个写操作必须带唯一 marker，例如 `ADP_E2E_YYYYMMDD_HHMMSS_xxx`。
- 必须记录 request payload、response status 和 response body 关键字段。
- 必须追踪 controller / service / mapper / SQL / 目标表。
- 必须记录 PostgreSQL 查询语句和结果摘要。
- 更新、删除、禁用、启用、状态变更类操作必须证明字段变化。
- 接口成功但查库无变化时标记 `FAIL`。
- 不落库功能必须说明原因并标记 `NOT_APPLICABLE`。
