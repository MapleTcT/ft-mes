## 1.0.2
CREATE TABLE wfm_diagram (
  id NUMBER(20,0) NOT NULL,
  cid NUMBER(20,0) NOT NULL,
  app_id VARCHAR2(64) DEFAULT '',
  process_key VARCHAR2(64) NOT NULL,
  process_name VARCHAR2(200) NOT NULL,
  publish_time TIMESTAMP DEFAULT NULL,
  latest_modify_time TIMESTAMP DEFAULT NULL,
  creator_staff VARCHAR2(128) DEFAULT '',
  publisher VARCHAR2(128) DEFAULT '',
  version NUMBER(5,0) DEFAULT 0,
  content_id NUMBER(20,0) NOT NULL,
  enabled NUMBER(1,0) DEFAULT 0,
  multi_company NUMBER(1,0) DEFAULT 0,
  start_on_mobile NUMBER(1,0) DEFAULT 1,
  process_status NUMBER(2,0) DEFAULT 1,
  valid NUMBER(1,0) DEFAULT 1,
  tenant_id VARCHAR2(64) DEFAULT '',
  creator VARCHAR2(32) DEFAULT '',
  create_staff_id NUMBER(20, 0) DEFAULT NULL,
  create_time TIMESTAMP DEFAULT systimestamp,
  modifier VARCHAR2(32) DEFAULT '',
  modify_staff_id NUMBER(20, 0) DEFAULT NULL,
  modify_time TIMESTAMP DEFAULT systimestamp,
  CONSTRAINT pk_wfm_diagram_id PRIMARY KEY (id)
);
CREATE INDEX idx_wfm_diagram_app_id ON wfm_diagram (app_id);
CREATE INDEX idx_wfm_diagram_key_versiom ON wfm_diagram (process_key, version);
COMMENT ON COLUMN wfm_diagram.cid IS '公司ID';
COMMENT ON COLUMN wfm_diagram.creator_staff IS '创建者人员名称';
COMMENT ON COLUMN wfm_diagram.publisher IS '发布者人员名称';
COMMENT ON COLUMN wfm_diagram.latest_modify_time IS '最近一次修改组态数据';
COMMENT ON COLUMN wfm_diagram.version IS '版本号';
COMMENT ON COLUMN wfm_diagram.enabled IS '0-未启用, 1-已启用';
COMMENT ON COLUMN wfm_diagram.multi_company IS '0-单公司, 1-多公司';
COMMENT ON COLUMN wfm_diagram.start_on_mobile IS '0-不能在移动端启动, 1-可以在移动端启动';
COMMENT ON COLUMN wfm_diagram.process_status IS '1-新增, 2-发布, 3-发布并修改, 4-导入版本';
COMMENT ON COLUMN wfm_diagram.valid IS '0-删除，1-有效';

CREATE TABLE wfm_diagram_content (
	id NUMBER(20,0) NOT NULL,
	published_json CLOB DEFAULT NULL,
  	draft_json CLOB DEFAULT NULL,
  	xml CLOB DEFAULT NULL,
  	creator VARCHAR2(32) DEFAULT '',
  	create_staff_id NUMBER(20, 0) DEFAULT NULL,
  	create_time TIMESTAMP DEFAULT systimestamp,
  	modifier VARCHAR2(32) DEFAULT '',
  	modify_staff_id NUMBER(20, 0) DEFAULT NULL,
  	modify_time TIMESTAMP DEFAULT systimestamp,
  	CONSTRAINT pk_wfm_diagram_content_id PRIMARY KEY (id)
);
COMMENT ON COLUMN wfm_diagram_content.published_json IS '已发布的流程组态数据';
COMMENT ON COLUMN wfm_diagram_content.draft_json IS '流程组态草稿';
COMMENT ON COLUMN wfm_diagram_content.xml IS '流程组态bpmn';

CREATE TABLE wfm_task_form (
	id NUMBER(20,0) NOT NULL,
	process_id VARCHAR2(64) NOT NULL,
	instance_id NUMBER(20,0) NOT NULL,
	user_id NUMBER(20,0) NOT NULL,
	form_data CLOB DEFAULT NULL,
  	form_temp_data CLOB DEFAULT NULL,
	creator VARCHAR2(32) DEFAULT '',
	create_staff_id NUMBER(20, 0) DEFAULT NULL,
  	create_time TIMESTAMP DEFAULT systimestamp,
 	modifier VARCHAR2(32) DEFAULT '',
 	modify_staff_id NUMBER(20, 0) DEFAULT NULL,
  	modify_time TIMESTAMP DEFAULT systimestamp,
  	CONSTRAINT pk_wfm_task_form_id PRIMARY KEY (id)
);
CREATE UNIQUE INDEX idx_wfm_form_ins_id ON wfm_task_form(instance_id, user_id);
COMMENT ON COLUMN wfm_task_form.process_id IS '流程实例ID';
COMMENT ON COLUMN wfm_task_form.instance_id IS '任务实例ID';
COMMENT ON COLUMN wfm_task_form.user_id IS '用户ID';
COMMENT ON COLUMN wfm_task_form.form_data IS '表单数据';
COMMENT ON COLUMN wfm_task_form.form_temp_data IS '表单临时保存数据';

CREATE TABLE wfm_process (
	id NUMBER(20,0) NOT NULL,
	app_id VARCHAR2(64) DEFAULT NULL,
	user_id NUMBER(20,0) NOT NULL,
	staff_name VARCHAR2(64) NOT NULL,
	process_key VARCHAR2(64) DEFAULT NULL,
  	process_version NUMBER(4,0) DEFAULT 0,
  	process_name VARCHAR2(200) NOT NULL,
  	process_status NUMBER(4,0) DEFAULT 88,
    cid NUMBER(20,0) DEFAULT NULL,	
    complete_time TIMESTAMP DEFAULT NULL,
    tenant_id VARCHAR2(64) DEFAULT '',
    creator VARCHAR2(32) DEFAULT NULL,
	create_staff_id NUMBER(20,0) DEFAULT NULL,
	create_time TIMESTAMP DEFAULT systimestamp,
	modifier VARCHAR(32) DEFAULT NULL,
	modify_staff_id NUMBER(20,0) DEFAULT NULL,
	modify_time TIMESTAMP DEFAULT systimestamp,
	CONSTRAINT pk_wfm_process_id PRIMARY KEY (id)
);
CREATE INDEX idx_wfm_process_user_id ON wfm_process (user_id);
CREATE INDEX idx_wfm_process_name ON wfm_process (process_name);
COMMENT ON COLUMN wfm_process.user_id IS '发起者ID';
COMMENT ON COLUMN wfm_process.staff_name IS '发起者人员名称';
COMMENT ON COLUMN wfm_process.process_key IS '流程编号';
COMMENT ON COLUMN wfm_process.process_version IS '流程版本';
COMMENT ON COLUMN wfm_process.process_name IS '流程名称';
COMMENT ON COLUMN wfm_process.process_status IS '88-进行中 77-暂停';
COMMENT ON COLUMN wfm_process.cid IS '公司ID';

CREATE TABLE wfm_process_attention (
	id NUMBER(20,0) NOT NULL,
	app_id VARCHAR2(64) DEFAULT NULL,
	process_id VARCHAR2(64) NOT NULL,
	user_id NUMBER(20,0) NOT NULL,
	initiator_id VARCHAR2(64) DEFAULT NULL,
  	staff_name VARCHAR2(128) DEFAULT NULL,
	tenant_id VARCHAR2(64) DEFAULT '',
    creator VARCHAR2(32) DEFAULT NULL,
	create_staff_id NUMBER(20,0) DEFAULT NULL,
	create_time TIMESTAMP DEFAULT systimestamp,
	modifier VARCHAR(32) DEFAULT NULL,
	modify_staff_id NUMBER(20,0) DEFAULT NULL,
	modify_time TIMESTAMP DEFAULT systimestamp,
	CONSTRAINT pk_attention_id PRIMARY KEY (id)
);
CREATE INDEX idx_attention_user_id ON wfm_process_attention (user_id);

CREATE TABLE wfm_task_pending (
  id NUMBER(20,0) NOT NULL,
  user_id NUMBER(20,0) NOT NULL,
  person_name VARCHAR2(128) DEFAULT NULL,
  app_id VARCHAR2(64) DEFAULT '',
  task_description VARCHAR2(512) DEFAULT NULL,
  activity_type VARCHAR2(32) DEFAULT NULL,
  activity_name VARCHAR2(64) DEFAULT NULL,
  execution_id VARCHAR2(128) DEFAULT NULL,
  task_status NUMBER(4,0) DEFAULT 88,
  open_url VARCHAR2(1024) DEFAULT NULL,
  instance_id VARCHAR2(64) DEFAULT NULL,
  process_key VARCHAR2(64) DEFAULT NULL,
  process_version NUMBER(4,0) DEFAULT 0,
  process_id VARCHAR2(64) DEFAULT NULL,
  process_name VARCHAR2(200) DEFAULT NULL,
  process_description VARCHAR2(512) DEFAULT NULL,
  initiator_id VARCHAR2(64) DEFAULT NULL,
  staff_name VARCHAR2(128) DEFAULT NULL,
  table_info_id NUMBER(20,0) DEFAULT NULL,
  entity_code VARCHAR2(64) DEFAULT NULL,
  table_no VARCHAR2(64) DEFAULT NULL,
  deployment_id NUMBER(20,0) DEFAULT NULL,
  task_type NUMBER(2,0) DEFAULT 0,
  proxy_source NUMBER(20,0) DEFAULT NULL,
  description VARCHAR2(512) DEFAULT NULL,
  loops NUMBER(2,0) DEFAULT NULL,
  task_source VARCHAR2(32) DEFAULT NULL,
  cid NUMBER(20,0) DEFAULT NULL,
  model_id NUMBER(20,0) DEFAULT NULL,
  main_loop NUMBER(1,0) DEFAULT 0,
  attention NUMBER(1,0) DEFAULT 0,
  has_read NUMBER(1,0) DEFAULT 0,
  multi_company NUMBER(1,0) DEFAULT 0,
  source_staff NUMBER(20,0) DEFAULT NULL,
  latest_user VARCHAR2(64) DEFAULT NULL,
  mobile_approve NUMBER(1,0) DEFAULT 1,
  description_zh_cn VARCHAR2(512) DEFAULT NULL,
  task_description_zh_cn VARCHAR2(512) DEFAULT NULL,
  process_description_zh_cn VARCHAR2(512) DEFAULT NULL,
  integration_id VARCHAR2(64),
  row_version NUMBER(11,0) DEFAULT NULL,
  version NUMBER(11,0) DEFAULT NULL,
  tenant_id VARCHAR2(64) DEFAULT '',
  creator VARCHAR2(32) DEFAULT NULL,
  create_staff_id NUMBER(20,0) DEFAULT NULL,
  create_time TIMESTAMP DEFAULT systimestamp,
  modifier VARCHAR2(32) DEFAULT NULL,
  modify_staff_id NUMBER(20,0) DEFAULT NULL,
  modify_time TIMESTAMP DEFAULT systimestamp,
  CONSTRAINT pk_wfm_task_pending_id PRIMARY KEY (id)
);
 CREATE INDEX idx_wfm_task_pending_userid ON wfm_task_pending (user_id);
 CREATE INDEX idx_wfm_task_pending_proceid ON wfm_task_pending (process_id);
 COMMENT ON COLUMN wfm_task_pending.app_id IS 'App ID';
 COMMENT ON COLUMN wfm_task_pending.task_description IS '活动名称-国际化编号';
 COMMENT ON COLUMN wfm_task_pending.activity_type IS '活动类型';
 COMMENT ON COLUMN wfm_task_pending.activity_name IS '活动ID';
 COMMENT ON COLUMN wfm_task_pending.execution_id IS '活动实例ID';
 COMMENT ON COLUMN wfm_task_pending.instance_id IS 'bap业务ID';
 COMMENT ON COLUMN wfm_task_pending.process_id IS '流程实例ID';
 COMMENT ON COLUMN wfm_task_pending.process_key IS '流程编号';
 COMMENT ON COLUMN wfm_task_pending.process_version IS '流程版本';
 COMMENT ON COLUMN wfm_task_pending.process_name IS '流程名称';
 COMMENT ON COLUMN wfm_task_pending.process_description IS '流程定义描述';
 COMMENT ON COLUMN wfm_task_pending.cid IS '公司ID';
 COMMENT ON COLUMN wfm_task_pending.table_info_id IS 'bap字段';
 COMMENT ON COLUMN wfm_task_pending.entity_code IS 'bap字段';
 COMMENT ON COLUMN wfm_task_pending.deployment_id IS '流程部署ID';
 COMMENT ON COLUMN wfm_task_pending.proxy_source IS '最近一次委托者';
 COMMENT ON COLUMN wfm_task_pending.table_no IS '单据编号';
 COMMENT ON COLUMN wfm_task_pending.open_url IS '关联页面url';
 COMMENT ON COLUMN wfm_task_pending.initiator_id IS '流程发起者';
 COMMENT ON COLUMN wfm_task_pending.staff_name IS '发起者人员名称';
 COMMENT ON COLUMN wfm_task_pending.description IS '委托原因';
 COMMENT ON COLUMN wfm_task_pending.loops IS '循环会签 0不是循环会签,1表示本公司，2表示夸公司，3表示本部门,4表示本部门及下级，5自定义';
 COMMENT ON COLUMN wfm_task_pending.task_source IS '数据来源 bap, supOS';
 COMMENT ON COLUMN wfm_task_pending.model_id IS 'bap字段';
 COMMENT ON COLUMN wfm_task_pending.main_loop IS '是否是主办人';
 COMMENT ON COLUMN wfm_task_pending.attention IS '是否关注';
 COMMENT ON COLUMN wfm_task_pending.has_read IS '是否只读 0-未读, 1-已读';
 COMMENT ON COLUMN wfm_task_pending.source_staff IS '委托的原始用户';
 COMMENT ON COLUMN wfm_task_pending.task_status IS '88-进行中, 77-挂起';
 COMMENT ON COLUMN wfm_task_pending.task_type IS '0-普通待办 2-委托待办';
 COMMENT ON COLUMN wfm_task_pending.mobile_approve IS '启动移动客户端审批';
 COMMENT ON COLUMN wfm_task_pending.task_source IS '待办来源';
 COMMENT ON COLUMN wfm_task_pending.latest_user IS '上个环节提交者人员名称';
 COMMENT ON COLUMN wfm_task_pending.integration_id IS '外部系统集成ID';
 COMMENT ON COLUMN wfm_task_pending.version IS 'bap乐观锁';
 
 CREATE TABLE wfm_task_complete (
  id NUMBER(20,0) NOT NULL,
  app_id VARCHAR2(64) DEFAULT NULL,
  cid NUMBER(20,0) DEFAULT 0,
  user_id NUMBER(20,0) NOT NULL,
  person_name VARCHAR2(128) DEFAULT NULL,
  task_name VARCHAR2(512) DEFAULT NULL,
  activity_name VARCHAR2(64) DEFAULT NULL,
  open_url VARCHAR2(1024) DEFAULT NULL,
  instance_id VARCHAR2(64) DEFAULT NULL,
  process_key VARCHAR2(64) DEFAULT '',
  process_version NUMBER(4,0) DEFAULT 0,
  process_id VARCHAR2(64) DEFAULT NULL,
  process_name VARCHAR2(64) DEFAULT NULL,
  initiator_id VARCHAR2(64) DEFAULT NULL,
  staff_name VARCHAR2(64) DEFAULT NULL,
  start_time TIMESTAMP DEFAULT NULL,
  proxy_source NUMBER(20,0) DEFAULT NULL,
  source_staff NUMBER(20,0) DEFAULT NULL,
  table_no VARCHAR2(64) DEFAULT NULL,
  task_type NUMBER(2,0) DEFAULT 0,
  task_source VARCHAR2(32) DEFAULT NULL,
  latest_user VARCHAR2(64) DEFAULT NULL,
  integration_id VARCHAR2(64),
  reject NUMBER(1,0) DEFAULT 0,
  tenant_id VARCHAR2(64) DEFAULT '',
  row_version NUMBER(11,0) DEFAULT NULL,
  creator VARCHAR2(32) DEFAULT NULL,
  create_staff_id NUMBER(20,0) DEFAULT NULL,
  create_time TIMESTAMP DEFAULT systimestamp,
  modifier VARCHAR2(32) DEFAULT NULL,
  modify_staff_id NUMBER(20,0) DEFAULT NULL,
  modify_time TIMESTAMP DEFAULT systimestamp,
  CONSTRAINT pk_wfm_task_complete_id PRIMARY KEY (id)
 );
 CREATE INDEX idx_wfm_task_complete_userid ON wfm_task_complete (user_id);
 COMMENT ON COLUMN wfm_task_complete.app_id IS 'App ID';
 COMMENT ON COLUMN wfm_task_complete.person_name IS '人员名称';
 COMMENT ON COLUMN wfm_task_complete.task_name IS '活动名称';
 COMMENT ON COLUMN wfm_task_complete.activity_name IS '活动节点ID';
 COMMENT ON COLUMN wfm_task_complete.instance_id IS 'bap业务ID';
 COMMENT ON COLUMN wfm_task_complete.process_id IS '流程实例ID';
 COMMENT ON COLUMN wfm_task_complete.process_key IS '流程编号';
 COMMENT ON COLUMN wfm_task_complete.process_version IS '流程版本';
 COMMENT ON COLUMN wfm_task_complete.process_name IS '流程名称';
 COMMENT ON COLUMN wfm_task_complete.cid IS '公司ID';
 COMMENT ON COLUMN wfm_task_complete.table_no IS '单据编号';
 COMMENT ON COLUMN wfm_task_complete.open_url IS '关联页面url';
 COMMENT ON COLUMN wfm_task_complete.initiator_id IS '流程发起者';
 COMMENT ON COLUMN wfm_task_complete.staff_name IS '流程发起者人员名称';
 COMMENT ON COLUMN wfm_task_complete.task_source IS '数据来源 bap, supOS';
 COMMENT ON COLUMN wfm_task_complete.task_type IS '0-普通待办 2-委托待办';
 COMMENT ON COLUMN wfm_task_complete.task_source IS '待办来源';
 COMMENT ON COLUMN wfm_task_complete.latest_user IS '上个环节提交者';
 COMMENT ON COLUMN wfm_task_complete.integration_id IS '外部系统集成ID';
 COMMENT ON COLUMN wfm_task_complete.reject IS '是否驳回 0-no, 1-yes';
 
 CREATE TABLE wfm_process_log (
  id NUMBER(20,0) NOT NULL,
  process_id VARCHAR2(64) NOT NULL,
  process_name VARCHAR2(200),
  task_id NUMBER(20,0) DEFAULT NULL,
  task_name VARCHAR2(128) DEFAULT '',
  audit_result VARCHAR2(32) DEFAULT NULL,
  leave_comment VARCHAR2(1000) DEFAULT NULL,
  action_desc VARCHAR2(256) DEFAULT NULL,
  action_type VARCHAR2(32) DEFAULT NULL,
  tenant_id VARCHAR2(64) DEFAULT '',
  creator VARCHAR2(32) DEFAULT '',
  create_staff_id NUMBER(20, 0) DEFAULT NULL,
  create_time TIMESTAMP DEFAULT systimestamp,
  modifier VARCHAR2(32) DEFAULT '',
  modify_staff_id NUMBER(20, 0) DEFAULT NULL,
  modify_time TIMESTAMP DEFAULT systimestamp,
  CONSTRAINT pk_wfm_process_log_id PRIMARY KEY (id)
);
CREATE INDEX idx_wfm_log_process_id ON wfm_process_log (process_id);
COMMENT ON COLUMN wfm_process_log.audit_result IS '审批结果: 同意OR驳回';
COMMENT ON COLUMN wfm_process_log.leave_comment IS '用户提交的备注';
COMMENT ON COLUMN wfm_process_log.action_desc IS '操作描述, 格式: I18N_MESSAGE_CODE#参数1#参数2';
COMMENT ON COLUMN wfm_process_log.action_type IS '操作类型';

CREATE TABLE wfm_entrust (
  id NUMBER(20,0) NOT NULL,
  cid NUMBER(20,0) NOT NULL,
  app_id VARCHAR2(64) DEFAULT '',
  principal NUMBER(20,0) DEFAULT 0,
  mandatary NUMBER(20,0) DEFAULT 0,
  mandatary_name VARCHAR2(64) DEFAULT '',
  process_id VARCHAR2(64) DEFAULT '',
  process_name VARCHAR2(200) DEFAULT '',
  task_id NUMBER(20,0) NOT NULL,
  task_name VARCHAR2(512) DEFAULT '',
  description VARCHAR2(512) DEFAULT NULL,
  tenant_id VARCHAR2(64) DEFAULT '',
  creator VARCHAR2(32) DEFAULT '',
  create_staff_id NUMBER(20, 0) DEFAULT NULL,
  create_time TIMESTAMP DEFAULT systimestamp,
  modifier VARCHAR2(32) DEFAULT '',
  modify_staff_id NUMBER(20, 0) DEFAULT NULL,
  modify_time TIMESTAMP DEFAULT systimestamp,
  CONSTRAINT pk_wfm_entrust_id PRIMARY KEY (id)
);
CREATE INDEX idx_wfm_entrust_principal ON wfm_entrust (principal);
COMMENT ON COLUMN wfm_entrust.cid IS '公司ID';
COMMENT ON COLUMN wfm_entrust.principal IS '委托方';
COMMENT ON COLUMN wfm_entrust.mandatary IS '被委托方';
COMMENT ON COLUMN wfm_entrust.description IS '委托原因';


alter table wfm_task_pending add start_time TIMESTAMP DEFAULT systimestamp;
alter table wfm_task_complete add end_time TIMESTAMP DEFAULT systimestamp;
alter table wfm_process_attention add table_no VARCHAR2(64);
alter table wfm_process add table_no VARCHAR2(64);
alter table wfm_entrust add table_no VARCHAR2(64);
alter table wfm_entrust add instance_id VARCHAR2(64);
alter table wfm_task_complete add showlog NUMBER(2, 0) DEFAULT 1;
CREATE INDEX idx_task_pending_instanceid ON wfm_task_pending (instance_id);
## 1.0.3
alter table wfm_task_pending add person_name VARCHAR2(128) DEFAULT NULL;
alter table wfm_task_pending add attention NUMBER(2, 0) DEFAULT 0;
alter table wfm_task_pending add has_read NUMBER(2, 0) DEFAULT 0;
alter table wfm_task_pending add latest_user VARCHAR2(64) DEFAULT NULL;
alter table wfm_task_pending add due_time TIMESTAMP DEFAULT NULL;
## 1.0.4
alter table wfm_task_pending MODIFY process_id NULL;
COMMENT ON COLUMN wfm_task_pending.process_id IS '流程实例ID';
update wfm_task_pending set tenant_id='dt' where tenant_id IS NULL or tenant_id='';
## 1.0.5
alter table wfm_task_form modify instance_id VARCHAR2(64) NULL;