## 1.0.2
CREATE TABLE wfm_diagram (
  id BIGINT NOT NULL primary key,
  cid BIGINT NOT NULL,
  app_id NVARCHAR(64) DEFAULT '',
  process_key NVARCHAR(64) NOT NULL,
  process_name NVARCHAR(200) NOT NULL,
  publish_time DATETIME DEFAULT NULL,
  latest_modify_time DATETIME DEFAULT NULL,
  creator_staff NVARCHAR(128) DEFAULT '',
  publisher NVARCHAR(128) DEFAULT '',
  version SMALLINT DEFAULT 0,
  content_id BIGINT NOT NULL,
  enabled BIT DEFAULT 0,
  multi_company BIT DEFAULT 0,
  start_on_mobile BIT DEFAULT 1,
  process_status SMALLINT DEFAULT 1,
  valid BIT DEFAULT 1,
  tenant_id NVARCHAR(64) DEFAULT '',
  creator NVARCHAR(32) DEFAULT '',
  create_staff_id BIGINT DEFAULT NULL,
  create_time DATETIME DEFAULT getutcdate(),
  modifier NVARCHAR(32) DEFAULT '',
  modify_staff_id BIGINT DEFAULT NULL,
  modify_time DATETIME DEFAULT getutcdate()
);
CREATE nonclustered INDEX idx_wfm_diagram_app_id ON wfm_diagram (app_id);
CREATE nonclustered INDEX idx_wfm_diagram_key_versiom ON wfm_diagram (process_key, version);
EXEC sp_addextendedproperty 'MS_Description',N'公司ID','SCHEMA','dbo','TABLE','wfm_diagram','COLUMN','cid';
EXEC sp_addextendedproperty 'MS_Description',N'创建者人员名称','SCHEMA','dbo','TABLE','wfm_diagram','COLUMN','creator_staff';
EXEC sp_addextendedproperty 'MS_Description',N'发布者人员名称','SCHEMA','dbo','TABLE','wfm_diagram','COLUMN','publisher';
EXEC sp_addextendedproperty 'MS_Description',N'最近一次修改组态数据','SCHEMA','dbo','TABLE','wfm_diagram','COLUMN','latest_modify_time';
EXEC sp_addextendedproperty 'MS_Description',N'版本号','SCHEMA','dbo','TABLE','wfm_diagram','COLUMN','version';
EXEC sp_addextendedproperty 'MS_Description',N'0-未启用, 1-已启用','SCHEMA','dbo','TABLE','wfm_diagram','COLUMN','enabled';
EXEC sp_addextendedproperty 'MS_Description',N'0-单公司, 1-多公司','SCHEMA','dbo','TABLE','wfm_diagram','COLUMN','multi_company';
EXEC sp_addextendedproperty 'MS_Description',N'0-不能在移动端启动, 1-可以在移动端启动','SCHEMA','dbo','TABLE','wfm_diagram','COLUMN','start_on_mobile';
EXEC sp_addextendedproperty 'MS_Description',N'1-新增, 2-发布, 3-发布并修改, 4-导入版本','SCHEMA','dbo','TABLE','wfm_diagram','COLUMN','process_status';
EXEC sp_addextendedproperty 'MS_Description',N'0-删除，1-有效','SCHEMA','dbo','TABLE','wfm_diagram','COLUMN','valid';

CREATE TABLE wfm_diagram_content (
	id BIGINT NOT NULL primary key,
	published_json TEXT DEFAULT NULL,
  	draft_json TEXT DEFAULT NULL,
  	xml TEXT DEFAULT NULL,
  	creator NVARCHAR(32) DEFAULT '',
  	create_staff_id BIGINT DEFAULT NULL,
  	create_time DATETIME DEFAULT getutcdate(),
  	modifier NVARCHAR(32) DEFAULT '',
  	modify_staff_id BIGINT DEFAULT NULL,
  	modify_time DATETIME DEFAULT getutcdate()
);
EXEC sp_addextendedproperty 'MS_Description',N'已发布的流程组态数据','SCHEMA','dbo','TABLE','wfm_diagram_content','COLUMN','published_json';
EXEC sp_addextendedproperty 'MS_Description',N'流程组态草稿','SCHEMA','dbo','TABLE','wfm_diagram_content','COLUMN','draft_json';
EXEC sp_addextendedproperty 'MS_Description',N'流程组态bpmn','SCHEMA','dbo','TABLE','wfm_diagram_content','COLUMN','xml';

CREATE TABLE wfm_task_form (
	id BIGINT NOT NULL primary key,
	process_id NVARCHAR(64) NOT NULL,
	instance_id BIGINT NOT NULL,
	user_id BIGINT NOT NULL,
	form_data TEXT DEFAULT NULL,
  	form_temp_data TEXT DEFAULT NULL,
	creator NVARCHAR(32) DEFAULT '',
	create_staff_id BIGINT DEFAULT NULL,
  	create_time DATETIME DEFAULT getutcdate(),
 	modifier NVARCHAR(32) DEFAULT '',
 	modify_staff_id BIGINT DEFAULT NULL,
  	modify_time DATETIME DEFAULT getutcdate()
);
CREATE UNIQUE INDEX idx_wfm_form_ins_id ON wfm_task_form (instance_id, user_id);
EXEC sp_addextendedproperty 'MS_Description',N'流程实例ID','SCHEMA','dbo','TABLE','wfm_task_form','COLUMN','process_id';
EXEC sp_addextendedproperty 'MS_Description',N'任务实例ID','SCHEMA','dbo','TABLE','wfm_task_form','COLUMN','instance_id';
EXEC sp_addextendedproperty 'MS_Description',N'用户ID','SCHEMA','dbo','TABLE','wfm_task_form','COLUMN','user_id';
EXEC sp_addextendedproperty 'MS_Description',N'表单数据','SCHEMA','dbo','TABLE','wfm_task_form','COLUMN','form_data';
EXEC sp_addextendedproperty 'MS_Description',N'表单临时保存数据','SCHEMA','dbo','TABLE','wfm_task_form','COLUMN','form_temp_data';

CREATE TABLE wfm_process (
	id BIGINT NOT NULL primary key,
	app_id NVARCHAR(64) DEFAULT NULL,
	user_id BIGINT NOT NULL,
	staff_name NVARCHAR(64) NOT NULL,
	process_key NVARCHAR(64) DEFAULT NULL,
  	process_version SMALLINT DEFAULT 0,
  	process_name NVARCHAR(200) NOT NULL,
  	process_status SMALLINT DEFAULT 88,
    cid BIGINT DEFAULT NULL,
    complete_time DATETIME DEFAULT NULL,
    tenant_id NVARCHAR(64) DEFAULT '',
    creator NVARCHAR(32) DEFAULT NULL,
	create_staff_id BIGINT DEFAULT NULL,
	create_time DATETIME DEFAULT getutcdate(),
	modifier NVARCHAR(32) DEFAULT NULL,
	modify_staff_id BIGINT DEFAULT NULL,
	modify_time DATETIME DEFAULT getutcdate()
);
CREATE nonclustered INDEX idx_wfm_process_user_id ON wfm_process (user_id);
EXEC sp_addextendedproperty 'MS_Description',N'发起者ID','SCHEMA','dbo','TABLE','wfm_process','COLUMN','user_id';
EXEC sp_addextendedproperty 'MS_Description',N'发起者人员名称','SCHEMA','dbo','TABLE','wfm_process','COLUMN','staff_name';
EXEC sp_addextendedproperty 'MS_Description',N'流程编号','SCHEMA','dbo','TABLE','wfm_process','COLUMN','process_key';
EXEC sp_addextendedproperty 'MS_Description',N'流程版本','SCHEMA','dbo','TABLE','wfm_process','COLUMN','process_version';
EXEC sp_addextendedproperty 'MS_Description',N'流程名称','SCHEMA','dbo','TABLE','wfm_process','COLUMN','process_name';
EXEC sp_addextendedproperty 'MS_Description',N'88-进行中 77-暂停','SCHEMA','dbo','TABLE','wfm_process','COLUMN','process_status';
EXEC sp_addextendedproperty 'MS_Description',N'公司ID','SCHEMA','dbo','TABLE','wfm_process','COLUMN','cid';

CREATE TABLE wfm_process_attention (
	id BIGINT NOT NULL primary key,
	app_id NVARCHAR(64) DEFAULT NULL,
	process_id NVARCHAR(64) NOT NULL,
	user_id BIGINT NOT NULL,
	initiator_id NVARCHAR(64) DEFAULT NULL,
  	staff_name NVARCHAR(128) DEFAULT NULL,
	tenant_id NVARCHAR(64) DEFAULT '',
    creator NVARCHAR(32) DEFAULT NULL,
	create_staff_id BIGINT DEFAULT NULL,
	create_time DATETIME DEFAULT getutcdate(),
	modifier NVARCHAR(32) DEFAULT NULL,
	modify_staff_id BIGINT DEFAULT NULL,
	modify_time DATETIME DEFAULT getutcdate()
);
CREATE nonclustered INDEX idx_attention_user_id ON wfm_process_attention (user_id);

 CREATE TABLE wfm_task_complete (
  id BIGINT NOT NULL primary key,
  app_id NVARCHAR(64) DEFAULT NULL,
  cid BIGINT DEFAULT 0,
  user_id BIGINT NOT NULL,
  person_name NVARCHAR(128) DEFAULT NULL,
  task_name NVARCHAR(512) DEFAULT NULL,
  activity_name NVARCHAR(64) DEFAULT NULL,
  open_url NVARCHAR(1024) DEFAULT NULL,
  instance_id NVARCHAR(64) DEFAULT NULL,
  process_key NVARCHAR(64) DEFAULT '',
  process_version SMALLINT DEFAULT 0,
  process_id NVARCHAR(64) DEFAULT NULL,
  process_name NVARCHAR(64) DEFAULT NULL,
  initiator_id NVARCHAR(64) DEFAULT NULL,
  staff_name NVARCHAR(64) DEFAULT NULL,
  start_time DATETIME DEFAULT NULL,
  proxy_source BIGINT DEFAULT NULL,
  source_staff BIGINT DEFAULT NULL,
  table_no NVARCHAR(64) DEFAULT NULL,
  task_type BIGINT DEFAULT 0,
  task_source NVARCHAR(32) DEFAULT NULL,
  latest_user NVARCHAR(64) DEFAULT NULL,
  integration_id NVARCHAR(64),
  reject BIT DEFAULT 0,
  tenant_id NVARCHAR(64) DEFAULT '',
  row_version BIGINT DEFAULT NULL,
  creator NVARCHAR(32) DEFAULT NULL,
  create_staff_id BIGINT DEFAULT NULL,
  create_time DATETIME DEFAULT getutcdate(),
  modifier NVARCHAR(32) DEFAULT NULL,
  modify_staff_id BIGINT DEFAULT NULL,
  modify_time DATETIME DEFAULT getutcdate()
 );
 CREATE nonclustered INDEX idx_wfm_task_complete_userid ON wfm_task_complete (user_id);
 EXEC sp_addextendedproperty 'MS_Description',N'App ID','SCHEMA','dbo','TABLE','wfm_task_complete','COLUMN','app_id';
 EXEC sp_addextendedproperty 'MS_Description',N'人员名称','SCHEMA','dbo','TABLE','wfm_task_complete','COLUMN','person_name';
 EXEC sp_addextendedproperty 'MS_Description',N'活动名称','SCHEMA','dbo','TABLE','wfm_task_complete','COLUMN','task_name';
 EXEC sp_addextendedproperty 'MS_Description',N'活动节点ID','SCHEMA','dbo','TABLE','wfm_task_complete','COLUMN','activity_name';
 EXEC sp_addextendedproperty 'MS_Description',N'bap业务ID','SCHEMA','dbo','TABLE','wfm_task_complete','COLUMN','instance_id';
 EXEC sp_addextendedproperty 'MS_Description',N'流程实例ID','SCHEMA','dbo','TABLE','wfm_task_complete','COLUMN','process_id';
 EXEC sp_addextendedproperty 'MS_Description',N'流程编号','SCHEMA','dbo','TABLE','wfm_task_complete','COLUMN','process_key';
 EXEC sp_addextendedproperty 'MS_Description',N'流程版本','SCHEMA','dbo','TABLE','wfm_task_complete','COLUMN','process_version';
 EXEC sp_addextendedproperty 'MS_Description',N'流程名称','SCHEMA','dbo','TABLE','wfm_task_complete','COLUMN','process_name';
 EXEC sp_addextendedproperty 'MS_Description',N'公司ID','SCHEMA','dbo','TABLE','wfm_task_complete','COLUMN','cid';
 EXEC sp_addextendedproperty 'MS_Description',N'单据编号','SCHEMA','dbo','TABLE','wfm_task_complete','COLUMN','table_no';
 EXEC sp_addextendedproperty 'MS_Description',N'关联页面url','SCHEMA','dbo','TABLE','wfm_task_complete','COLUMN','open_url';
 EXEC sp_addextendedproperty 'MS_Description',N'流程发起者','SCHEMA','dbo','TABLE','wfm_task_complete','COLUMN','initiator_id';
 EXEC sp_addextendedproperty 'MS_Description',N'流程发起者人员名称','SCHEMA','dbo','TABLE','wfm_task_complete','COLUMN','staff_name';
 EXEC sp_addextendedproperty 'MS_Description',N'数据来源 bap, supOS','SCHEMA','dbo','TABLE','wfm_task_complete','COLUMN','task_source';
 EXEC sp_addextendedproperty 'MS_Description',N'0-普通待办 2-委托待办','SCHEMA','dbo','TABLE','wfm_task_complete','COLUMN','task_type';
 EXEC sp_addextendedproperty 'MS_Description',N'上个环节提交者','SCHEMA','dbo','TABLE','wfm_task_complete','COLUMN','latest_user';
 EXEC sp_addextendedproperty 'MS_Description',N'外部系统集成ID','SCHEMA','dbo','TABLE','wfm_task_complete','COLUMN','integration_id';
 EXEC sp_addextendedproperty 'MS_Description',N'是否驳回 0-no, 1-yes','SCHEMA','dbo','TABLE','wfm_task_complete','COLUMN','reject';

 CREATE TABLE wfm_process_log (
  id BIGINT NOT NULL primary key,
  process_id NVARCHAR(64) NOT NULL,
  process_name NVARCHAR(200),
  task_id BIGINT DEFAULT NULL,
  task_name NVARCHAR(128) DEFAULT '',
  audit_result NVARCHAR(32) DEFAULT NULL,
  leave_comment NVARCHAR(1000) DEFAULT NULL,
  action_desc NVARCHAR(256) DEFAULT NULL,
  action_type NVARCHAR(32) DEFAULT NULL,
  tenant_id NVARCHAR(64) DEFAULT '',
  creator NVARCHAR(32) DEFAULT '',
  create_staff_id BIGINT DEFAULT NULL,
  create_time DATETIME DEFAULT getutcdate(),
  modifier NVARCHAR(32) DEFAULT '',
  modify_staff_id BIGINT DEFAULT NULL,
  modify_time DATETIME DEFAULT getutcdate()
);
CREATE nonclustered INDEX idx_wfm_log_process_id ON wfm_process_log (process_id);
EXEC sp_addextendedproperty 'MS_Description',N'审批结果: 同意OR驳回','SCHEMA','dbo','TABLE','wfm_process_log','COLUMN','audit_result';
EXEC sp_addextendedproperty 'MS_Description',N'用户提交的备注','SCHEMA','dbo','TABLE','wfm_process_log','COLUMN','leave_comment';
EXEC sp_addextendedproperty 'MS_Description',N'操作描述, 格式: I18N_MESSAGE_CODE#参数1#参数2','SCHEMA','dbo','TABLE','wfm_process_log','COLUMN','action_desc';
EXEC sp_addextendedproperty 'MS_Description',N'操作类型','SCHEMA','dbo','TABLE','wfm_process_log','COLUMN','action_type';

CREATE TABLE wfm_entrust (
  id BIGINT NOT NULL primary key,
  cid BIGINT NOT NULL,
  app_id NVARCHAR(64) DEFAULT '',
  principal BIGINT DEFAULT 0,
  mandatary BIGINT DEFAULT 0,
  mandatary_name NVARCHAR(64) DEFAULT '',
  process_id NVARCHAR(64) DEFAULT '',
  process_name NVARCHAR(200) DEFAULT '',
  task_id BIGINT NOT NULL,
  task_name NVARCHAR(512) DEFAULT '',
  description NVARCHAR(512) DEFAULT NULL,
  tenant_id NVARCHAR(64) DEFAULT '',
  creator NVARCHAR(32) DEFAULT '',
  create_staff_id BIGINT DEFAULT NULL,
  create_time DATETIME DEFAULT getutcdate(),
  modifier NVARCHAR(32) DEFAULT '',
  modify_staff_id BIGINT DEFAULT NULL,
  modify_time DATETIME DEFAULT getutcdate()
);
CREATE nonclustered INDEX idx_wfm_entrust_principal ON wfm_entrust (principal);
EXEC sp_addextendedproperty 'MS_Description',N'公司ID','SCHEMA','dbo','TABLE','wfm_entrust','COLUMN','cid';
EXEC sp_addextendedproperty 'MS_Description',N'委托方','SCHEMA','dbo','TABLE','wfm_entrust','COLUMN','principal';
EXEC sp_addextendedproperty 'MS_Description',N'被委托方','SCHEMA','dbo','TABLE','wfm_entrust','COLUMN','mandatary';
EXEC sp_addextendedproperty 'MS_Description',N'委托原因','SCHEMA','dbo','TABLE','wfm_entrust','COLUMN','description';

CREATE TABLE wfm_task_pending (
  id BIGINT NOT NULL primary key,
  user_id BIGINT NOT NULL,
  person_name NVARCHAR(128) DEFAULT NULL,
  app_id NVARCHAR(64) DEFAULT NULL,
  task_description NVARCHAR(512) DEFAULT NULL,
  activity_type NVARCHAR(32) DEFAULT NULL,
  activity_name NVARCHAR(64) DEFAULT NULL,
  execution_id NVARCHAR(128) DEFAULT NULL,
  task_status SMALLINT NOT NULL DEFAULT 88,
  open_url NVARCHAR(1024) DEFAULT NULL,
  instance_id NVARCHAR(64) DEFAULT NULL,
  process_key NVARCHAR(64) DEFAULT NULL,
  process_version INT DEFAULT 0,
  process_name NVARCHAR(64) DEFAULT NULL,
  process_description NVARCHAR(512) DEFAULT NULL,
  initiator_id BIGINT DEFAULT NULL,
  initiator_name NVARCHAR(64) DEFAULT NULL,
  staff_name NVARCHAR(512) DEFAULT NULL,
  process_id NVARCHAR(64) DEFAULT NULL,
  table_info_id BIGINT DEFAULT NULL,
  entity_code NVARCHAR(64) DEFAULT NULL,
  table_no NVARCHAR(64) DEFAULT NULL,
  deployment_id BIGINT DEFAULT 0,
  task_type SMALLINT DEFAULT 0,
  proxy_source BIGINT DEFAULT NULL,
  description NVARCHAR(512) DEFAULT NULL,
  loops SMALLINT DEFAULT NULL,
  task_source NVARCHAR(32) DEFAULT NULL,
  cid BIGINT DEFAULT NULL,
  model_id BIGINT DEFAULT NULL,
  main_loop SMALLINT DEFAULT 0,
  multi_company BIT DEFAULT 0,
  source_staff BIGINT DEFAULT 0,
  mobile_approve BIT DEFAULT 1,
  description_zh_cn NVARCHAR(512) DEFAULT NULL,
  task_description_zh_cn NVARCHAR(512) DEFAULT NULL,
  process_description_zh_cn NVARCHAR(512) DEFAULT NULL,
  integration_id NVARCHAR(64) DEFAULT NULL,
  row_version INT DEFAULT NULL,
  version INT DEFAULT NULL,
  tenant_id NVARCHAR(64) DEFAULT '',
  creator NVARCHAR(32) DEFAULT NULL,
  create_staff_id BIGINT DEFAULT NULL,
  create_time DATETIME DEFAULT getutcdate(),
  modifier NVARCHAR(32) DEFAULT NULL,
  modify_staff_id BIGINT DEFAULT NULL,
  modify_time DATETIME DEFAULT  getutcdate(),
);
create nonclustered INDEX idx_wfm_task_pending_user_id ON wfm_task_pending(user_id);
create nonclustered INDEX idx_wfm_task_pending_process_id ON wfm_task_pending(process_id);
CREATE nonclustered INDEX idx_task_pending_instanceid ON wfm_task_pending(instance_id);
EXEC sp_addextendedproperty 'MS_Description',N'待白执行者','SCHEMA','dbo','TABLE','wfm_task_pending','COLUMN','user_id';
EXEC sp_addextendedproperty 'MS_Description',N'活动名称-国际化编号','SCHEMA','dbo','TABLE','wfm_task_pending','COLUMN','task_description';
EXEC sp_addextendedproperty 'MS_Description',N'活动类型','SCHEMA','dbo','TABLE','wfm_task_pending','COLUMN','activity_type';
EXEC sp_addextendedproperty 'MS_Description',N'活动ID','SCHEMA','dbo','TABLE','wfm_task_pending','COLUMN','activity_name';
EXEC sp_addextendedproperty 'MS_Description',N'活动实例ID','SCHEMA','dbo','TABLE','wfm_task_pending','COLUMN','execution_id';
EXEC sp_addextendedproperty 'MS_Description',N'88-进行中 77-暂停','SCHEMA','dbo','TABLE','wfm_task_pending','COLUMN','task_status';
EXEC sp_addextendedproperty 'MS_Description',N'待办关联url','SCHEMA','dbo','TABLE','wfm_task_pending','COLUMN','open_url';
EXEC sp_addextendedproperty 'MS_Description',N'bap业务ID','SCHEMA','dbo','TABLE','wfm_task_pending','COLUMN','instance_id';
EXEC sp_addextendedproperty 'MS_Description',N'流程编号','SCHEMA','dbo','TABLE','wfm_task_pending','COLUMN','process_key';
EXEC sp_addextendedproperty 'MS_Description',N'流程版本','SCHEMA','dbo','TABLE','wfm_task_pending','COLUMN','process_version';
EXEC sp_addextendedproperty 'MS_Description',N'流程名称','SCHEMA','dbo','TABLE','wfm_task_pending','COLUMN','process_name';
EXEC sp_addextendedproperty 'MS_Description',N'流程定义描述','SCHEMA','dbo','TABLE','wfm_task_pending','COLUMN','process_description';
EXEC sp_addextendedproperty 'MS_Description',N'流程发起者','SCHEMA','dbo','TABLE','wfm_task_pending','COLUMN','initiator_id';
EXEC sp_addextendedproperty 'MS_Description',N'流程发起者姓名','SCHEMA','dbo','TABLE','wfm_task_pending','COLUMN','initiator_name';
EXEC sp_addextendedproperty 'MS_Description',N'流程实例ID','SCHEMA','dbo','TABLE','wfm_task_pending','COLUMN','process_id';
EXEC sp_addextendedproperty 'MS_Description',N'单据编号','SCHEMA','dbo','TABLE','wfm_task_pending','COLUMN','table_no';
EXEC sp_addextendedproperty 'MS_Description',N'流程部署ID','SCHEMA','dbo','TABLE','wfm_task_pending','COLUMN','deployment_id';
EXEC sp_addextendedproperty 'MS_Description',N'待办类型 0-普通待办 2-委托待办','SCHEMA','dbo','TABLE','wfm_task_pending','COLUMN','task_type';
EXEC sp_addextendedproperty 'MS_Description',N'最近一次委托者','SCHEMA','dbo','TABLE','wfm_task_pending','COLUMN','proxy_source';
EXEC sp_addextendedproperty 'MS_Description',N'委托原因','SCHEMA','dbo','TABLE','wfm_task_pending','COLUMN','description';
EXEC sp_addextendedproperty 'MS_Description',N'循环会签 0不是循环会签,1表示本公司，2表示夸公司，3表示本部门,4表示本部门及下级，5自定义','SCHEMA','dbo','TABLE','wfm_task_pending','COLUMN','loops';
EXEC sp_addextendedproperty 'MS_Description',N'数据来源 bap, supOS','SCHEMA','dbo','TABLE','wfm_task_pending','COLUMN','task_source';
EXEC sp_addextendedproperty 'MS_Description',N'公司ID','SCHEMA','dbo','TABLE','wfm_task_pending','COLUMN','cid';
EXEC sp_addextendedproperty 'MS_Description',N'是否是主办人','SCHEMA','dbo','TABLE','wfm_task_pending','COLUMN','main_loop';
EXEC sp_addextendedproperty 'MS_Description',N'是否跨公司','SCHEMA','dbo','TABLE','wfm_task_pending','COLUMN','multi_company';
EXEC sp_addextendedproperty 'MS_Description',N'委托的原始用户','SCHEMA','dbo','TABLE','wfm_task_pending','COLUMN','source_staff';
EXEC sp_addextendedproperty 'MS_Description',N'启动移动客户端审批','SCHEMA','dbo','TABLE','wfm_task_pending','COLUMN','mobile_approve';

alter table wfm_task_pending add start_time DATETIME DEFAULT getutcdate();
EXEC sp_addextendedproperty 'MS_Description',N'待办接收时间','SCHEMA','dbo','TABLE','wfm_task_pending','COLUMN','start_time';
alter table wfm_task_complete add end_time DATETIME DEFAULT getutcdate();
EXEC sp_addextendedproperty 'MS_Description',N'待办结束时间','SCHEMA','dbo','TABLE','wfm_task_complete','COLUMN','end_time';
alter table wfm_process_attention add table_no NVARCHAR(64) DEFAULT NULL;
EXEC sp_addextendedproperty 'MS_Description',N'单据编号','SCHEMA','dbo','TABLE','wfm_process_attention','COLUMN','table_no';
alter table wfm_process add table_no NVARCHAR(64) DEFAULT NULL;
EXEC sp_addextendedproperty 'MS_Description',N'单据编号','SCHEMA','dbo','TABLE','wfm_process','COLUMN','table_no';
alter table wfm_entrust add table_no NVARCHAR(64) DEFAULT NULL;
EXEC sp_addextendedproperty 'MS_Description',N'单据编号','SCHEMA','dbo','TABLE','wfm_entrust','COLUMN','table_no';
alter table wfm_entrust add instance_id NVARCHAR(64) DEFAULT NULL;
EXEC sp_addextendedproperty 'MS_Description',N'待办实例ID','SCHEMA','dbo','TABLE','wfm_entrust','COLUMN','instance_id';
alter table wfm_task_complete add showlog BIT DEFAULT 1;
EXEC sp_addextendedproperty 'MS_Description',N'是否显示流程日志','SCHEMA','dbo','TABLE','wfm_task_complete','COLUMN','showlog';
alter table wfm_task_pending add person_name NVARCHAR(128) DEFAULT NULL;
EXEC sp_addextendedproperty 'MS_Description',N'人员名','SCHEMA','dbo','TABLE','wfm_task_pending','COLUMN','person_name';
alter table wfm_task_pending add attention BIT DEFAULT 0;
EXEC sp_addextendedproperty 'MS_Description',N'是否关注','SCHEMA','dbo','TABLE','wfm_task_pending','COLUMN','attention';
alter table wfm_task_pending add has_read BIT DEFAULT 0;
EXEC sp_addextendedproperty 'MS_Description',N'是否已读 0-未读, 1-已读','SCHEMA','dbo','TABLE','wfm_task_pending','COLUMN','has_read';
alter table wfm_task_pending add latest_user NVARCHAR(64) DEFAULT NULL;
EXEC sp_addextendedproperty 'MS_Description',N'上个环节提交者人员名称','SCHEMA','dbo','TABLE','wfm_task_pending','COLUMN','latest_user';
alter table wfm_task_pending add due_time DATETIME DEFAULT NULL;
EXEC sp_addextendedproperty 'MS_Description',N'过期时间','SCHEMA','dbo','TABLE','wfm_task_pending','COLUMN','due_time';

## 1.0.3
alter table wfm_task_pending alter column process_id NVARCHAR(64) NULL;
update wfm_task_pending set tenant_id='dt' where tenant_id IS NULL or tenant_id='';
## 1.0.4
drop index wfm_task_form.idx_wfm_form_ins_id;
alter table wfm_task_form alter column instance_id NVARCHAR(64) NULL;
CREATE UNIQUE INDEX idx_wfm_form_ins_id ON wfm_task_form (instance_id, user_id);