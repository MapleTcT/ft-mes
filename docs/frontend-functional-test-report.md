# 前端功能测试报告

## 当前状态

本文件是功能验收记录入口。当前已记录一轮平台基础 smoke 和组织部门 CRUD 落库验收；未列入的页面和生产模块完整功能仍未完成验收。

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
| 被测功能 | 4 |
| PASS | 4 |
| FAIL | 0 |
| BLOCKED | 0 |
| NOT_APPLICABLE | 0 |

## 功能验收明细

| 模块 | 页面/路由 | 操作 | API | 前端结果 | 后端结果 | 数据库表 | 验收状态 | 问题 |
|---|---|---|---|---|---|---|---|---|
| 平台基础 | `http://10.11.100.17:18080` | 登录、平台 API、首页待办、组织只读、RBAC 权限、菜单页面 smoke | `/inter-api/auth/login`、`/inter-api/rbac/v1/menus/currentUser`、组织/待办/RBAC 相关接口 | Playwright 真实浏览器 smoke `6/6` passed；首页待办无 visible error、network error、console error；菜单抽样 `12/12` passed | platform API `16/16` passed；RBAC authority `9/9` passed；组织只读接口通过 | 不适用 | PASS | 证据：`/tmp/adp-platform-validation-acceptance-20260615182929/platform-validation-summary.json` |
| 组织管理 | `/organization/#/organizationmanage` | 选择默认公司，打开新增部门弹窗，新增部门 `ADP_E2E_20260615103620_ORGDEP` | `POST /inter-api/organization/v1/department` | 真实浏览器打开组织管理页面并提交新增弹窗；navigation status `200`；无 console error、page error、request failure、visible error | 返回 `200`，响应包含新部门 id `6587462000591376`；查询详情接口返回新增数据 | `org_department` | PASS | 无 |
| 组织管理 | `/organization/#/organizationmanage` | 在已登录组织页面上下文编辑部门名称和描述 | `PUT /inter-api/organization/v1/department` | 同一浏览器页面上下文发起同源请求；无 console error、page error、request failure、visible error | 返回 `200`；PostgreSQL 同一 id 的 `name` 和 `description` 更新为 marker EDIT 值 | `org_department` | PASS | 无 |
| 组织管理 | `/organization/#/organizationmanage` | 在已登录组织页面上下文删除部门 | `DELETE /inter-api/organization/v1/department/6587462000591376` | 同一浏览器页面上下文发起同源请求；无 console error、page error、request failure、visible error | 返回 `200`；PostgreSQL 同一 id 软删除，`valid=0` | `org_department` | PASS | 无 |

## 本轮证据

- 平台 smoke：`/tmp/adp-platform-validation-acceptance-20260615182929/platform-validation-summary.json`
- 组织 CRUD 落库验收：`/tmp/adp-organization-persistence-20260615183619.json`
- 组织页面截图：`/tmp/adp-organization-persistence-20260615103620/organization-persistence.png`
- Marker：`ADP_E2E_20260615103620_ORGDEP`
- 前端捕获请求：`POST`、`GET detail`、`GET department/person`、`PUT`、`DELETE` 均来自 `http://10.11.100.17:18080/inter-api/organization/v1/department...`

## 未完成范围

- 用户、岗位、人员、公司、组管理的新增/编辑/删除落库验收仍需补。
- 基础配置、Nacos、Keycloak、runtime patch 仍需继续形成专项验收记录。
- 生产模块完整功能测试用例尚未建立，不能视为已完成。

## 记录要求

- 每个功能必须记录真实页面操作步骤、预期结果和实际结果。
- 必须记录 console error、page error、network error、可见系统错误和空白页情况。
- 会改变业务数据的操作必须在 [后端落库验收报告](backend-table-audit/persistence-acceptance.md) 中有对应记录。
- 未执行的功能不要写成 `PASS`。
