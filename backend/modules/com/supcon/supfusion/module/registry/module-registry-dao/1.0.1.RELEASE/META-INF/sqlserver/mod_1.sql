## 1.0.0
CREATE TABLE mod_module_registry (
  id BIGINT NOT NULL PRIMARY KEY,
  module_id VARCHAR(64) NOT NULL,
  module_code VARCHAR(128) DEFAULT NULL,
  module_name VARCHAR(64) NOT NULL,
  module_type VARCHAR(24) DEFAULT 'SYSTEM',
  creator VARCHAR(64) DEFAULT '',
  create_time DATETIME DEFAULT getutcdate(),
  create_staff_id BIGINT DEFAULT NULL,
  modifier VARCHAR(64) DEFAULT '',
  modify_time DATETIME,
  modify_staff_id BIGINT DEFAULT NULL
);
CREATE UNIQUE INDEX udx_mod_id ON mod_module_registry(module_id);
CREATE TABLE mod_module_app_rel (
  id BIGINT NOT NULL PRIMARY KEY,
  app_id VARCHAR(64) NOT NULL,
  module_id VARCHAR(64) NOT NULL,
  creator VARCHAR(64) DEFAULT '',
  create_time DATETIME DEFAULT getutcdate(),
  create_staff_id BIGINT DEFAULT NULL,
  modifier VARCHAR(64) DEFAULT '',
  modify_time DATETIME,
  modify_staff_id BIGINT DEFAULT NULL
);
CREATE INDEX idx_module_app_rel_appid ON mod_module_app_rel(app_id);
insert into mod_module_registry(id, module_id, module_name, module_type) values (10000, 'authentication', 'reg.moduleName.authentication', 'SYSTEM');
insert into mod_module_registry(id, module_id, module_name, module_type) values (10001, 'notificationEmail', 'reg.moduleName.notificationEmail', 'SYSTEM');
insert into mod_module_registry(id, module_id, module_name, module_type) values (10002, 'appConfig', 'reg.moduleName.appConfig', 'SYSTEM');
insert into mod_module_registry(id, module_id, module_name, module_type) values (10003, 'notificationDingtalk', 'reg.moduleName.notificationDingtalk', 'SYSTEM');
insert into mod_module_registry(id, module_id, module_name, module_type) values (10004, 'systemConfig', 'reg.moduleName.systemConfig', 'SYSTEM');
insert into mod_module_registry(id, module_id, module_name, module_type) values (10005, 'notificationWechat', 'reg.moduleName.notificationWechat', 'SYSTEM');
insert into mod_module_registry(id, module_id, module_name, module_type) values (10006, 'reg', 'reg.moduleName.reg', 'SYSTEM');
insert into mod_module_registry(id, module_id, module_name, module_type) values (10007, 'sys', 'reg.moduleName.sys', 'SYSTEM');
insert into mod_module_registry(id, module_id, module_name, module_type) values (10008, 'i18n', 'reg.moduleName.i18n', 'SYSTEM');
insert into mod_module_registry(id, module_id, module_name, module_type) values (10009, 'notificationStationletter', 'reg.moduleName.notificationStationletter', 'SYSTEM');
insert into mod_module_registry(id, module_id, module_name, module_type) values (10010, 'notificationApiServer', 'reg.moduleName.notificationApiServer', 'SYSTEM');
insert into mod_module_registry(id, module_id, module_name, module_type) values (10011, 'workflow', 'reg.moduleName.workflow', 'SYSTEM');
insert into mod_module_registry(id, module_id, module_name, module_type) values (10012, 'organization', 'reg.moduleName.organization', 'SYSTEM');
insert into mod_module_registry(id, module_id, module_name, module_type) values (10013, 'theme', 'reg.moduleName.theme', 'SYSTEM');
insert into mod_module_registry(id, module_id, module_name, module_type) values (10014, 'userManagement', 'reg.moduleName.userManagement', 'SYSTEM');
insert into mod_module_registry(id, module_id, module_name, module_type) values (10015, 'composeManage', 'reg.moduleName.composeManage', 'SYSTEM');
insert into mod_module_registry(id, module_id, module_name, module_type) values (10016, 'notificationAdmin', 'reg.moduleName.notificationAdmin', 'SYSTEM');
insert into mod_module_registry(id, module_id, module_name, module_type) values (10017, 'notificationEngine', 'reg.moduleName.notificationEngine', 'SYSTEM');
insert into mod_module_registry(id, module_id, module_name, module_type) values (10018, 'rbac', 'reg.moduleName.rbac', 'SYSTEM');
insert into mod_module_registry(id, module_id, module_name, module_type) values (10019, 'systemCode', 'reg.moduleName.systemCode', 'SYSTEM');
insert into mod_module_registry(id, module_id, module_name, module_type) values (10020, 'appManager', 'reg.moduleName.appManager', 'SYSTEM');
insert into mod_module_registry(id, module_id, module_name, module_type) values (10021, 'signature', 'reg.moduleName.signature', 'SYSTEM');
insert into mod_module_registry(id, module_id, module_name, module_type) values (10022, 'portal', 'reg.moduleName.portal', 'SYSTEM');
insert into mod_module_registry(id, module_id, module_name, module_type) values (10023, 'fileServer', 'reg.moduleName.fileServer', 'SYSTEM');
insert into mod_module_registry(id, module_id, module_name, module_type) values (10024, 'taskScheduler', 'reg.moduleName.taskScheduler', 'SYSTEM');
insert into mod_module_registry(id, module_id, module_name, module_type) values (10025, 'counter', 'reg.moduleName.counter', 'SYSTEM');
insert into mod_module_registry(id, module_id, module_name, module_type) values (10026, 'sysbase', 'reg.moduleName.sysbase', 'SYSTEM');
insert into mod_module_registry(id, module_id, module_name, module_type) values (10027, 'printer', 'reg.moduleName.printer', 'SYSTEM');
## 1.0.1
insert into mod_module_registry(id, module_id, module_name, module_type) values (10028, 'auditlog', 'reg.moduleName.auditlog', 'SYSTEM');
insert into mod_module_registry(id, module_id, module_name, module_type) values (10029, 'notificationMobile', 'reg.moduleName.notificationMobile', 'SYSTEM');
insert into mod_module_registry(id, module_id, module_name, module_type) values (10030, 'notificationSupplant', 'reg.moduleName.notificationSupplant', 'SYSTEM');
insert into mod_module_registry(id, module_id, module_name, module_type) values (10031, 'notificationSMS', 'reg.moduleName.notificationSMS', 'SYSTEM');