## 1.0.2
CREATE TABLE IF NOT EXISTS wfm_diagram (
  id BIGINT(20) NOT NULL,
  cid BIGINT(20) NOT NULL COMMENT '公司ID',
  app_id VARCHAR(64) DEFAULT '',
  process_key VARCHAR(64) NOT NULL,
  process_name VARCHAR(200) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  publish_time TIMESTAMP DEFAULT 0 COMMENT '发布时间',
  latest_modify_time TIMESTAMP DEFAULT 0 COMMENT '最近一次修改组态数据',
  creator_staff VARCHAR(128) DEFAULT '' COMMENT '创建者人员名称',
  publisher VARCHAR(128) DEFAULT '' COMMENT '发布者人员名称',
  start_on_mobile SMALLINT(1) DEFAULT 1 COMMENT '移动端启动',
  version TINYINT(4) DEFAULT 0 COMMENT '版本号',
  content_id BIGINT(20) NOT NULL,
  enabled TINYINT(1) DEFAULT 0 COMMENT '0-未启用, 1-已启用',
  multi_company TINYINT(1) DEFAULT 0 COMMENT '0-单公司, 1-多公司',
  process_status TINYINT(2) DEFAULT 1 COMMENT '1-新增, 2-发布, 3-发布并修改, 4-导入版本',
  valid TINYINT(1) DEFAULT 1 COMMENT '0-删除，1-有效',
  tenant_id VARCHAR(64) DEFAULT '',
  creator VARCHAR(32) DEFAULT '',
  create_staff_id BIGINT(20) DEFAULT NULL,
  create_time TIMESTAMP DEFAULT current_timestamp(),
  modifier VARCHAR(32) DEFAULT '',
  modify_staff_id BIGINT(20) DEFAULT NULL,
  modify_time TIMESTAMP DEFAULT current_timestamp(),
  PRIMARY KEY (id),
  KEY idx_wfm_diagram_app_id (app_id),
  KEY idx_wfm_diagram_key_version (process_key, version)
) DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS wfm_diagram_content (
	id BIGINT(20) NOT NULL,
	published_json MEDIUMTEXT DEFAULT NULL COMMENT '已发布的流程组态数据',
  	draft_json MEDIUMTEXT DEFAULT NULL COMMENT '流程组态草稿',
  	xml MEDIUMTEXT DEFAULT NULL COMMENT '流程组态bpmn',
  	creator VARCHAR(32) DEFAULT '',
  	create_staff_id BIGINT(20) DEFAULT NULL,
  	create_time TIMESTAMP DEFAULT current_timestamp(),
  	modifier VARCHAR(32) DEFAULT '',
  	modify_staff_id BIGINT(20) DEFAULT NULL,
  	modify_time TIMESTAMP,
  	PRIMARY KEY (id)
) DEFAULT CHARSET=utf8;

 CREATE TABLE IF NOT EXISTS wfm_process (
	id BIGINT(20) NOT NULL,
	app_id VARCHAR(64) DEFAULT NULL COMMENT 'app id',
	user_id BIGINT(20) NOT NULL COMMENT '发起者ID',
	staff_name VARCHAR(64) DEFAULT NULL COMMENT '发起者人员名称',
	process_key VARCHAR(64) DEFAULT NULL COMMENT '流程编号',
  	process_version INT(4) DEFAULT 0 COMMENT '流程版本',
  	process_name VARCHAR(200) NOT NULL COMMENT '流程名称',
  	process_status TINYINT(4) NOT NULL DEFAULT 88 COMMENT '88-进行中 77-暂停 99-作废 66-已完成',
    cid BIGINT(20) DEFAULT NULL COMMENT '公司ID',
    complete_time TIMESTAMP DEFAULT 0 COMMENT '流程结束时间',
    tenant_id VARCHAR(64) DEFAULT '',
    creator VARCHAR(32) DEFAULT NULL,
	create_staff_id BIGINT(20) DEFAULT NULL,
	create_time TIMESTAMP DEFAULT current_timestamp(),
	modifier VARCHAR(32) DEFAULT NULL,
	modify_staff_id BIGINT(20) DEFAULT NULL,
	modify_time TIMESTAMP DEFAULT current_timestamp(),
	PRIMARY KEY (id),
	KEY idx_wfm_process_user_id (user_id),
	KEY idx_wfm_process_name (process_name)
 ) DEFAULT CHARSET=utf8;
 
 CREATE TABLE IF NOT EXISTS wfm_process_attention (
 	id BIGINT(20) NOT NULL,
 	app_id VARCHAR(64) DEFAULT NULL COMMENT 'app id',
 	process_id VARCHAR(64) NOT NULL COMMENT '流程实例ID',
 	user_id BIGINT(20) NOT NULL COMMENT '关注者ID',
 	initiator_id VARCHAR(64) DEFAULT NULL COMMENT '流程发起者',
  	staff_name VARCHAR(128) DEFAULT NULL COMMENT '流程发起者人员名称',
 	tenant_id VARCHAR(64) DEFAULT '',
    creator VARCHAR(32) DEFAULT NULL,
	create_staff_id BIGINT(20) DEFAULT NULL,
	create_time TIMESTAMP DEFAULT current_timestamp(),
	modifier VARCHAR(32) DEFAULT NULL,
	modify_staff_id BIGINT(20) DEFAULT NULL,
	modify_time TIMESTAMP DEFAULT current_timestamp(),
	PRIMARY KEY (id),
	KEY idx_attention_user_id (user_id)
 ) DEFAULT CHARSET=utf8;
 
CREATE TABLE IF NOT EXISTS wfm_task_form (
	id BIGINT(20) NOT NULL,
	process_id VARCHAR(64) NOT NULL COMMENT '流程实例ID',
	instance_id BIGINT(20) NOT NULL COMMENT '任务实例ID',
	user_id BIGINT(20) NOT NULL COMMENT '用户ID',
	form_data MEDIUMTEXT DEFAULT NULL COMMENT '表单数据',
  	form_temp_data MEDIUMTEXT DEFAULT NULL COMMENT '表单临时保存数据',
  	tenant_id VARCHAR(64) DEFAULT '',
	creator VARCHAR(32) DEFAULT '',
	create_staff_id BIGINT(20) DEFAULT NULL,
  	create_time TIMESTAMP DEFAULT current_timestamp(),
 	modifier VARCHAR(32) DEFAULT '',
 	modify_staff_id BIGINT(20) DEFAULT NULL,
  	modify_time TIMESTAMP,
  	PRIMARY KEY (id),
  	UNIQUE KEY idx_wfm_form_ins_id (instance_id, user_id)
) DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS wfm_task_pending (
  id BIGINT(20) NOT NULL,
  user_id BIGINT(20) NOT NULL COMMENT '用户ID',
  person_name VARCHAR(128) DEFAULT NULL COMMENT '人员名',
  app_id VARCHAR(64) DEFAULT NULL COMMENT 'app id',
  task_description VARCHAR(512) DEFAULT NULL COMMENT '活动名称-国际化编号',
  activity_type VARCHAR(32) DEFAULT NULL COMMENT '活动类型',
  activity_name VARCHAR(64) DEFAULT NULL COMMENT '活动ID',
  execution_id VARCHAR(128) DEFAULT NULL COMMENT '活动实例ID',
  task_status TINYINT(4) NOT NULL DEFAULT 88 COMMENT '88-进行中 77-暂停',
  open_url VARCHAR(1024) DEFAULT NULL COMMENT '待办关联url',
  instance_id VARCHAR(64) DEFAULT NULL COMMENT 'bap业务ID',
  process_key VARCHAR(64) DEFAULT NULL COMMENT '流程编号',
  process_version INT(4) DEFAULT 0 COMMENT '流程版本',
  process_name VARCHAR(200) DEFAULT NULL COMMENT '流程名称',
  process_description VARCHAR(512) DEFAULT NULL COMMENT '流程定义描述',
  initiator_id VARCHAR(64) DEFAULT NULL COMMENT '流程发起者',
  staff_name VARCHAR(128) DEFAULT NULL COMMENT '流程发起者人员名称',
  process_id VARCHAR(64) DEFAULT NULL COMMENT '流程实例ID',
  table_info_id BIGINT(20) DEFAULT NULL COMMENT 'bap字段',
  entity_code VARCHAR(64) DEFAULT NULL COMMENT 'bap字段',
  table_no VARCHAR(64) DEFAULT NULL COMMENT '单据编号',
  deployment_id BIGINT(20) DEFAULT 0 COMMENT '流程部署ID',
  task_type TINYINT(2) DEFAULT 0 COMMENT '待办类型 0-普通待办 2-委托待办 4-编辑待发',
  proxy_source BIGINT(20) DEFAULT NULL COMMENT '最近一次委托者',
  description VARCHAR(512) DEFAULT NULL COMMENT '委托原因',
  loops TINYINT(2) DEFAULT NULL COMMENT '循环会签 0不是循环会签,1表示本公司，2表示夸公司，3表示本部门,4表示本部门及下级，5自定义',
  task_source VARCHAR(32) DEFAULT NULL COMMENT '数据来源 bap, supOS',
  cid BIGINT(20) DEFAULT NULL COMMENT '公司ID',
  model_id BIGINT(20) DEFAULT NULL COMMENT 'bap字段',
  main_loop SMALLINT(1) DEFAULT 0 COMMENT '是否是主办人',
  attention SMALLINT(1) DEFAULT 0 COMMENT '是否关注',
  has_read SMALLINT(1) DEFAULT 0 COMMENT '是否已读 0-未读, 1-已读',
  multi_company SMALLINT(1) DEFAULT 0 COMMENT '是否跨公司',
  source_staff BIGINT(20) DEFAULT 0 COMMENT '委托的原始用户',
  latest_user VARCHAR(64) DEFAULT NULL COMMENT '上个环节提交者人员名称',
  mobile_approve SMALLINT(1) DEFAULT 1 COMMENT '启动移动客户端审批',
  description_zh_cn VARCHAR(512) DEFAULT NULL,
  task_description_zh_cn VARCHAR(512) DEFAULT NULL,
  process_description_zh_cn VARCHAR(512) DEFAULT NULL,
  integration_id VARCHAR(64) DEFAULT NULL,
  row_version INT(11) DEFAULT NULL COMMENT '乐观锁',
  version INT(11) DEFAULT NULL COMMENT 'bap专用乐观锁',
  tenant_id VARCHAR(64) DEFAULT '',
  creator VARCHAR(32) DEFAULT NULL,
  create_staff_id BIGINT(20) DEFAULT NULL,
  create_time TIMESTAMP DEFAULT current_timestamp(),
  modifier VARCHAR(32) DEFAULT NULL,
  modify_staff_id BIGINT(20) DEFAULT NULL,
  modify_time TIMESTAMP DEFAULT current_timestamp(),
  PRIMARY KEY (id),
  KEY idx_wfm_task_pending_user_id (user_id),
  KEY idx_wfm_task_pending_process_id (process_id)
) DEFAULT CHARSET=utf8;
 CREATE TABLE IF NOT EXISTS wfm_task_complete (
  id BIGINT(20) NOT NULL,
  app_id VARCHAR(64) DEFAULT NULL COMMENT 'app id',
  cid BIGINT(20) DEFAULT NULL COMMENT '公司ID',
  user_id BIGINT(20) NOT NULL COMMENT '用户ID',
  person_name VARCHAR(128) DEFAULT NULL COMMENT '人员名',
  task_name VARCHAR(512) DEFAULT NULL COMMENT '活动名称',
  activity_name VARCHAR(64) DEFAULT NULL COMMENT '活动节点ID',
  open_url VARCHAR(1024) DEFAULT NULL COMMENT '待办关联url',
  instance_id VARCHAR(64) DEFAULT NULL COMMENT '待办实例ID',
  process_key VARCHAR(64) DEFAULT NULL COMMENT '流程编号',
  process_version INT(4) DEFAULT 0 COMMENT '流程版本',
  process_id VARCHAR(64) DEFAULT NULL COMMENT '流程实例ID',
  process_name VARCHAR(128) DEFAULT NULL COMMENT '流程名称',
  initiator_id VARCHAR(64) DEFAULT NULL COMMENT '流程发起者',
  staff_name VARCHAR(64) DEFAULT NULL COMMENT '流程发起者名称',
  start_time TIMESTAMP DEFAULT 0,
  proxy_source BIGINT(20) DEFAULT NULL COMMENT '最近一次委托者',
  source_staff BIGINT(20) DEFAULT 0 COMMENT '委托的原始用户',
  table_no VARCHAR(64) DEFAULT NULL COMMENT '单据编号',
  task_type TINYINT(2) DEFAULT 0 COMMENT '待办类型 0-普通待办 2-委托待办',
  task_source VARCHAR(32) DEFAULT NULL COMMENT '数据来源 bap, supOS',
  latest_user VARCHAR(64) DEFAULT NULL COMMENT '上个环节提交者人员名称',
  reject SMALLINT(1) DEFAULT 0 COMMENT '是否驳回 0-no, 1-yes',
  integration_id VARCHAR(64) DEFAULT NULL,
  tenant_id VARCHAR(64) DEFAULT '',
  row_version INT(11) DEFAULT NULL COMMENT '乐观锁',
  creator VARCHAR(32) DEFAULT NULL,
  create_staff_id BIGINT(20) DEFAULT NULL,
  create_time TIMESTAMP DEFAULT current_timestamp(),
  modifier VARCHAR(32) DEFAULT NULL,
  modify_staff_id BIGINT(20) DEFAULT NULL,
  modify_time TIMESTAMP DEFAULT current_timestamp(),
  PRIMARY KEY (id),
  KEY idx_wfm_task_complete_user_id (user_id)
 ) DEFAULT CHARSET=utf8;
 
 CREATE TABLE IF NOT EXISTS wfm_process_log (
  id BIGINT(20) NOT NULL,
  process_id VARCHAR(64) NOT NULL,
  process_name VARCHAR(200),
  task_id BIGINT(20) DEFAULT NULL,
  task_name VARCHAR(128) DEFAULT '',
  audit_result VARCHAR(32) DEFAULT NULL COMMENT '审批结果: 同意OR驳回',
  leave_comment VARCHAR(1000) DEFAULT NULL COMMENT '用户提交的备注',
  action_desc VARCHAR(256) DEFAULT NULL COMMENT '操作描述, 格式: I18N_MESSAGE_CODE#参数1#参数2',
  action_type VARCHAR(32) DEFAULT NULL COMMENT '操作类型',
  tenant_id VARCHAR(64) DEFAULT '',
  creator VARCHAR(32) DEFAULT '',
  create_staff_id BIGINT(20) DEFAULT NULL,
  create_time TIMESTAMP DEFAULT current_timestamp(),
  modifier VARCHAR(32) DEFAULT '',
  modify_staff_id BIGINT(20) DEFAULT NULL,
  modify_time TIMESTAMP DEFAULT current_timestamp(),
  PRIMARY KEY (id),
  KEY idx_wfm_log_process_id (process_id)
) DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS wfm_entrust (
  id BIGINT(20) NOT NULL,
  cid BIGINT(20) NOT NULL COMMENT '公司ID',
  app_id VARCHAR(64) DEFAULT '',
  principal BIGINT(20) DEFAULT 0 COMMENT '委托方',
  mandatary BIGINT(20) DEFAULT 0 COMMENT '被委托方',
  mandatary_name VARCHAR(64) DEFAULT '' COMMENT '被委托方名称',
  process_id VARCHAR(64) DEFAULT '',
  process_name VARCHAR(200) DEFAULT NULL,
  task_id BIGINT(20) NOT NULL,
  task_name VARCHAR(128) DEFAULT NULL,
  description VARCHAR(512) DEFAULT NULL COMMENT '委托原因',
  tenant_id VARCHAR(64) DEFAULT NULL,
  creator VARCHAR(32) DEFAULT '',
  create_staff_id BIGINT(20) DEFAULT NULL,
  create_time TIMESTAMP DEFAULT current_timestamp(),
  modifier VARCHAR(32) DEFAULT '',
  modify_staff_id BIGINT(20) DEFAULT NULL,
  modify_time TIMESTAMP DEFAULT current_timestamp(),
  PRIMARY KEY (id),
  KEY idx_wfm_entrust_principal (principal)
) DEFAULT CHARSET=utf8;

alter table wfm_task_pending add index idx_task_pending_userid(user_id);
alter table wfm_task_pending add index idx_task_pending_processid(process_id);
alter table wfm_task_pending add column start_time TIMESTAMP DEFAULT current_timestamp() COMMENT '待办接收时间';
alter table wfm_task_complete add column end_time TIMESTAMP DEFAULT current_timestamp() COMMENT '待办结束时间';
alter table wfm_process_attention add column table_no VARCHAR(64) DEFAULT NULL COMMENT '单据编号';
alter table wfm_process add column table_no VARCHAR(64) DEFAULT NULL COMMENT '单据编号';
alter table wfm_entrust add column table_no VARCHAR(64) DEFAULT NULL COMMENT '单据编号';
alter table wfm_entrust add column instance_id VARCHAR(64) DEFAULT NULL COMMENT '待办实例ID';
alter table wfm_task_complete add column showlog SMALLINT(1) DEFAULT 1 COMMENT '是否显示流程日志'; 
alter table wfm_task_pending add index idx_task_pending_instanceid(instance_id);
## 1.0.3
alter table wfm_task_pending add column person_name VARCHAR(128) DEFAULT NULL COMMENT '人员名';
alter table wfm_task_pending add column attention SMALLINT(1) DEFAULT 0 COMMENT '是否关注';
alter table wfm_task_pending add column has_read SMALLINT(1) DEFAULT 0 COMMENT '是否已读 0-未读, 1-已读';
alter table wfm_task_pending add column latest_user VARCHAR(64) DEFAULT NULL COMMENT '上个环节提交者人员名称';
alter table wfm_task_pending add column due_time TIMESTAMP DEFAULT 0 COMMENT '过期时间';
## 1.0.4
alter table wfm_task_pending modify column process_id VARCHAR(64) NULL COMMENT '流程实例ID';
update wfm_task_pending set tenant_id='dt' where tenant_id IS NULL or tenant_id='';
## 1.0.5
alter table wfm_task_form modify column instance_id VARCHAR(64) NULL;