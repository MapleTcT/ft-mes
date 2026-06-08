## 1.0.0
CREATE TABLE supfusion_i18n_excel (
   id  bigint(20) NOT NULL COMMENT '主键ID',
   status  int(11) NOT NULL COMMENT '导入/导出状态,1进行中, 2成功, 3失败',
   file_name  varchar(255) NOT NULL COMMENT '导入/导出文件名',
   error_file  varchar(255) DEFAULT NULL COMMENT '生成的错误文件名',
   error_message  varchar(255) DEFAULT NULL COMMENT '错误消息',
   operate_type  varchar(100) NOT NULL COMMENT '类型,import 或 export',
   error_num  int(11) DEFAULT 0 COMMENT '错误行数',
   add_num  int(11) DEFAULT 0 COMMENT '新增行数',
   update_num  int(11) DEFAULT 0 COMMENT '更新行数',
   all_num  int(11) DEFAULT 0 COMMENT '总行数',
   valid  tinyint(1) DEFAULT 1 COMMENT '逻辑删除,是否有效0:无效 1:有效',
   creator  varchar(32) DEFAULT 'system' COMMENT '创建者',
   create_time  timestamp DEFAULT current_timestamp COMMENT '创建时间',
   create_staff_id  bigint(20)  default 0 COMMENT '创建者人员ID',
   modifier  varchar(32)  NULL COMMENT '修改者',
   modify_time  timestamp  NULL COMMENT '修改时间',
   modify_staff_id  bigint(20)  NULL COMMENT '修改者人员ID',
   PRIMARY KEY ( id )
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='execl导入导出状态实体类';
-- supfusion_i18n.supfusion_i18n_index definition

CREATE TABLE  supfusion_i18n_index  (
   id  bigint(20) NOT NULL COMMENT '主键ID',
   module_code  varchar(256) NOT NULL COMMENT '模块名code',
   module_index_code  varchar(256) NOT NULL COMMENT '国际化资源索引',
   valid  tinyint(1) DEFAULT 1 COMMENT '逻辑删除,是否有效0:无效 1:有效',
   creator  varchar(32) DEFAULT 'system' COMMENT '创建者',
   create_time  timestamp DEFAULT current_timestamp COMMENT '创建时间',
   create_staff_id  bigint(20)  default 0 COMMENT '创建者人员ID',
   modifier  varchar(32)  NULL COMMENT '修改者',
   modify_time  timestamp  NULL COMMENT '修改时间',
   modify_staff_id  bigint(20)  NULL COMMENT '修改者人员ID',
  PRIMARY KEY ( id )
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='索引表';
-- supfusion_i18n.supfusion_i18n_language definition

CREATE TABLE  supfusion_i18n_language  (
   id  bigint(20) NOT NULL  COMMENT '主键ID',
   langu_code  varchar(256) NOT NULL COMMENT '语言code码',
   langu_type  varchar(256) NOT NULL COMMENT '语言类型(中文描述)',
   langu_name  varchar(256) NOT NULL COMMENT '语言类型(code自己对应的语言描述)',
   has_used  tinyint(1) DEFAULT 1 COMMENT '是否启用,1:是 0:否',
   valid  tinyint(1) DEFAULT 1 COMMENT '逻辑删除,是否有效0:无效 1:有效',
   creator  varchar(32) default 'system' COMMENT '创建者',
   create_time  timestamp default current_timestamp COMMENT '创建时间',
   create_staff_id  bigint(20)  default 0 COMMENT '创建者人员ID',
   modifier  varchar(32)  NULL COMMENT '修改者',
   modify_time  timestamp  NULL COMMENT '修改时间',
   modify_staff_id  bigint(20)  NULL COMMENT '修改者人员ID',
  PRIMARY KEY ( id )
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='语言类型表';
INSERT INTO supfusion_i18n_language (id,langu_code,langu_type,langu_name,has_used,valid,creator,create_time,create_staff_id,modifier,modify_time,modify_staff_id) VALUES
(1,'zh_CN','中文（简体）','中文（简体）',1,1,'system','2020-01-13T18:14:35.300',1,NULL,'2020-01-13T18:14:35.300',NULL)
,(2,'zh_HK','中文（香港）','中文（繁体）',1,1,'system','2020-01-13T18:14:35.300',1,NULL,'2020-01-13T18:14:35.300',NULL)
,(3,'en_US','英文（美国）','English',1,1,'system','2020-01-13T18:14:35.300',1,NULL,'2020-01-13T18:14:35.300',NULL);


-- supfusion_i18n.supfusion_i18n_resource definition

CREATE TABLE  supfusion_i18n_resource (
   id  bigint(20) NOT NULL COMMENT '主键ID',
   i18n_key  varchar(255) NOT NULL COMMENT '国际化主键',
   i18n_value  varchar(1024) DEFAULT NULL COMMENT '国际化值',
   langu_code  varchar(255) NOT NULL COMMENT '语言类型',
   module_code  varchar(255) NOT NULL COMMENT '模块名',
   module_version_code  varchar(256) DEFAULT NULL COMMENT '版本号表',
   valid  tinyint(1) DEFAULT 1 COMMENT '逻辑删除,是否有效0:无效 1:有效',
   creator  varchar(32) default 'system' COMMENT '创建者',
   create_time  timestamp default current_timestamp COMMENT '创建时间',
   create_staff_id  bigint(20)  default 0 COMMENT '创建者人员ID',
   modifier  varchar(32)  NULL COMMENT '修改者',
   modify_time  timestamp  NULL COMMENT '修改时间',
   modify_staff_id  bigint(20)  NULL COMMENT '修改者人员ID',
  PRIMARY KEY ( id ),
  KEY  sup_i18n_re_key_IDX (i18n_key) USING BTREE,
  KEY  sup_i18n_re_value_IDX (i18n_value) USING BTREE,
  KEY  sup_i18n_re_langu_IDX  ( langu_code ) USING BTREE,
  KEY  sup_i18n_re_modu_IDX  ( module_code ) USING BTREE,
  KEY  sup_i18n_re_mo_time_IDX  ( modify_time ) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='国际化资源表';

-- supfusion_i18n.supfusion_i18n_token definition

CREATE TABLE  supfusion_i18n_token  (
   id  bigint(20) NOT NULL COMMENT '主键ID',
   module_code  varchar(256) NOT NULL COMMENT '模块名code',
   has_lock  tinyint(1) DEFAULT 0 COMMENT '是否持有锁,是否有锁 0:没有 1:有',
   token  varchar(255) NOT NULL COMMENT 'token字段',
   valid  tinyint(1) DEFAULT 1 COMMENT '逻辑删除,是否有效0:无效 1:有效',
   creator  varchar(32) default 'system' COMMENT '创建者',
   create_time  timestamp default current_timestamp COMMENT '创建时间',  
   create_staff_id  bigint(20)  default 0 COMMENT '创建者人员ID',
   modifier  varchar(32)  NULL COMMENT '修改者',
   modify_time  timestamp  NULL COMMENT '修改时间',
   modify_staff_id  bigint(20)  NULL COMMENT '修改者人员ID',
  PRIMARY KEY ( id )
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Token表';

-- supfusion_i18n.supfusion_i18n_version definition

CREATE TABLE  supfusion_i18n_version  (
   id  bigint(20) NOT NULL COMMENT '主键ID',
   module_code  varchar(256) NOT NULL COMMENT '模块名code',
   module_version_code  varchar(256) NOT NULL COMMENT '应用国际化资源版本号',
   valid  tinyint(1) DEFAULT 1 COMMENT '逻辑删除,是否有效0:无效 1:有效',
   creator  varchar(32) default 'system' COMMENT '创建者',
   create_time  timestamp default current_timestamp COMMENT '创建时间',
   create_staff_id  bigint(20)  default 0 COMMENT '创建者人员ID',
   modifier  varchar(32)  NULL COMMENT '修改者',
   modify_time  timestamp  NULL COMMENT '修改时间',
   modify_staff_id  bigint(20)  NULL COMMENT '修改者人员ID',
  PRIMARY KEY ( id )
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='版本号表';
alter table supfusion_i18n_index add unique index tb_ind_mo_code (module_code);
alter table supfusion_i18n_version add unique index tb_ver_mo_code (module_code);

## 1.0.1
Alter TABLE supfusion_i18n_excel ADD COLUMN tenant_id VARCHAR(64) DEFAULT 'dt' AFTER valid;
Alter TABLE supfusion_i18n_index ADD COLUMN tenant_id VARCHAR(64) DEFAULT 'dt' AFTER valid;
Alter TABLE supfusion_i18n_language ADD COLUMN tenant_id VARCHAR(64) DEFAULT 'dt' AFTER valid;
Alter TABLE supfusion_i18n_resource ADD COLUMN tenant_id VARCHAR(64) DEFAULT 'dt' AFTER valid;
Alter TABLE supfusion_i18n_resource CHANGE i18n_key i18n_key VARCHAR(255) CHARACTER SET utf8 COLLATE utf8_bin;
UPDATE supfusion_i18n_language SET tenant_id='dt' WHERE tenant_id IS NULL;
UPDATE supfusion_i18n_excel SET tenant_id='dt' WHERE tenant_id IS NULL;
UPDATE supfusion_i18n_index SET tenant_id='dt' WHERE tenant_id IS NULL;
UPDATE supfusion_i18n_resource SET tenant_id='dt' WHERE tenant_id IS NULL;
Alter TABLE supfusion_i18n_resource add index idx_resource_i18n_key (i18n_key);
Alter TABLE supfusion_i18n_resource drop index sup_i18n_re_key_IDX;
Alter TABLE supfusion_i18n_resource drop index sup_i18n_re_value_IDX;
Alter TABLE supfusion_i18n_resource drop index sup_i18n_re_langu_IDX;
Alter TABLE supfusion_i18n_index drop index tb_ind_mo_code;
Alter TABLE supfusion_i18n_index add unique index udx_index_code_tenant (module_code, tenant_id);
