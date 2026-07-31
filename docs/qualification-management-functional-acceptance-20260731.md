# 资质管理功能与落库验收

## 验收基线

- 验收时间：2026-07-31
- 测试环境：`http://10.11.100.17:18080`
- 数据库：PostgreSQL
- 功能提交：`da10c0d26710479c282172821a334373c17aa31e`
- 验收方式：真实浏览器页面、Network 请求、后端响应和 PostgreSQL marker 查库

本轮按原产品运行时布局恢复动作，不给查询页或导入页虚构通用 CRUD。页面动作通过表示该页面按原产品语义正常显示并可进入；只有下方“落库验收”中的动作才表示已完成页面、API、PostgreSQL 三层闭环。

## 页面动作验收

| 模块 | 页面/路由 | 原产品动作 | 页面结果 | Console/Network | 落库范围 | 状态 |
|---|---|---|---|---|---|---|
| 过期提醒人 | `/msService/Qualify/reminderSet/reminderSet/reminderSetLayout` | 新增、修改、删除、导入 | 动作栏正常显示，新增/编辑入口可打开 | 无 console error；无 HTTP 4xx/5xx | 本轮未提交提醒人业务数据 | PASS |
| 人员资质查询 | `/msService/Qualify/staffCertificate/staffCert/staffCertList` | 查询、清空 | 查询控件正常显示 | 无 console error；无 HTTP 4xx/5xx | 查询页，不应提供新增/删除 | PASS |
| 企业资质（导入） | `/msService/Qualify/companyCertificateImport/companyCertImp/companyCertImpList` | 导入 | 导入动作正常显示 | 无 console error；无 HTTP 4xx/5xx | 本轮未上传真实文件 | PASS |
| 人员资质设置 | `/msService/Qualify/staffCertificate/certStaff/certStaffList` | 人员资质设置、外部修改记录查看 | 领域动作正常显示 | 无 console error；无 HTTP 4xx/5xx | 本轮未提交人员资质分配 | PASS |
| 企业资质设置 | `/msService/Qualify/companyCertificate/certCompany/certCompanyList` | 企业资质设置 | 领域动作正常显示 | 无 console error；无 HTTP 4xx/5xx | 本轮未提交企业资质分配 | PASS |
| 资质分类 | `/msService/Qualify/certificateType/certificateType/cerTypeLayOut` | 新增、修改、删除、导入 | 动作栏和中文弹窗正常 | 无 console error；业务请求 HTTP 200 | `qlf_certificate_types`、`qlf_certificate_types_mc` | PASS |
| 企业资质查询 | `/msService/Qualify/companyCertificate/companyCert/companyCertList` | 查询、清空 | 查询控件正常显示 | 无 console error；无 HTTP 4xx/5xx | 查询页，不应提供新增/删除 | PASS |
| 过期提醒待办 | `/msService/Qualify/reminderPend/reminderPend/reminderPendList` | 处理 | 处理动作正常显示 | 无 console error；无 HTTP 4xx/5xx | 本轮未驱动完整提醒待办状态机 | PASS |
| 资质 | `/msService/Qualify/certificate/certificate/certifcateLayOut` | 新增、修改、删除、导入 | 动作栏、资质编辑和等级明细正常 | 无 console error；业务请求 HTTP 200 | `qlf_certificates`、`qlf_certificate_levels` | PASS |
| 人员资质（导入） | `/msService/Qualify/staffCertificateImport/staffCertImp/staffCertImpList` | 导入 | 导入动作正常显示 | 无 console error；无 HTTP 4xx/5xx | 本轮未上传真实文件 | PASS |

结论：资质管理目录 10/10 页面通过原产品动作语义验收。截图中整页“没有新增、查看、删除”的根因已关闭；查询页、导入页、设置页和待办页仍保持各自的业务动作，不统一塞入无意义的 CRUD。

## 落库验收

### 资质分类

- Marker：`ADP_E2E_20260731_QUAL_TYPE_1785456672809`
- 记录 ID：`771611831981312`
- 新增/修改入口：`/msService/Qualify/certificateType/certificateType/cerTypeEdit/submit`
- 删除入口：`/msService/Qualify/certificateType/certificateType/delete`
- 实际结果：新增、修改、软删除请求均为 HTTP 200；最终分类名称为 marker 加 `_EDIT`，`valid=false`，父节点为人员资质根分类 `1000`；助记码明细清理为 0 行。

验收 SQL：

```sql
SELECT id, name, valid, parent_id
FROM public.qlf_certificate_types
WHERE id = 771611831981312;

SELECT count(*)
FROM public.qlf_certificate_types_mc
WHERE COALESCE(certificate_type, certificate_type_id, certificatetype_id, cer_type)
      = 771611831981312;
```

### 资质主档与等级

- Marker：`ADP_E2E_QUAL_CERT_1785460608011`
- 主档 ID：`771626883347712`
- 等级 ID：`771626884224256`
- 新增/修改入口：`/msService/Qualify/certificate/certificate/certificateEdit/submit`
- 删除入口：`/msService/Qualify/certificate/certificate/delete`
- 实际结果：
  - 新增 HTTP 200，响应 `operate=add`，主档和等级同时写入。
  - 修改 HTTP 200，响应 `operate=edit`；名称、备注和等级名称均真实更新。
  - 删除 HTTP 200，页面提示“删除成功！”；主档最终 `valid=false`、`version=2`，等级明细为 0 行。

验收 SQL：

```sql
SELECT id, code, name, memo_field, cer_type, valid, version
FROM public.qlf_certificates
WHERE id = 771626883347712;

SELECT id, certificate, code, name, memo_field, valid, version
FROM public.qlf_certificate_levels
WHERE id = 771626884224256;
```

## 已关闭根因

1. Qualify 原始运行时视图和按钮权限未进入 PostgreSQL 运行元数据，导致整页动作栏缺失。
2. Qualify 静态页面与模块国际化资源未纳入 Linux/Nginx 部署，导致弹窗空白或按钮文本显示为 key。
3. 资质分类根节点、助记码字段别名和系统配置缺失，造成 PostgreSQL 查询或保存链不完整。
4. `QualifyConfigureUtil` 在租户配置未命中时返回 `null`，业务保存链自动拆箱后触发异常；补丁改为保留动态配置，并在缺省时安全返回 `false`。

## 后续验收边界

以下功能已经恢复入口和页面交互，但本轮没有把它们写成“业务落库已通过”：

- 人员/企业资质 Excel 导入的文件解析、错误回执和事务回滚。
- 人员资质设置、企业资质设置的分配、续期、撤销和历史记录。
- 过期提醒人、过期提醒待办的定时生成、处理状态和消息送达。

这些动作应在下一轮分别使用唯一 marker、真实请求、目标表 SQL 和清理步骤完成闭环。
