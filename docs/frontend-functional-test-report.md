# 前端功能测试报告

## 当前状态

本文件是功能验收记录入口。当前已记录一轮平台基础 smoke、组织部门 CRUD 落库验收、组织组管理 CRUD 落库验收、组织岗位 CRUD 落库验收、组织公司 CRUD 落库验收、组织人员 CRUD 落库验收、组织人员创建账号落库验收、独立用户管理账号新增/编辑/锁定/解锁/删除落库验收、RBAC 角色/角色用户/角色权限/用户权限/数据资源权限落库验收、基础配置-系统编码字典项/字典值 CRUD 落库验收、业务模块 53 个入口的 API/layout 与真实浏览器页面 smoke，以及 WOM 制造任务创建入口发现。未列入的页面、生产模块动作级功能和写操作落库仍未完成验收。

执行功能验收时，必须按照 [功能验收与落库验收规则](functional-persistence-acceptance.md) 更新本文件，并同步更新 `metadata/persistence-acceptance.json`。

## 环境

| 项 | 值 |
| --- | --- |
| 前端入口 | `http://10.11.100.17:18080` |
| 默认数据库 | PostgreSQL |
| Oracle 状态 | legacy-template-only / oracle-legacy only |
| 测试方式 | 真实浏览器或等效 E2E |

## 总览

| 指标 | 数量 |
| --- | ---: |
| 被测功能 | 46 |
| PASS | 44 |
| FAIL | 1 |
| BLOCKED | 1 |
| NOT_APPLICABLE | 0 |

## 功能验收明细

| 模块 | 页面/路由 | 操作 | API | 前端结果 | 后端结果 | 数据库表 | 验收状态 | 问题 |
|---|---|---|---|---|---|---|---|---|
| 平台基础 | `http://10.11.100.17:18080` | 登录、平台 API、首页待办、组织只读、RBAC 权限、菜单页面 smoke | `/inter-api/auth/login`、`/inter-api/rbac/v1/menus/currentUser`、组织/待办/RBAC 相关接口 | Playwright 真实浏览器 smoke `6/6` passed；首页待办无 visible error、network error、console error；菜单抽样 `12/12` passed | platform API `16/16` passed；RBAC authority `9/9` passed；组织只读接口通过 | 不适用 | PASS | 证据：`/tmp/adp-platform-validation-acceptance-20260615182929/platform-validation-summary.json` |
| 基础配置-系统编码 | `/systemcode/#/` | 新增系统编码字典项 `systemCode_ADP_E2E_20260615161445_SYSCODE_ENTITY` | `POST /inter-api/systemcode/v1/entity` | 真实浏览器打开系统编码页面；navigation status `200`；无 visible error、console error、page error、systemcode request failure | 返回 `200`；PostgreSQL `sys_entity` 新增 active 行，`code/name/display_name/type/module_id/cid/memo` 均为 marker 值 | `sys_entity` | PASS | 证据：`/tmp/adp-systemcode-persistence-second.json` |
| 基础配置-系统编码 | `/systemcode/#/` | 编辑系统编码字典项名称、显示名和备注 | `PUT /inter-api/systemcode/v1/entity` | 同一浏览器页面上下文发起同源请求；无 visible error、console error、page error、systemcode request failure | 返回 `200`；PostgreSQL 同一 `sys_entity` id `6588127318163920` 更新为 update marker，`row_version=1`，`valid=1` | `sys_entity` | PASS | 无 |
| 基础配置-系统编码 | `/systemcode/#/` | 新增系统编码值 `ADP_E2E_20260615161445_SYSCODE_VALUE` | `POST /inter-api/systemcode/v1/value` | 同一浏览器页面上下文发起同源请求；无 visible error、console error、page error、systemcode request failure | 返回 `200`；PostgreSQL `sys_code` 新增 active 行，`entity_code/code/cid/default_flag/lay_no/full_path/memo/desA/desB/desC` 均写入 | `sys_code` | PASS | 无 |
| 基础配置-系统编码 | `/systemcode/#/` | 编辑系统编码值名称、显示名、默认值和描述字段 | `PUT /inter-api/systemcode/v1/value` | 同一浏览器页面上下文发起同源请求；无 visible error、console error、page error、systemcode request failure | 返回 `200`；PostgreSQL 同一 `sys_code` id `6588127405097424` 更新为 update marker，`row_version=1`、`default_flag=0` | `sys_code` | PASS | 无 |
| 基础配置-系统编码 | `/systemcode/#/` | 删除系统编码值 | `DELETE /inter-api/systemcode/v1/systemCode_ADP_E2E_20260615161445_SYSCODE_ENTITY/values/ADP_E2E_20260615161445_SYSCODE_VALUE` | 同一浏览器页面上下文发起同源请求；无 visible error、console error、page error、systemcode request failure | 返回 `200`；PostgreSQL 同一 `sys_code` 保留且 `valid=0`，证明软删除 | `sys_code` | PASS | 软删后 `display_name` 被服务对象回写为 i18n key；因记录已失效，不阻断当前落库验收 |
| 基础配置-系统编码 | `/systemcode/#/` | 删除系统编码字典项并联动字典值 | `DELETE /inter-api/systemcode/v1/entities/systemCode_ADP_E2E_20260615161445_SYSCODE_ENTITY` | 同一浏览器页面上下文发起同源请求；无 visible error、console error、page error、systemcode request failure | 返回 `200`；PostgreSQL 同一 `sys_entity.valid=0`，子 `sys_code.valid=0`，证明字典项软删除与子值清理 | `sys_entity`、`sys_code` | PASS | 无 |
| 业务模块页面 | `53` 个 `/msService/...` 入口 | 登录后逐页打开 BaseSet、LIMS/QCS、WOM、RM、craftGraph、EAM、Measure、WTS、workAppointment 等业务模块页面 | `GET /msService/...` 页面与布局入口；详见 `deploy/docker/scripts/adp-business-module-smoke.js`、`deploy/docker/scripts/adp-business-page-smoke.js` | API/layout smoke `53/53` passed；Playwright 真实浏览器页面 smoke 复验 `53/53` passed；无 visible error、network error、console error、page error、request failure | 当前只验证页面/API 可达和渲染无错误，不代表新增、编辑、删除、状态流转等业务写操作已落库 | 不适用 | PASS | 证据：`/tmp/adp-business-module-smoke-20260615184508.json`、`/tmp/adp-business-page-smoke-final-20260615204003/business-page-smoke-results.json`；动作级落库仍未验收 |
| 生产模块 | `/msService/WOM/produceTask/produceTask/makeTaskList` | 真实浏览器打开制造任务列表，定位新建生产工单/制造任务入口 | `POST /msService/WOM/produceTask/produceTask/makeTaskList-pending` | 页面 `200`，无 visible error、console error、page error、request failure；可见按钮只有“查询 / 仅查待办 / 清空”，未发现安全的新增/新建/添加/创建按钮；运行时视图 `layoutDatagrid.buttons` 为 `[]` | 只捕获到列表查询接口，读模型识别到 `WOM_PRODUCE_TASKS` 和 `wfm_task_pending`，但未发现创建接口；无法执行 marker 写动作和 PostgreSQL 落库验收 | 待确认写表，读模型为 `WOM_PRODUCE_TASKS` / `wfm_task_pending` | BLOCKED | 证据：`/tmp/adp-production-action-discovery-final8-20260615204438/production-action-discovery.json`；截图：`/tmp/adp-production-action-discovery-final8-20260615204438/01-PROD-002--msService-WOM-produceTask-produceTask-makeTaskList-initial.png` |
| 生产模块动作视图 | WOM `makeTaskEdit`、`makeTaskSubmitView`、`makeTaskView`、`makeTaskBatchView`、`easyTaskOperateView`、`makeTaskGraphList` | 真实浏览器逐个打开源包中出现的生产动作候选页面，只观察不点击新增/保存 | `GET /msService/baseService/view/layoutJson?viewCode=...`；`GET /msService/WOM/produceTask/produceTask/makeTaskGraphList` | runtime JSON 补丁后 `makeTaskEdit/Submit/View/Batch/EasyOperate` 的 `layoutJson` 已返回 200，且包含 `DataGridCode` 和真实 `produceTask` 字段 key；但真实浏览器复验仍显示错误页，console 为 React error #130，页面无按钮、无表单字段、无写请求；`makeTaskGraphList` 仍返回 404 | 源包已定位状态流转、报工、活动开始/结束、备料生成等写接口，但当前动作页面前端渲染失败或不可达，不能进入 marker 落库验收 | `WOM_PRODUCE_TASKS`、`WOM_TASK_PROCESSES`、`WOM_TASK_ACTIVES`、`WOM_PROC_REPORTS` 等待动作恢复后复验 | FAIL | 证据：`/tmp/adp-production-action-discovery-final8-20260615204438/production-action-discovery.json`；layoutJson 直连复验：5 个动作 viewCode 均 `200`，pageType 为 `EDIT/VIEW`；动作地图：`metadata/production-module-source-action-map.json` |
| 组织管理 | `/organization/#/organizationmanage` | 选择默认公司，打开新增部门弹窗，新增部门 `ADP_E2E_20260615123939_ORGDEP` | `POST /inter-api/organization/v1/department` | 真实浏览器打开组织管理页面并提交新增弹窗；navigation status `200`；无 console error、page error、request failure、visible error | 返回 `200`，响应包含新部门 id `6587704459641360`；PostgreSQL 查询返回新增数据 | `org_department` | PASS | 无 |
| 组织管理 | `/organization/#/organizationmanage` | 在已登录组织页面上下文编辑部门名称和描述 | `PUT /inter-api/organization/v1/department` | 同一浏览器页面上下文发起同源请求；无 console error、page error、request failure、visible error | 返回 `200`；PostgreSQL 同一 id 的 `name` 和 `description` 更新为 marker EDIT 值 | `org_department` | PASS | 无 |
| 组织管理 | `/organization/#/organizationmanage` | 在已登录组织页面上下文删除部门 | `DELETE /inter-api/organization/v1/department/6587704459641360` | 同一浏览器页面上下文发起同源请求；无 console error、page error、request failure、visible error | 返回 `200`；PostgreSQL 同一 id 软删除，`valid=0` | `org_department` | PASS | 无 |
| 组织管理-组管理 | `/organization/#/organizationmanage` | 在已登录组织页面上下文新增组 `ADP_E2E_20260615133645_GRP` | `POST /inter-api/organization/v1/group` | 真实浏览器打开组织管理页面；navigation status `200`；同源前端会话发起新增请求；无 console error、page error、request failure、visible error | 返回 `200`；PostgreSQL 查询 `org_group` 返回 id `6587816676082192`，`valid=1`，名称和描述为 marker create 值 | `org_group` | PASS | 无 |
| 组织管理-组管理 | `/organization/#/organizationmanage` | 在已登录组织页面上下文编辑组名称和描述 | `PUT /inter-api/organization/v1/group` | 同一浏览器页面上下文发起同源请求；无 console error、page error、request failure、visible error | 返回 `200`；PostgreSQL 同一 id 的 `name` 更新为 `ADP_E2E_20260615133645_GRP_EDIT`，`description` 更新为 marker update 值 | `org_group` | PASS | 无 |
| 组织管理-组管理 | `/organization/#/organizationmanage` | 在已登录组织页面上下文删除组 | `DELETE /inter-api/organization/v1/group/6587816676082192` | 同一浏览器页面上下文发起同源请求；无 console error、page error、request failure、visible error | 返回 `200`；PostgreSQL 按 code 查询无剩余行，确认为物理删除 | `org_group` | PASS | 无 |
| 组织管理-岗位管理 | `/organization/#/organizationmanage` | 在已登录组织页面上下文新增岗位 `ADP_E2E_20260615134928_POS` | `POST /inter-api/organization/v1/position` | 真实浏览器打开组织管理页面；navigation status `200`；同源前端会话发起新增请求；无 console error、page error、request failure、visible error | 返回 `200`，响应包含岗位 id `6587841746223632`；PostgreSQL 查询 `org_position` 返回 `valid=1`、`dep_id=1`、名称/描述/full_path/lay_rec 已写入；`org_position_mnecode` 已生成助记码 | `org_position`、`org_position_mnecode` | PASS | 无 |
| 组织管理-岗位管理 | `/organization/#/organizationmanage` | 在已登录组织页面上下文编辑岗位名称和描述 | `PUT /inter-api/organization/v1/position` | 同一浏览器页面上下文发起同源请求；无 console error、page error、request failure、visible error | 返回 `200`；PostgreSQL 同一 id 的 `name` 更新为 `ADP_E2E_20260615134928_POS_EDIT`，`description` 和 `full_path` 更新为 marker update 值；`org_position_mnecode` 已刷新 | `org_position`、`org_position_mnecode` | PASS | 无 |
| 组织管理-岗位管理 | `/organization/#/organizationmanage` | 在已登录组织页面上下文删除岗位 | `DELETE /inter-api/organization/v1/position/6587841746223632` | 同一浏览器页面上下文发起同源请求；无 console error、page error、request failure、visible error | 返回 `200`；PostgreSQL 同一 id 保留且 `valid=0`，`org_position_mnecode` 查询为空，删除模式为软删除 | `org_position`、`org_position_mnecode` | PASS | 无 |
| 组织管理-公司管理 | `/organization/#/organizationmanage` | 在已登录组织页面上下文新增公司 `ADP_E2E_20260615141009_COM` | `POST /inter-api/organization/v1/company` | 真实浏览器打开组织管理页面；navigation status `200`；同源前端会话发起新增请求；无 console error、page error、request failure、visible error | 返回 `200`，响应包含公司 id `6587882338992656`；PostgreSQL `org_company` 新增 `valid=1`；`org_company_mnecode` 生成；虚拟部门/岗位/人员和公司管理员 `auth_user` 均写入 | `org_company`、`org_company_mnecode`、`org_department`、`org_position`、`org_person`、`auth_user` | PASS | 无 |
| 组织管理-公司管理 | `/organization/#/organizationmanage` | 在已登录组织页面上下文编辑公司简称、全称和描述 | `PUT /inter-api/organization/v1/company` | 同一浏览器页面上下文发起同源请求；无 console error、page error、request failure、visible error | 返回 `200`；PostgreSQL 同一 id 的 `short_name` 更新为 `ADP_E2E_20260615141009_COM_EDIT`，`full_name` 更新为 `ADP_E2E_20260615141009_COM_FULL_EDIT`，`full_path` 同步更新；`org_company_mnecode` 刷新为编辑后简称 | `org_company`、`org_company_mnecode` | PASS | 无 |
| 组织管理-公司管理 | `/organization/#/organizationmanage` | 在已登录组织页面上下文删除公司 | `DELETE /inter-api/organization/v1/company/6587882338992656` | 同一浏览器页面上下文发起同源请求；无 console error、page error、request failure、visible error | 返回 `200`；PostgreSQL 同一公司保留且 `valid=0`；`org_company_mnecode` 查询为空；虚拟部门/岗位/人员和 `auth_user` 均无 active 行，`auth_user` 删除模式为软删除 | `org_company`、`org_company_mnecode`、`org_department`、`org_position`、`org_person`、`auth_user` | PASS | 无 |
| 组织管理-人员管理 | `/organization/#/organizationmanage` | 在已登录组织页面上下文新增人员 `ADP_E2E_20260615142411_PER`，`createUser=false` | `POST /inter-api/organization/v1/person` | 真实浏览器打开组织管理页面；navigation status `200`；同源前端会话发起新增请求；无 console error、page error、request failure、visible error | 返回 `200`；PostgreSQL `org_person` 新增 id `6587910008717840`，`valid=1`，姓名/性别/手机号/邮箱/描述均为 marker 值；`org_person_position`、`org_person_department`、`org_person_company` 写入 active 关系；`org_person_mnecode` 生成；未创建 `auth_user` 符合 `createUser=false` | `org_person`、`org_person_position`、`org_person_department`、`org_person_company`、`org_person_mnecode` | PASS | 无 |
| 组织管理-人员管理 | `/organization/#/organizationmanage` | 在已登录组织页面上下文编辑人员姓名、性别、手机号、邮箱和描述 | `PUT /inter-api/organization/v1/person` | 同一浏览器页面上下文发起同源请求；无 console error、page error、request failure、visible error | 返回 `200`；PostgreSQL 同一人员变为 `ADP_E2E_20260615142411_PER_EDIT`，`gender=sys_gender/female`，手机号/邮箱/描述更新，`valid=1`；`org_person_mnecode` 刷新为编辑后名称 | `org_person`、`org_person_mnecode` | PASS | 无 |
| 组织管理-人员管理 | `/organization/#/organizationmanage` | 在已登录组织页面上下文删除人员 | `DELETE /inter-api/organization/v1/person/6587910008717840` | 同一浏览器页面上下文发起同源请求；无 console error、page error、request failure、visible error | 返回 `200`；PostgreSQL 同一 `org_person` 保留且 `valid=0`；岗位/部门/公司人员关系均 `valid=0`；`org_person_mnecode` 查询为空；人员 user 绑定为空 | `org_person`、`org_person_position`、`org_person_department`、`org_person_company`、`org_person_mnecode` | PASS | 无 |
| 组织管理-人员创建账号 | `/organization/#/organizationmanage` | 在已登录组织页面上下文新增人员 `ADP_E2E_20260615144220_PUSR`，`createUser=true` | `POST /inter-api/organization/v1/person` | 真实浏览器打开组织管理页面；navigation status `200`；同源前端会话发起新增请求；无 console error、page error、request failure、visible error；请求中密码已在报告中脱敏 | 返回 `200`；PostgreSQL `org_person` 新增 id `6587945615016464` 并绑定 `user_id=6587945615639056`、`user_name=adp_e2e_20260615144220_person_user`；`auth_user` 新增 active 行，`person_id/person_code/person_name/company_id/user_type/description` 均匹配；密码字段为 32 位非明文编码值；岗位/部门/公司人员关系均 active | `org_person`、`org_person_position`、`org_person_department`、`org_person_company`、`auth_user`、`auth_user_role` | PASS | 无 |
| 组织管理-人员创建账号 | `/organization/#/organizationmanage` | 在已登录组织页面上下文编辑人员姓名、性别、手机号、邮箱和描述，并验证账号人员名同步 | `PUT /inter-api/organization/v1/person` | 同一浏览器页面上下文发起同源请求；无 console error、page error、request failure、visible error | 返回 `200`；PostgreSQL 同一人员变为 `ADP_E2E_20260615144220_PUSR_EDIT`；`auth_user.person_name` 同步更新为编辑后的人员姓名，账号绑定保持不变 | `org_person`、`auth_user` | PASS | 无 |
| 组织管理-人员创建账号 | `/organization/#/organizationmanage` | 在已登录组织页面上下文删除带账号人员 | `DELETE /inter-api/organization/v1/person/6587945615016464` | 同一浏览器页面上下文发起同源请求；无 console error、page error、request failure、visible error | 返回 `200`；PostgreSQL `org_person.valid=0` 且 `user_id/user_name` 清空；岗位/部门/公司人员关系均 `valid=0`；`auth_user.valid=0`，证明人员删除联动软删账号 | `org_person`、`org_person_position`、`org_person_department`、`org_person_company`、`auth_user`、`auth_user_role` | PASS | 无 |
| 用户管理 | `/auth/#/user` | 在已登录用户管理页面上下文新增账号 `ADP_E2E_20260615150418_AUSR` | `POST /inter-api/auth/v1/user` | 真实浏览器打开用户管理页面；navigation status `200`；无 console error、page error、request failure、visible error；请求中密码已脱敏 | 返回 `200`，响应包含用户 id `6587988858225168`；PostgreSQL `auth_user` 新增 active 行，绑定前置人员 id `6587988816478736`；`org_person.user_id/user_name` 同步写入；密码为 32 位非明文编码值；未传角色时 `auth_user_role` 计数为 `0` | `auth_user`、`org_person`、`auth_user_role` | PASS | 无 |
| 用户管理 | `/auth/#/user` | 在已登录用户管理页面上下文编辑账号描述和时区 | `PUT /inter-api/auth/v1/user` | 同一浏览器页面上下文发起同源请求；无 console error、page error、request failure、visible error | 返回 `200`；PostgreSQL 同一 `auth_user.description` 更新为 update marker，`time_zone` 更新为 `JST+09:00`，人员绑定保持不变 | `auth_user`、`auth_user_role` | PASS | 无 |
| 用户管理 | `/auth/#/user` | 在已登录用户管理页面上下文锁定账号 | `PUT /inter-api/auth/v1/user/status` | 同一浏览器页面上下文发起同源请求；无 console error、page error、request failure、visible error | 返回 `200`；PostgreSQL 同一 `auth_user.has_lock=1` | `auth_user` | PASS | 无 |
| 用户管理 | `/auth/#/user` | 在已登录用户管理页面上下文解锁账号 | `PUT /inter-api/auth/v1/user/status` | 同一浏览器页面上下文发起同源请求；无 console error、page error、request failure、visible error | 首次真实验收暴露 PostgreSQL 缺列 `auth_user.error_count`，已通过 `deploy/docker/postgres/init/073-auth-user-lock-status-compat.sql` 补齐并应用；复验返回 `200`，PostgreSQL 同一 `auth_user.has_lock=0` | `auth_user` | PASS | 已修复：`auth_user.error_count` 缺列 |
| 用户管理 | `/auth/#/user` | 在已登录用户管理页面上下文删除账号，并清理前置人员 | `DELETE /inter-api/auth/v1/user` | 同一浏览器页面上下文发起同源请求；无 console error、page error、request failure、visible error | 返回 `200`；PostgreSQL 同一 `auth_user.valid=0`；`org_person.user_id/user_name` 清空；`auth_user_role` 计数为 `0`；随后通过组织人员删除接口清理前置人员，`org_person.valid=0` | `auth_user`、`org_person`、`auth_user_role` | PASS | 无 |
| 权限/RBAC-角色管理 | `/auth/#/role` | 在已登录角色页面上下文新增角色 `ADP_E2E_20260615153240_RBAC_ROLE` | `POST /inter-api/rbac/v1/role` | 真实浏览器打开角色管理页面；navigation status `200`；核心 RBAC API 无 page error、request failure、visible error；仅记录非业务静态资源 `/supide-app/runtime/permissions/ping.png` 404 | 返回 `200`；PostgreSQL `rbac_role` 新增 active 行，`name/description/cid/uuid` 均写入 | `rbac_role`、`rbac_role_mnecode` | PASS | 首轮验收暴露 `rbac_roleuser.valid` 默认 false，已用 `074-rbac-roleuser-valid-default.sql` 修复后复验通过 |
| 权限/RBAC-角色管理 | `/auth/#/role` | 在已登录角色页面上下文编辑角色名称和描述 | `PUT /inter-api/rbac/v1/role` | 同一浏览器页面上下文发起同源请求；核心 RBAC API 无阻断错误 | 返回 `200`；PostgreSQL 同一 `rbac_role.name` 更新为 `ADP_E2E_20260615153240_RBAC_ROLE_NAME_UPDATED`，`description` 更新为 update marker | `rbac_role`、`rbac_role_mnecode` | PASS | 无 |
| 权限/RBAC-角色用户 | `/auth/#/role` | 给 marker 角色绑定 marker 用户 | `POST /inter-api/rbac/v1/roleUser` | 同一浏览器页面上下文发起同源请求；核心 RBAC API 无阻断错误 | 返回 `200`；PostgreSQL `rbac_roleuser` 新增 active 行，`role_id/user_id/user_name/person_code/from_position` 匹配 | `rbac_roleuser` | PASS | 已修复 `rbac_roleuser.valid` 默认 false |
| 权限/RBAC-角色数据资源 | `/auth/#/authority?status=role&id=6588088357011984&name=ADP_E2E_20260615155455_RBAC_ROLE_NAME_UPDATED` | 给 marker 角色保存数据资源权限 `ADP_E2E_20260615155455_RBAC_DATA_RESOURCE_ROLE` | `POST /inter-api/rbac/v1/role/6588088357011984/data/resource/oodm-data-group-permission` | 真实浏览器打开角色权限配置页；同一前端会话提交数据资源权限；非业务静态 `ping.png` 404 已忽略；无 blocking console/page/network error | 返回 `200`；PostgreSQL `rbac_role_data_permission` 新增 active 行，`rbac_role_data_permission_ctrl.controlled=1`；已绑定用户同步生成 `rbac_user_data_permission.purview_type=0` 继承行 | `rbac_role_data_permission`、`rbac_role_data_permission_ctrl`、`rbac_user_data_permission` | PASS | 首次验收发现角色权限已写表但用户继承行未落库；已用 `075-rbac-data-resource-permission-tables.sql` 补表和同步 trigger 后复验通过 |
| 权限/RBAC-角色数据资源 | `/auth/#/authority?status=role&id=6588088357011984&name=ADP_E2E_20260615155455_RBAC_ROLE_NAME_UPDATED` | 关闭 marker 角色数据资源受控 | `POST /inter-api/rbac/v1/role/6588088357011984/data/resource/oodm-data-group-permission` | 同一浏览器页面上下文发起关闭受控请求；核心 RBAC API 无阻断错误 | 返回 `200`；PostgreSQL 同一角色资源行 `valid=false`，继承用户资源行 `valid=false`，`rbac_role_data_permission_ctrl.controlled=0` | `rbac_role_data_permission`、`rbac_role_data_permission_ctrl`、`rbac_user_data_permission` | PASS | 无 |
| 权限/RBAC-角色用户 | `/auth/#/role` | 解绑 marker 角色用户关系 | `DELETE /inter-api/rbac/v1/roleUser/6588044794642960` | 同一浏览器页面上下文发起同源请求；核心 RBAC API 无阻断错误 | 返回 `200`；PostgreSQL 按 `role_id/user_id` 查询无剩余 active/valid 行，关系已删除 | `rbac_roleuser` | PASS | 无 |
| 权限/RBAC-角色权限 | `/auth/#/authority?status=role&id=6588044621169168&name=ADP_E2E_20260615153240_RBAC_ROLE_NAME_UPDATED` | 给 marker 角色分配 `personmanage/addPerson` 菜单操作权限 | `POST /inter-api/rbac/v1/rolePermission` | 真实浏览器打开角色权限配置页；核心 RBAC API 无阻断错误；静态 `ping.png` 404 已记录为非业务资源问题 | 返回 `200`；PostgreSQL `rbac_rolepermission` 新增行，`role_id/menuoperate_id/cid/no_restrict_flag` 匹配 | `rbac_rolepermission` | PASS | 无 |
| 权限/RBAC-角色权限 | `/auth/#/authority?status=role&id=6588044621169168&name=ADP_E2E_20260615153240_RBAC_ROLE_NAME_UPDATED` | 删除 marker 角色的 `personmanage/addPerson` 权限 | `POST /inter-api/rbac/v1/rolePermission` | 同一浏览器页面上下文发起删除权限请求；核心 RBAC API 无阻断错误 | 返回 `200`；PostgreSQL 按 `role_id/menuoperate_id` 查询无剩余行 | `rbac_rolepermission`、`rbac_rolep*` | PASS | 无 |
| 权限/RBAC-用户数据资源 | `/auth/#/authority?status=user&id=6588088482251280&name=adp_e2e_20260615155455_rbac_user` | 给 marker 用户保存直接数据资源权限 `ADP_E2E_20260615155455_RBAC_DATA_RESOURCE_USER` | `POST /inter-api/rbac/v1/user/6588088482251280/data/resource/oodm-data-group-permission` | 真实浏览器打开用户权限配置页；同一前端会话提交数据资源权限；无 blocking console/page/network error | 返回 `200`；PostgreSQL `rbac_user_data_permission` 新增 active 行，`purview_type=1`，`rbac_user_data_permission_ctrl.controlled=1` | `rbac_user_data_permission`、`rbac_user_data_permission_ctrl` | PASS | 已由 `075-rbac-data-resource-permission-tables.sql` 补齐缺表 |
| 权限/RBAC-用户数据资源 | `/auth/#/authority?status=user&id=6588088482251280&name=adp_e2e_20260615155455_rbac_user` | 关闭 marker 用户直接数据资源受控 | `POST /inter-api/rbac/v1/user/6588088482251280/data/resource/oodm-data-group-permission` | 同一浏览器页面上下文发起关闭受控请求；核心 RBAC API 无阻断错误 | 返回 `200`；PostgreSQL 同一用户资源行 `valid=false`，`rbac_user_data_permission_ctrl.controlled=0` | `rbac_user_data_permission`、`rbac_user_data_permission_ctrl` | PASS | 无 |
| 权限/RBAC-用户权限 | `/auth/#/authority?status=user&id=6588044746244624&name=adp_e2e_20260615153240_rbac_user` | 给 marker 用户分配 `personmanage/addPerson` 直接权限 | `POST /inter-api/rbac/v1/userPermission` | 真实浏览器打开用户权限配置页；核心 RBAC API 无阻断错误 | 返回 `200`；PostgreSQL `rbac_userpermission` 新增行，`user_id/menuoperate_id/menuoperate_code/purview_type=1/no_restrict_flag` 匹配 | `rbac_userpermission` | PASS | 无 |
| 权限/RBAC-用户权限 | `/auth/#/authority?status=user&id=6588044746244624&name=adp_e2e_20260615153240_rbac_user` | 删除 marker 用户的 `personmanage/addPerson` 直接权限 | `POST /inter-api/rbac/v1/userPermission` | 同一浏览器页面上下文发起删除权限请求；核心 RBAC API 无阻断错误 | 返回 `200`；PostgreSQL 按 `user_id/menuoperate_id/purview_type=1` 查询无剩余行 | `rbac_userpermission`、`rbac_userp*` | PASS | 无 |
| 权限/RBAC-角色管理 | `/auth/#/role` | 删除 marker 角色并清理前置 marker 用户/人员 | `DELETE /inter-api/rbac/v1/role/ADP_E2E_20260615153240_RBAC_ROLE` | 同一浏览器页面上下文发起清理请求；核心 RBAC API 无阻断错误 | 返回 `200`；PostgreSQL 同一 `rbac_role.valid=false`；前置 `auth_user.valid=0`、`org_person.valid=0` | `rbac_role`、`auth_user`、`org_person` | PASS | 无 |

## 本轮证据

- 平台 smoke：`/tmp/adp-platform-validation-acceptance-20260615182929/platform-validation-summary.json`
- 业务模块 API/layout smoke：`/tmp/adp-business-module-smoke-20260615184508.json`
- 业务模块页面 smoke：`/tmp/adp-business-page-smoke-final-20260615204003/business-page-smoke-results.json`
- WOM 制造任务动作发现：`/tmp/adp-production-action-discovery-final8-20260615204438/production-action-discovery.json`
- WOM 生产动作候选页探测：`/tmp/adp-production-action-discovery-candidates-20260615193213/production-action-discovery.json`
- WOM runtime JSON 修复后复验：`/tmp/adp-production-action-discovery-final8-20260615204438/production-action-discovery.json`
- WOM layoutJson 直连复验：5 个动作 viewCode 均 `200`，pageType 为 `EDIT/VIEW`
- WOM 生产动作源地图：`metadata/production-module-source-action-map.json`
- 组织 CRUD 落库验收：`/tmp/adp-organization-persistence-final-20260615203939.json`
- 组织页面截图：`/tmp/adp-organization-persistence-20260615123939/organization-persistence.png`
- Marker：`ADP_E2E_20260615123939_ORGDEP`
- 前端捕获请求：`POST`、`GET detail`、`GET department/person`、`PUT`、`DELETE` 均来自 `http://10.11.100.17:18080/inter-api/organization/v1/department...`
- 组织组管理 CRUD 落库验收：`/tmp/adp-organization-group-persistence-acceptance.json`
- 组织组管理页面截图：`/tmp/adp-organization-group-persistence-20260615133645/organization-group-persistence.png`
- 组管理 Marker：`ADP_E2E_20260615133645_GRP`
- 组管理前端捕获请求：`POST`、`PUT`、`DELETE` 均来自 `http://10.11.100.17:18080/inter-api/organization/v1/group...`
- 组织岗位 CRUD 落库验收：`/tmp/adp-organization-position-persistence-acceptance.json`
- 组织岗位页面截图：`/tmp/adp-organization-position-persistence-20260615134928/organization-position-persistence.png`
- 岗位 Marker：`ADP_E2E_20260615134928_POS`
- 岗位前端捕获请求：`POST`、`PUT`、`DELETE` 均来自 `http://10.11.100.17:18080/inter-api/organization/v1/position...`
- 组织公司 CRUD 落库验收：`/tmp/adp-organization-company-persistence-acceptance.json`
- 组织公司页面截图：`/tmp/adp-organization-company-persistence-20260615141009/organization-company-persistence.png`
- 公司 Marker：`ADP_E2E_20260615141009_COM`
- 公司前端捕获请求：`POST`、`PUT`、`DELETE` 均来自 `http://10.11.100.17:18080/inter-api/organization/v1/company...`
- 组织人员 CRUD 落库验收：`/tmp/adp-organization-person-persistence-acceptance.json`
- 组织人员页面截图：`/tmp/adp-organization-person-persistence-20260615142411/organization-person-persistence.png`
- 人员 Marker：`ADP_E2E_20260615142411_PER`
- 人员前端捕获请求：`POST`、`PUT`、`DELETE` 均来自 `http://10.11.100.17:18080/inter-api/organization/v1/person...`
- 组织人员创建账号落库验收：`/tmp/adp-organization-person-user-persistence-acceptance.json`
- 组织人员创建账号页面截图：`/tmp/adp-organization-person-user-persistence-20260615144220/organization-person-user-persistence.png`
- 人员创建账号 Marker：`ADP_E2E_20260615144220_PUSR`
- 人员创建账号前端捕获请求：`POST`、`PUT`、`DELETE` 均来自 `http://10.11.100.17:18080/inter-api/organization/v1/person...`
- 用户管理账号 CRUD/锁定解锁落库验收：`/tmp/adp-auth-user-persistence-acceptance.json`
- 用户管理页面截图：`/tmp/adp-auth-user-persistence-20260615150418/auth-user-persistence.png`
- 用户管理 Marker：`ADP_E2E_20260615150418_AUSR`
- 用户管理前端捕获请求：`POST /inter-api/auth/v1/user`、`PUT /inter-api/auth/v1/user`、`PUT /inter-api/auth/v1/user/status`、`DELETE /inter-api/auth/v1/user`
- 用户管理修复证据：首次解锁验收暴露 `auth_user.error_count` 缺列，已新增并应用 `deploy/docker/postgres/init/073-auth-user-lock-status-compat.sql` 后复验通过。
- RBAC 权限落库验收：`/tmp/adp-rbac-permission-persistence-current.json`
- RBAC Make 入口复验：`/tmp/adp-rbac-permission-persistence-make-target.json`，marker `ADP_E2E_20260615153823_RBAC`
- RBAC 权限页面截图：`/tmp/adp-rbac-permission-persistence-20260615153240/rbac-permission-persistence.png`
- RBAC Marker：`ADP_E2E_20260615153240_RBAC`
- RBAC 前端捕获请求：`POST /inter-api/rbac/v1/role`、`PUT /inter-api/rbac/v1/role`、`POST /inter-api/rbac/v1/roleUser`、`DELETE /inter-api/rbac/v1/roleUser/{id}`、`POST /inter-api/rbac/v1/rolePermission`、`POST /inter-api/rbac/v1/userPermission`、`GET /inter-api/rbac/v1/data/resource/groups`、`POST /inter-api/rbac/v1/role/{id}/data/resource/{groupCode}`、`POST /inter-api/rbac/v1/user/{id}/data/resource/{groupCode}`、`DELETE /inter-api/rbac/v1/role/{code}`
- RBAC 修复证据：首次角色用户绑定验收暴露 `rbac_roleuser.valid` 默认 `false`，已新增并应用 `deploy/docker/postgres/init/074-rbac-roleuser-valid-default.sql` 后复验通过。
- RBAC 数据资源权限复验：`/tmp/adp-rbac-permission-data-resource-acceptance-rerun.json`，marker `ADP_E2E_20260615155455_RBAC`；首次运行发现缺少数据资源权限表和角色资源权限未同步用户继承行，已新增并应用 `deploy/docker/postgres/init/075-rbac-data-resource-permission-tables.sql` 后复验通过。
- 基础配置-系统编码字典 CRUD 落库验收：`/tmp/adp-systemcode-persistence-second.json`
- 基础配置-系统编码页面截图：`/tmp/adp-systemcode-persistence-20260615161445/systemcode-persistence.png`
- 基础配置-系统编码 Marker：`ADP_E2E_20260615161445_SYSCODE`
- 基础配置-系统编码前端捕获请求：`POST /inter-api/systemcode/v1/entity`、`PUT /inter-api/systemcode/v1/entity`、`POST /inter-api/systemcode/v1/value`、`PUT /inter-api/systemcode/v1/value`、`DELETE /inter-api/systemcode/v1/{entityCode}/values/{valueCode}`、`DELETE /inter-api/systemcode/v1/entities/{entityCode}`

## 未完成范围

- 人员勾选创建账号、独立用户管理账号新增/编辑/锁定/解锁/删除、RBAC 角色/角色用户/角色权限/用户权限、RBAC 数据资源权限已完成真实前端和 PostgreSQL 落库验收。
- 基础配置中的系统编码字典项/字典值 CRUD 已完成真实前端和 PostgreSQL 落库验收；系统配置其他页面、Nacos、Keycloak、runtime patch 仍需继续形成专项验收记录。
- 生产模块当前只有 API/layout 与页面可达性 smoke；`layoutJson` 500 已向前推进为动作页 React #130 渲染阻断，但完整动作级测试用例和写操作落库验收仍未建立，不能视为已完成。

## 记录要求

- 每个功能必须记录真实页面操作步骤、预期结果和实际结果。
- 必须记录 console error、page error、network error、可见系统错误和空白页情况。
- 会改变业务数据的操作必须在 [后端落库验收报告](backend-table-audit/persistence-acceptance.md) 中有对应记录。
- 未执行的功能不要写成 `PASS`。
