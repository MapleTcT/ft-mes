## 1.0.0
/*==============================================================*/
/* Table: 系统编码字典实体表(systemcode_sys_entity)                */
/*==============================================================*/
create table if not exists sys_entity (
    id bigint not null comment '主键id',
    row_version bigint default 0 comment '版本号',
    type varchar(200) not null comment '类型(list或者tree)',
    code varchar(200) not null comment '编码',
    name varchar(255) default null comment '名称',
    display_name varchar(1000) default null comment '展示名称',
    module_id varchar(200) default null comment '模块id',
    cid bigint default null comment '所属公司id',
    valid tinyint default 1 comment '是否有效',
    multi_flag tinyint default 0 comment '是否多选',
    sys_default tinyint default 0 comment '是否系统默认',
    memo varchar(500) default null  comment '备注',
    creator varchar(200) default 'system' comment '创建者',
    modifier varchar(200) default null comment '修改者',
    create_staff_id bigint comment '创建者人员id',
    modify_staff_id bigint comment '修改者人员id',
    create_time timestamp not null default current_timestamp comment '创建时间',
    modify_time timestamp comment '修改时间',
    primary key (id)
)
engine=innodb
default charset=utf8
comment='系统编码字典实体表';

alter table sys_entity modify column name varchar(510);
alter table sys_entity modify column memo varchar(600);

/*==============================================================*/
/* Table: 系统编码值表(sys_code)                     */
/*==============================================================*/
create table if not exists sys_code (
    id bigint not null comment '主键id',
    row_version bigint default 0 comment '版本号',
    type varchar(200) default null comment '类型',
    code varchar(200) not null comment '编码',
    entity_code varchar(200) not null comment '系统字典实体编码',
    name varchar(255) default null comment '名称',
    display_name varchar(1000) not null comment '展示名称',
    cid bigint default null comment '所属公司id',
    valid tinyint default 1 comment '是否有效',
    leaf tinyint default 0 comment '是否叶子节点',
    default_flag tinyint default 0 comment '是否默认',
    full_path varchar(4000) default null comment '全路径',
    full_path_name varchar(4000) default null comment '全路径',
    parent_id bigint default null comment '父级节点id',
    parent_name varchar(200) default null comment '父节点名称',
    lay_no int default null comment '层级序号',
    lay_rec varchar(500) default null comment '层级',
    seq_id bigint default null comment '序号',
    sort double default null comment '排序',
    des_a varchar(500) default null comment '描述a',
    des_b varchar(500) default null comment '描述b',
    des_c varchar(500) default null comment '描述c',
    memo varchar(500) default null comment '备注',
    creator varchar(200) default 'system' comment '创建者',
    modifier varchar(200) default null comment '修改者',
    create_staff_id bigint comment '创建者人员id',
    modify_staff_id bigint comment '修改者人员id',
    create_time timestamp not null default current_timestamp comment '创建时间',
    modify_time timestamp comment '修改时间',
    primary key (id)
)
engine=innodb
default charset=utf8
comment='系统编码值表';

alter table sys_code modify column name varchar(510);
alter table sys_code modify column memo varchar(600);
alter table sys_code modify column des_a varchar(600);
alter table sys_code modify column des_b varchar(600);
alter table sys_code modify column des_c varchar(600);

alter table sys_entity modify modify_time timestamp not null default current_timestamp;
alter table sys_code modify modify_time timestamp not null default current_timestamp;

insert into sys_entity(id,code,name,display_name,type,module_id,cid,sys_default,memo) select 1000,'sys_gender','systemCode.sys_gender','性别','list','sys',1000,1,'系统默认' from DUAL where not exists(select code from sys_entity where code='sys_gender');
insert into sys_code(id,code,name,display_name,entity_code,cid,default_flag,full_path,parent_name,lay_no,sort,memo) select 1000,'male','systemCode.male','男','sys_gender',1000,1,'male','systemCode.sys_gender',1,1,'系统默认' from DUAL where not exists(select code from sys_code where code='male');
insert into sys_code(id,code,name,display_name,entity_code,cid,default_flag,full_path,parent_name,lay_no,sort,memo) select 1001,'female','systemCode.female','女','sys_gender',1000,0,'female','systemCode.sys_gender',1,2,'系统默认' from DUAL where not exists(select code from sys_code where code='female');

insert into sys_entity(id,code,name,display_name,type,module_id,cid,sys_default,memo) select 1001,'sys_education','systemCode.sys_education','学历','list','sys',1000,1,'系统默认' from DUAL where not exists(select code from sys_entity where code='sys_education');
insert into sys_code(id,code,name,display_name,entity_code,cid,default_flag,full_path,parent_name,lay_no,sort,memo) select 1002,'phd','systemCode.phd','博士','sys_education',1000,1,'phd','systemCode.sys_education',1,1,'系统默认' from DUAL where not exists(select code from sys_code where code='phd');
insert into sys_code(id,code,name,display_name,entity_code,cid,default_flag,full_path,parent_name,lay_no,sort,memo) select 1003,'master','systemCode.master','硕士','sys_education',1000,0,'master','systemCode.sys_education',1,2,'系统默认' from DUAL where not exists(select code from sys_code where code='master');
insert into sys_code(id,code,name,display_name,entity_code,cid,default_flag,full_path,parent_name,lay_no,sort,memo) select 1004,'college','systemCode.college','本科','sys_education',1000,0,'college','systemCode.sys_education',1,3,'系统默认' from DUAL where not exists(select code from sys_code where code='college');
insert into sys_code(id,code,name,display_name,entity_code,cid,default_flag,full_path,parent_name,lay_no,sort,memo) select 1005,'degree','systemCode.degree','大专','sys_education',1000,0,'degree','systemCode.sys_education',1,4,'系统默认' from DUAL where not exists(select code from sys_code where code='degree');
insert into sys_code(id,code,name,display_name,entity_code,cid,default_flag,full_path,parent_name,lay_no,sort,memo) select 1006,'specialOrOther','systemCode.specialOrOther','中专或其他','sys_education',1000,0,'specialOrOther','systemCode.sys_education',1,5,'系统默认' from DUAL where not exists(select code from sys_code where code='specialOrOther');

insert into sys_entity(id,code,name,display_name,type,module_id,cid,sys_default,memo) select 1002,'sys_person_status','systemCode.sys_person_status','状态','list','sys',1000,1,'系统默认' from DUAL where not exists(select code from sys_entity where code='sys_person_status');
insert into sys_code(id,code,name,display_name,entity_code,cid,default_flag,full_path,parent_name,lay_no,sort,memo) select 1007,'onWork','systemCode.onWork','在职','sys_person_status',1000,1,'onWork','systemCode.sys_person_status',1,1,'系统默认' from DUAL where not exists(select code from sys_code where code='onWork');
insert into sys_code(id,code,name,display_name,entity_code,cid,default_flag,full_path,parent_name,lay_no,sort,memo) select 1008,'offWork','systemCode.offWork','离职','sys_person_status',1000,0,'offWork','systemCode.sys_person_status',1,2,'系统默认' from DUAL where not exists(select code from sys_code where code='offWork');

insert into sys_entity(id,code,name,display_name,type,module_id,cid,sys_default,memo) select 1003,'sys_classified_grade','systemCode.sys_classified_grade','涉密等级','list','sys',1000,1,'系统默认' from DUAL where not exists(select code from sys_entity where code='sys_classified_grade');
insert into sys_code(id,code,name,display_name,entity_code,cid,default_flag,full_path,parent_name,lay_no,sort,memo) select 1009,'unclassified','systemCode.unclassified','非密','sys_classified_grade',1000,1,'unclassified','systemCode.sys_classified_grade',1,1,'系统默认' from DUAL where not exists(select code from sys_code where code='unclassified');
insert into sys_code(id,code,name,display_name,entity_code,cid,default_flag,full_path,parent_name,lay_no,sort,memo) select 1010,'generalClassified','systemCode.generalClassified','一般涉密','sys_classified_grade',1000,0,'generalClassified','systemCode.sys_classified_grade',1,2,'系统默认' from DUAL where not exists(select code from sys_code where code='generalClassified');
insert into sys_code(id,code,name,display_name,entity_code,cid,default_flag,full_path,parent_name,lay_no,sort,memo) select 1011,'importantClassified','systemCode.importantClassified','重要涉密','sys_classified_grade',1000,0,'importantClassified','systemCode.sys_classified_grade',1,3,'系统默认' from DUAL where not exists(select code from sys_code where code='importantClassified');
insert into sys_code(id,code,name,display_name,entity_code,cid,default_flag,full_path,parent_name,lay_no,sort,memo) select 1012,'coreClassified','systemCode.coreClassified','核心涉密','sys_classified_grade',1000,0,'coreClassified','systemCode.sys_classified_grade',1,4,'系统默认' from DUAL where not exists(select code from sys_code where code='coreClassified');

insert into sys_entity(id,code,name,display_name,type,module_id,cid,sys_default,memo) select 1004,'sys_department_type','systemCode.sys_department_type','部门类型','list','sys',1000,1,'系统默认' from DUAL where not exists(select code from sys_entity where code='sys_department_type');
insert into sys_code(id,code,name,display_name,entity_code,cid,default_flag,full_path,parent_name,lay_no,sort,memo) select 1013,'general','systemCode.general','普通部门','sys_department_type',1000,1,'general','systemCode.sys_department_type',1,1,'系统默认' from DUAL where not exists(select code from sys_code where code='general');
insert into sys_code(id,code,name,display_name,entity_code,cid,default_flag,full_path,parent_name,lay_no,sort,memo) select 1014,'emergency','systemCode.emergency','应急部门','sys_department_type',1000,0,'emergency','systemCode.sys_department_type',1,2,'系统默认' from DUAL where not exists(select code from sys_code where code='emergency');

insert into sys_entity(id,code,name,display_name,type,module_id,cid,sys_default,memo) select 1005,'sys_auth_user_directory','systemCode.sys_auth_user_directory','用户目录类型','list','sys',1000,1,'系统默认' from DUAL where not exists(select code from sys_entity where code='sys_auth_user_directory');
insert into sys_code(id,code,name,display_name,entity_code,cid,default_flag,full_path,parent_name,lay_no,sort,memo) select 1015,'msad','systemCode.msad','Microsoft活动目录','sys_auth_user_directory',1000,1,'msad','systemCode.sys_auth_user_directory',1,1,'系统默认' from DUAL where not exists(select code from sys_code where code='msad');
insert into sys_code(id,code,name,display_name,entity_code,cid,default_flag,full_path,parent_name,lay_no,sort,memo) select 1016,'ldap','systemCode.ldap','LDAP','sys_auth_user_directory',1000,0,'ldap','systemCode.sys_auth_user_directory',1,2,'系统默认' from DUAL where not exists(select code from sys_code where code='ldap');

insert into sys_entity(id,code,name,display_name,type,module_id,cid,sys_default,memo) select 1006,'sys_auth_protocol','systemCode.sys_auth_protocol','认证协议类型','list','sys',1000,1,'系统默认' from DUAL where not exists(select code from sys_entity where code='sys_auth_protocol');
insert into sys_code(id,code,name,display_name,entity_code,cid,default_flag,full_path,parent_name,lay_no,sort,memo) select 1017,'oauth','systemCode.oauth','OAuth','sys_auth_protocol',1000,1,'oauth','systemCode.sys_auth_protocol',1,1,'系统默认' from DUAL where not exists(select code from sys_code where code='oauth');
insert into sys_code(id,code,name,display_name,entity_code,cid,default_flag,full_path,parent_name,lay_no,sort,memo) select 1018,'saml','systemCode.saml','SAML','sys_auth_protocol',1000,0,'saml','systemCode.sys_auth_protocol',1,2,'系统默认' from DUAL where not exists(select code from sys_code where code='saml');

insert into sys_entity(id,code,name,display_name,type,module_id,cid,sys_default,memo) select 1007,'sys_auth_grant','systemCode.sys_auth_grant','授权类型','list','sys',1000,1,'系统默认' from DUAL where not exists(select code from sys_entity where code='sys_auth_grant');
insert into sys_code(id,code,name,display_name,entity_code,cid,default_flag,full_path,parent_name,lay_no,sort,memo) select 1019,'authorizationCode','systemCode.authorizationCode','授权码模式','sys_auth_grant',1000,1,'authorizationCode','systemCode.sys_auth_grant',1,1,'系统默认' from DUAL where not exists(select code from sys_code where code='authorizationCode');
insert into sys_code(id,code,name,display_name,entity_code,cid,default_flag,full_path,parent_name,lay_no,sort,memo) select 1020,'implicit','systemCode.implicit','隐式模式','sys_auth_grant',1000,0,'implicit','systemCode.sys_auth_grant',1,2,'系统默认' from DUAL where not exists(select code from sys_code where code='implicit');
insert into sys_code(id,code,name,display_name,entity_code,cid,default_flag,full_path,parent_name,lay_no,sort,memo) select 1021,'passwordCredentials','systemCode.passwordCredentials','密码模式','sys_auth_grant',1000,0,'passwordCredentials','systemCode.sys_auth_grant',1,3,'系统默认' from DUAL where not exists(select code from sys_code where code='passwordCredentials');
insert into sys_code(id,code,name,display_name,entity_code,cid,default_flag,full_path,parent_name,lay_no,sort,memo) select 1022,'clientCredentials','systemCode.clientCredentials','客户端凭证模式','sys_auth_grant',1000,0,'clientCredentials','systemCode.sys_auth_grant',1,4,'系统默认' from DUAL where not exists(select code from sys_code where code='clientCredentials');

insert into sys_entity(id,code,name,display_name,type,module_id,cid,sys_default,memo) select 1008,'sys_auth_method','systemCode.sys_auth_method','认证方式','list','sys',1000,1,'系统默认' from DUAL where not exists(select code from sys_entity where code='sys_auth_method');
insert into sys_code(id,code,name,display_name,entity_code,cid,default_flag,full_path,parent_name,lay_no,sort,memo) select 1023,'np','systemCode.np','用户名+密码','sys_auth_method',1000,1,'np','systemCode.sys_auth_method',1,1,'系统默认' from DUAL where not exists(select code from sys_code where code='np');
insert into sys_code(id,code,name,display_name,entity_code,cid,default_flag,full_path,parent_name,lay_no,sort,memo) select 1024,'npm','systemCode.npm','用户名+密码+短信','sys_auth_method',1000,0,'npm','systemCode.sys_auth_method',1,2,'系统默认' from DUAL where not exists(select code from sys_code where code='npm');

insert into sys_entity(id,code,name,display_name,type,module_id,cid,sys_default,memo) values(1009,'sys_operate_type','systemCode.sys_operate_type','操作类型','list','sys',1000,1,'系统默认');
insert into sys_code(id,code,name,display_name,entity_code,cid,default_flag,full_path,parent_name,lay_no,sort,memo) values(1025,'ADD','systemCode.operateType.ADD','新增','sys_operate_type',1000,1,'ADD','systemCode.sys_operate_type',1,1,'系统默认');
insert into sys_code(id,code,name,display_name,entity_code,cid,default_flag,full_path,parent_name,lay_no,sort,memo) values(1026,'MODIFY','systemCode.operateType.MODIFY','修改','sys_operate_type',1000,0,'MODIFY','systemCode.sys_operate_type',1,2,'系统默认');
insert into sys_code(id,code,name,display_name,entity_code,cid,default_flag,full_path,parent_name,lay_no,sort,memo) values(1027,'DELETE','systemCode.operateType.DELETE','删除','sys_operate_type',1000,0,'DELETE','systemCode.sys_operate_type',1,3,'系统默认');
insert into sys_code(id,code,name,display_name,entity_code,cid,default_flag,full_path,parent_name,lay_no,sort,memo) values(1028,'OTHER','systemCode.operateType.OTHER','其他','sys_operate_type',1000,0,'OTHER','systemCode.sys_operate_type',1,4,'系统默认');
insert into sys_code(id,code,name,display_name,entity_code,cid,default_flag,full_path,parent_name,lay_no,sort,memo) values(1029,'IMPORT','systemCode.operateType.IMPORT','导入','sys_operate_type',1000,0,'IMPORT','systemCode.sys_operate_type',1,5,'系统默认');
insert into sys_code(id,code,name,display_name,entity_code,cid,default_flag,full_path,parent_name,lay_no,sort,memo) values(1030,'EXPORT','systemCode.operateType.EXPORT','导出','sys_operate_type',1000,0,'EXPORT','systemCode.sys_operate_type',1,6,'系统默认');
insert into sys_code(id,code,name,display_name,entity_code,cid,default_flag,full_path,parent_name,lay_no,sort,memo) values(1031,'INVALID','systemCode.operateType.INVALID','作废','sys_operate_type',1000,0,'INVALID','systemCode.sys_operate_type',1,7,'系统默认');
insert into sys_code(id,code,name,display_name,entity_code,cid,default_flag,full_path,parent_name,lay_no,sort,memo) values(1032,'REJECT','systemCode.operateType.REJECT','驳回','sys_operate_type',1000,0,'REJECT','systemCode.sys_operate_type',1,8,'系统默认');
insert into sys_code(id,code,name,display_name,entity_code,cid,default_flag,full_path,parent_name,lay_no,sort,memo) values(1033,'PRINT','systemCode.operateType.PRINT','打印','sys_operate_type',1000,0,'PRINT','systemCode.sys_operate_type',1,9,'系统默认');
insert into sys_code(id,code,name,display_name,entity_code,cid,default_flag,full_path,parent_name,lay_no,sort,memo) values(1034,'BATCH_PRINT','systemCode.operateType.BATCH_PRINT','批量打印','sys_operate_type',1000,0,'BATCH_PRINT','systemCode.sys_operate_type',1,10,'系统默认');
insert into sys_code(id,code,name,display_name,entity_code,cid,default_flag,full_path,parent_name,lay_no,sort,memo) values(1035,'ROLLBACK','systemCode.operateType.ROLLBACK','还原','sys_operate_type',1000,0,'ROLLBACK','systemCode.sys_operate_type',1,11,'系统默认');

create or replace view base_systementity (id, version, list_type, code, name, module_code, cid, valid, multi_flag, sys_default, memo, create_staff_id, modify_staff_id, delete_staff_id, create_time, modify_time, delete_time, code_desc, code_desb, code_desa, sys_class_code) as
  (
select
  id,
  row_version as version,
  type as list_type,
  code,
  name,
  module_id as module_code,
  cid,
  valid,
  multi_flag,
  sys_default,
  memo,
  null as create_staff_id,
  null as modify_staff_id,
  null as delete_staff_id,
  create_time,
  modify_time,
  null as delete_time,
  null as code_desc,
  null as code_desb,
  null as code_desa,
  null as sys_class_code
from sys_entity
where valid = 1
);

create or replace view base_systemcode (id, version, code, type, entity_code, value, value_zh_cn, cid, valid, leaf, default_flag, full_path_name, parent_id, lay_no, lay_rec, seq_id, sort, code_desa, code_desb, code_desc, memo, create_staff_id, modify_staff_id, delete_staff_id, create_time, modify_time, delete_time, attribute) as
  (
select
  concat(concat(entity_code,'/'),code) as id,
  row_version as version,
  code,
  type,
  entity_code,
  name as value,
  display_name as value_zh_cn,
  cid,
  valid,
  leaf,
  default_flag,
  full_path_name,
  parent_id,
  lay_no,
  lay_rec,
  seq_id,
  sort,
  des_a as code_desa,
  des_b as code_desb,
  des_c as code_desc,
  memo,
  null as create_staff_id,
  null as modify_staff_id,
  null as delete_staff_id,
  create_time,
  modify_time,
  null as delete_time,
  null as attribute
from sys_code
where valid = 1
);

insert into sys_entity(id, row_version, type, code, name,display_name, module_id, cid, valid, multi_flag,sys_default, memo)values(2000, 0, 'list', 'COMPANY_NATURE', 'systemCode.foundation.company.nature','企业性质', 'sys', 1000, 1, 0,1, '系统默认');
insert into sys_entity(id, row_version, type, code, name,display_name, module_id, cid, valid, multi_flag,sys_default, memo)values(2001, 0, 'list', 'POSITION_CATEGORY', 'systemCode.foundation.infoSetCol.positiontypenature','岗位类别', 'sys', 1000, 1, 0,1, '系统默认');
insert into sys_entity(id, row_version, type, code, name,display_name, module_id, cid, valid, multi_flag,sys_default, memo)values(2002, 0, 'list', 'EDUCATIONAL_NATURE', 'systemCode.foundation.infoSetColumn.JTCYXL','学历', 'sys', 1000, 1, 0,1, '系统默认');
insert into sys_entity(id, row_version, type, code, name,display_name, module_id, cid, valid, multi_flag,sys_default, memo)values(2003, 0, 'list', 'MARRIAGE_NATURE', 'systemCode.foundation.systemEntity.marriage','婚姻状况', 'sys', 1000, 1, 0,1, '系统默认');
insert into sys_entity(id, row_version, type, code, name,display_name, module_id, cid, valid, multi_flag,sys_default, memo)values(2004, 0, 'list', 'POLITICSINFO_NATURE', 'systemCode.foundation.infoSetColumn.POLITICS_INFO','政治面貌', 'sys', 1000, 1, 0,1, '系统默认');
insert into sys_entity(id, row_version, type, code, name,display_name, module_id, cid, valid, multi_flag,sys_default, memo)values(2005, 0, 'list', 'HUKOUXZ_NATURE', 'systemCode.foundation.infoSetColumn.HUKOUXINGZHI','户口性质', 'sys', 1000, 1, 0,1, '系统默认');
insert into sys_entity(id, row_version, type, code, name,display_name, module_id, cid, valid, multi_flag,sys_default, memo)values(2006, 0, 'list', 'NATION_NATURE', 'systemCode.foundation.infoSetColumn.NATION','民族', 'sys', 1000, 1, 0,1, '系统默认');
insert into sys_entity(id, row_version, type, code, name,display_name, module_id, cid, valid, multi_flag,sys_default, memo)values(2007, 0, 'list', 'SEX_NATURE', 'systemCode.foundation.infoSetColumn.SEX','性别', 'sys', 1000, 1, 0,1, '系统默认');
insert into sys_entity(id, row_version, type, code, name,display_name, module_id, cid, valid, multi_flag,sys_default, memo)values(2008, 0, 'list', 'DEGREE_NATURE', 'systemCode.foundation.systemEntity.DEGREE','学位', 'sys', 1000, 1, 0,1, '系统默认');
insert into sys_entity(id, row_version, type, code, name,display_name, module_id, cid, valid, multi_flag,sys_default, memo)values(2009, 0, 'list', 'STAFFSTATUSE_NATURE', 'systemCode.foundation.infoSetColumn.WORK_STATUS','人员状态', 'sys', 1000, 1, 0,1, '系统默认');
insert into sys_entity(id, row_version, type, code, name,display_name, module_id, cid, valid, multi_flag,sys_default, memo)values(2010, 1, 'list', 'PAYMENT_NATURE', 'systemCode.foundation.systemEntity.PAYMENT_NATURE','薪资类别', 'sys', 1000, 1, 0,1, '系统默认');
insert into sys_entity(id, row_version, type, code, name,display_name, module_id, cid, valid, multi_flag,sys_default, memo)values(2011, 0, 'list', 'LEAVETYPE_NATURE', 'systemCode.foundation.infoSetColumn.BDLB','变动类别', 'sys', 1000, 1, 0,1, '系统默认');
insert into sys_entity(id, row_version, type, code, name,display_name, module_id, cid, valid, multi_flag,sys_default, memo)values(2012, 0, 'list', 'CONTRACTTYPE_NATURE', 'systemCode.foundation.infoSetColumn.HTLB','合同类别', 'sys', 1000, 1, 0,1, '系统默认');
insert into sys_entity(id, row_version, type, code, name,display_name, module_id, cid, valid, multi_flag,sys_default, memo)values(2013, 0, 'list', 'CONTRACTCLASS_NATURE', 'systemCode.foundation.infoSetColumn.HTLX','合同类型', 'sys', 1000, 1, 0,1, '系统默认');
insert into sys_entity(id, row_version, type, code, name,display_name, module_id, cid, valid, multi_flag,sys_default, memo)values(2014, 0, 'list', 'CONTRACTTIME_NATURE', 'systemCode.foundation.infoSetColumn.THQX','合同期限', 'sys', 1000, 1, 0,1, '系统默认');
insert into sys_entity(id, row_version, type, code, name,display_name, module_id, cid, valid, multi_flag,sys_default, memo)values(2015, 0, 'list', 'ZHICHENG_NATURE', 'systemCode.foundation.infoSetCol.innerpostnature','内部职称', 'sys', 1000, 1, 0,1, '系统默认');
insert into sys_entity(id, row_version, type, code, name,display_name, module_id, cid, valid, multi_flag,sys_default, memo)values(2016, 0, 'list', 'TRANSACTION_NATURE', 'systemCode.foundation.infoSetColumn.YDLB','异动类别', 'sys', 1000, 1, 0,1, '系统默认');
insert into sys_entity(id, row_version, type, code, name,display_name, module_id, cid, valid, multi_flag,sys_default, memo)values(2017, 0, 'list', 'YGXS', 'systemCode.foundation.infoSetColumn.YGXS','用工形式', 'sys', 1000, 1, 0,1, '系统默认');
insert into sys_entity(id, row_version, type, code, name,display_name, module_id, cid, valid, multi_flag,sys_default, memo)values(2018, 0, 'list', 'ZHICHENJB_NATURE', 'systemCode.foundation.infoSetCol.outerpostnature','外部职称', 'sys', 1000, 1, 0,1, '系统默认');
insert into sys_entity(id, row_version, type, code, name,display_name, module_id, cid, valid, multi_flag,sys_default, memo)values(2019, 0, 'list', 'SYSTEM', 'systemCode.foundation.SystemCode.system','所属系统', 'sys', 1000, 1, 0,1, '系统默认');
insert into sys_entity(id, row_version, type, code, name,display_name, module_id, cid, valid, multi_flag,sys_default, memo)values(2020, 0, 'list', 'EXTENSION_ZONETYPE', 'systemCode.foundation.SystemCode.extension.zoneType','扩展点区域', 'sys', 1000, 1, 0,1, '系统默认');
insert into sys_entity(id, row_version, type, code, name,display_name, module_id, cid, valid, multi_flag,sys_default, memo)values(2021, 0, 'list', 'ROLE_TYPE', 'systemCode.foundation.SystemCode.role.type','角色类型', 'sys', 1000, 1, 0,1, '系统默认');
insert into sys_entity(id, row_version, type, code, name,display_name, module_id, cid, valid, multi_flag,sys_default, memo)values(2022, 0, 'list', 'DEPARTMENT_CATEGORY', 'systemCode.foundation.SystemCode.department.type','部门类别', 'sys', 1000, 1, 0,1, '系统默认');
insert into sys_entity(id, row_version, type, code, name,display_name, module_id, cid, valid, multi_flag,sys_default, memo)values(2023, 0, 'list', 'SECURITY_CLASS', 'systemCode.foundation.infoSetColumn.SECURITYCLASS','密级', 'sys', 1000, 1, 0,1, '系统默认');
insert into sys_entity(id, row_version, type, code, name,display_name, module_id, cid, valid, multi_flag,sys_default, memo)values(2024, 0, 'list', 'SECRET_CLASS', 'systemCode.foundation.menuinfo.SECURITYCLASS','密级', 'sys', 1000, 1, 0,1, '系统默认');
insert into sys_entity(id, row_version, type, code, name,display_name, module_id, cid, valid, multi_flag,sys_default, memo)values(2025, 0, 'list', 'BASE_CONFIG_LAYERS', 'systemCode.foundation.SystemCode.GISLayer','GIS图层', 'sys', 1000, 1, 0,1, '系统默认');
insert into sys_entity(id, row_version, type, code, name,display_name, module_id, cid, valid, multi_flag,sys_default, memo)values(2026, 0, 'list', 'APP_TYPE', 'systemCode.foundation.mobile.AppType','应用类型', 'sys', 1000, 1, 0,1, '系统默认');
insert into sys_entity(id, row_version, type, code, name,display_name, module_id, cid, valid, multi_flag,sys_default, memo)values(2027, 0, 'list', 'SCREEN_TYPE', 'systemCode.foundation.mobile.ScreenType','屏幕方向', 'sys', 1000, 1, 0,1, '系统默认');

insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2000, 0, 'NATION_NATURE_95', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_95','怒族', 1000, 1, 0, 0,'NATION_NATURE_95', '怒族', 0, '12273', 12273,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2001, 0, 'NATION_NATURE_97', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_97','俄罗斯族', 1000, 1, 0, 0,'NATION_NATURE_97', '俄罗斯族', 0, '12274', 12274,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2002, 0, 'NATION_NATURE_98', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_98','鄂温克族', 1000, 1, 0, 0,'NATION_NATURE_98', '鄂温克族', 0, '12275', 12275,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2003, 0, 'NATION_NATURE_100', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_100','保安族', 1000, 1, 0, 0,'NATION_NATURE_100', '保安族', 0, '12276', 12276,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2004, 0, 'NATION_NATURE_105', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_105','鄂伦春族', 1000, 1, 0, 0,'NATION_NATURE_105', '鄂伦春族', 0, '12277', 12277,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2005, 0, 'NATION_NATURE_107', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_107','门巴族', 1000, 1, 0, 0,'NATION_NATURE_107', '门巴族', 0, '12278', 12278,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2006, 0, 'NATION_NATURE_109', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_109','基诺族', 1000, 1, 0, 0,'NATION_NATURE_109', '基诺族', 0, '12279', 12279,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2007, 0, 'NATION_NATURE_110', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_110','畲族', 1000, 1, 0, 0,'NATION_NATURE_110', '畲族', 0, '12280', 12280,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2008, 0, 'NATION_NATURE_34', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_34','汉族', 1000, 1, 0, 0,'NATION_NATURE_34', '汉族', 0, '12281', 12281,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2009, 0, 'NATION_NATURE_36', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_36','回族', 1000, 1, 0, 0,'NATION_NATURE_36', '回族', 0, '12282', 12282,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2010, 0, 'NATION_NATURE_37', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_37','藏族', 1000, 1, 0, 0,'NATION_NATURE_37', '藏族', 0, '12283', 12283,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2011, 0, 'NATION_NATURE_40', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_40','彝族', 1000, 1, 0, 0,'NATION_NATURE_40', '彝族', 0, '12284', 12284,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2012, 0, 'NATION_NATURE_41', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_41','壮族', 1000, 1, 0, 0,'NATION_NATURE_41', '壮族', 0, '12285', 12285,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2013, 0, 'NATION_NATURE_42', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_42','布依族', 1000, 1, 0, 0,'NATION_NATURE_42', '布依族', 0, '12286', 12286,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2014, 0, 'NATION_NATURE_43', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_43','朝鲜族', 1000, 1, 0, 0,'NATION_NATURE_43', '朝鲜族', 0, '12287', 12287,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2042, 0, 'NATION_NATURE_71', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_71','哈萨克族', 1000, 1, 0, 0,'NATION_NATURE_71', '哈萨克族', 0, '12315', 12315,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2043, 0, 'NATION_NATURE_72', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_72','傣族', 1000, 1, 0, 0,'NATION_NATURE_72', '傣族', 0, '12316', 12316,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2044, 0, 'NATION_NATURE_73', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_73','黎族', 1000, 1, 0, 0,'NATION_NATURE_73', '黎族', 0, '12317', 12317,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2045, 0, 'NATION_NATURE_74', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_74','僳僳族', 1000, 1, 0, 0,'NATION_NATURE_74', '僳僳族', 0, '12318', 12318,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2046, 0, 'NATION_NATURE_75', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_75','佤族', 1000, 1, 0, 0,'NATION_NATURE_75', '佤族', 0, '12319', 12319,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2047, 0, 'NATION_NATURE_76', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_76','高山族', 1000, 1, 0, 0,'NATION_NATURE_76', '高山族', 0, '12320', 12320,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2048, 0, 'NATION_NATURE_77', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_77','拉祜族', 1000, 1, 0, 0,'NATION_NATURE_77', '拉祜族', 0, '12321', 12321,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2049, 0, 'NATION_NATURE_99', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_99','德昂族', 1000, 1, 0, 0,'NATION_NATURE_99', '德昂族', 0, '12322', 12322,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2050, 0, 'NATION_NATURE_108', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_108','珞巴族', 1000, 1, 0, 0,'NATION_NATURE_108', '珞巴族', 0, '12323', 12323,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2051, 0, 'NATION_NATURE_38', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_38','维吾尔族', 1000, 1, 0, 0,'NATION_NATURE_38', '维吾尔族', 0, '12324', 12324,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2052, 0, 'NATION_NATURE_39', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_39','苗族', 1000, 1, 0, 0,'NATION_NATURE_39', '苗族', 0, '12325', 12325,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2053, 0, 'NATION_NATURE_83', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_83','土族', 1000, 1, 0, 0,'NATION_NATURE_83', '土族', 0, '12326', 12326,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2054, 0, 'NATION_NATURE_86', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_86','羌族', 1000, 1, 0, 0,'NATION_NATURE_86', '羌族', 0, '12327', 12327,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2055, 0, 'NATION_NATURE_89', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_89','毛南族', 1000, 1, 0, 0,'NATION_NATURE_89', '毛南族', 0, '12328', 12328,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2056, 0, 'NATION_NATURE_93', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_93','普米族', 1000, 1, 0, 0,'NATION_NATURE_93', '普米族', 0, '12329', 12329,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2057, 0, 'NATION_NATURE_96', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_96','乌孜别克族', 1000, 1, 0, 0,'NATION_NATURE_96', '乌孜别克族', 0, '12330', 12330,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2058, 0, 'NATION_NATURE_101', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_101','裕固族', 1000, 1, 0, 0,'NATION_NATURE_101', '裕固族', 0, '12331', 12331,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2059, 0, 'NATION_NATURE_106', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_106','赫哲族', 1000, 1, 0, 0,'NATION_NATURE_106', '赫哲族', 0, '12332', 12332,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2060, 0, 'NATION_NATURE_35', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_35','蒙古族', 1000, 1, 0, 0,'NATION_NATURE_35', '蒙古族', 0, '12333', 12333,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2061, 0, 'NATION_NATURE_103', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_103','塔塔尔族', 1000, 1, 0, 0,'NATION_NATURE_103', '塔塔尔族', 0, '12334', 12334,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2062, 0, 'NATION_NATURE_104', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_104','独龙族', 1000, 1, 0, 0,'NATION_NATURE_104', '独龙族', 0, '12335', 12335,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2063, 0, 'FEMALE', 'SEX_NATURE', 'systemCode.foundation.SystemCode.FEMALE','女', 1000, 1, 0, 0,'FEMALE', '女', 0, '12336', 12336,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2064, 0, 'MALE', 'SEX_NATURE', 'systemCode.foundation.SystemCode.MALE','男', 1000, 1, 0, 0,'MALE', '男', 0, '12337', 12337,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c)values(2065, 0, 'DEGREE_NATURE_02', 'DEGREE_NATURE', 'systemCode.foundation.SystemCode.DEGREE_NATURE_02','硕士', 1000, 1, 0, 0,'DEGREE_NATURE_02', '硕士', 0, '12338', 12338,2, '0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c)values(2066, 0, 'DEGREE_NATURE_04', 'DEGREE_NATURE', 'systemCode.foundation.SystemCode.DEGREE_NATURE_04','博士后', 1000, 1, 0, 0,'DEGREE_NATURE_04', '博士后', 0, '12339', 12339,4, '0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c)values(2067, 0, 'DEGREE_NATURE_01', 'DEGREE_NATURE', 'systemCode.foundation.SystemCode.DEGREE_NATURE_01','学士', 1000, 1, 0, 0,'DEGREE_NATURE_01', '学士', 0, '12340', 12340,1, '0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c)values(2068, 0, 'DEGREE_NATURE_03', 'DEGREE_NATURE', 'systemCode.foundation.SystemCode.DEGREE_NATURE_03','博士', 1000, 1, 0, 0,'DEGREE_NATURE_03', '博士', 0, '12341', 12341,3, '0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2069, 0, 'STAFFSTATUTS_02', 'STAFFSTATUSE_NATURE', 'systemCode.foundation.SystemCode.STAFFSTATUTS_02','转正', 1000, 1, 0, 0,'STAFFSTATUTS_02', '转正', 0, '12342', 12342,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2070, 0, 'STAFFSTATUTS_03', 'STAFFSTATUSE_NATURE', 'systemCode.foundation.SystemCode.STAFFSTATUTS_03','离职', 1000, 1, 0, 0,'STAFFSTATUTS_03', '离职', 0, '12343', 12343,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2071, 0, 'STAFFSTATUTS_01', 'STAFFSTATUSE_NATURE', 'systemCode.foundation.SystemCode.STAFFSTATUTS_01','试用', 1000, 1, 0, 0,'STAFFSTATUTS_01', '试用', 0, '12344', 12344,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2072, 0, 'PAYMENT_01', 'PAYMENT_NATURE', 'systemCode.foundation.SystemCode.PAYMENT_01','岗位工资', 1000, 1, 0, 0,'PAYMENT_01', '岗位工资', 0, '12345', 12345,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2073, 0, 'PAYMENT_02', 'PAYMENT_NATURE', 'systemCode.foundation.SystemCode.PAYMENT_02','工龄工资', 1000, 1, 0, 0,'PAYMENT_02', '工龄工资', 0, '12346', 12346,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2074, 0, 'PAYMENT_03', 'PAYMENT_NATURE', 'systemCode.foundation.SystemCode.PAYMENT_03','基本工资', 1000, 1, 0, 0,'PAYMENT_03', '基本工资', 0, '12347', 12347,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2075, 0, 'LEAVETYPE_01', 'LEAVETYPE_NATURE', 'systemCode.foundation.SystemCode.LEAVETYPE_01','主动', 1000, 1, 0, 0,'LEAVETYPE_01', '主动', 0, '12348', 12348,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2076, 0, 'LEAVETYPE_02', 'LEAVETYPE_NATURE', 'systemCode.foundation.SystemCode.LEAVETYPE_02','被动', 1000, 1, 0, 0,'LEAVETYPE_02', '被动', 0, '12349', 12349,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2077, 0, 'CONTRACTTYPE_01', 'CONTRACTTYPE_NATURE', 'systemCode.foundation.SystemCode.CONTRACTTYPE_01','劳动合同', 1000, 1, 0, 0,'CONTRACTTYPE_01', '劳动合同', 0, '12350', 12350,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2078, 0, 'CONTRACTTYPE_02', 'CONTRACTTYPE_NATURE', 'systemCode.foundation.SystemCode.CONTRACTTYPE_02','聘用合同', 1000, 1, 0, 0,'CONTRACTTYPE_02', '聘用合同', 0, '12351', 12351,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2079, 0, 'CONTRACTCLASS_01', 'CONTRACTCLASS_NATURE', 'systemCode.foundation.SystemCode.CONTRACTCLASS_01','全日制', 1000, 1, 0, 0,'CONTRACTCLASS_01', '全日制', 0, '12352', 12352,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2081, 0, 'CONTRACTTIME_01', 'CONTRACTTIME_NATURE', 'systemCode.foundation.SystemCode.CONTRACTTIME_01','固定期限劳动合同', 1000, 1, 0, 0,'CONTRACTTIME_01', '固定期限劳动合同', 0, '12354', 12354,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2082, 0, 'CONTRACTTIME_02', 'CONTRACTTIME_NATURE', 'systemCode.foundation.SystemCode.CONTRACTTIME_02','无固定期限劳动合同', 1000, 1, 0, 0,'CONTRACTTIME_02', '无固定期限劳动合同', 0, '12355', 12355,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2083, 0, 'NB_GJ', 'ZHICHENG_NATURE', 'systemCode.foundation.SystemCode.NB_GJ','副高级', 1000, 1, 0, 0,'NB_GJ', '副高级', 0, '12356', 12356,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2084, 0, 'NB_ZHULIJI', 'ZHICHENG_NATURE', 'systemCode.foundation.SystemCode.NB_ZHULIJI','助理级', 1000, 1, 0, 0,'NB_ZHULIJI', '助理级', 0, '12357', 12357,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2085, 0, 'NB_YJ', 'ZHICHENG_NATURE', 'systemCode.foundation.SystemCode.NB_YJ','员级', 1000, 1, 0, 0,'NB_YJ', '员级', 0, '12358', 12358,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2087, 0, 'NB_JS', 'ZHICHENG_NATURE', 'systemCode.foundation.SystemCode.NB_JS','技师级', 1000, 1, 0, 0,'NB_JS', '技师级', 0, '12360', 12360,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2088, 0, 'NB_JG', 'ZHICHENG_NATURE', 'systemCode.foundation.SystemCode.NB_JG','技工级', 1000, 1, 0, 0,'NB_JG', '技工级', 0, '12361', 12361,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2089, 0, 'TRANSACTION_02', 'TRANSACTION_NATURE', 'systemCode.foundation.SystemCode.TRANSACTION_02','离职', 1000, 1, 0, 0,'TRANSACTION_02', '离职', 0, '12362', 12362,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2090, 0, 'TRANSACTION_01', 'TRANSACTION_NATURE', 'systemCode.foundation.SystemCode.TRANSACTION_01','岗位变动', 1000, 1, 0, 0,'TRANSACTION_01', '岗位变动', 0, '12363', 12363,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c)values(2092, 0, 'LWPQYG_01', 'YGXS', 'systemCode.foundation.SystemCode.LWPQYG_01','劳务派遣工', 1000, 1, 0, 0,'LWPQYG_01', '劳务派遣工', 0, '12365', 12365,2, '0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2093, 0, 'JSJ', 'ZHICHENJB_NATURE', 'systemCode.foundation.SystemCode.JSJ','正高级', 1000, 1, 0, 0,'JSJ', '正高级', 0, '12366', 12366,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2094, 0, 'ZLJ', 'ZHICHENJB_NATURE', 'systemCode.foundation.SystemCode.ZLJ','助理级', 1000, 1, 0, 0,'ZLJ', '助理级', 0, '12367', 12367,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2095, 0, 'GJ', 'ZHICHENJB_NATURE', 'systemCode.foundation.SystemCode.GJ','高级', 1000, 1, 0, 0,'GJ', '高级', 0, '12368', 12368,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2096, 0, 'YJ', 'ZHICHENJB_NATURE', 'systemCode.foundation.SystemCode.YJ','员级', 1000, 1, 0, 0,'YJ', '员级', 0, '12369', 12369,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2097, 0, 'ZJ', 'ZHICHENJB_NATURE', 'systemCode.foundation.SystemCode.ZJ','中级', 1000, 1, 0, 0,'ZJ', '中级', 0, '12370', 12370,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c)values(2098, 0, 'BAP', 'SYSTEM', 'systemCode.foundation.SystemCode.system_bap','BAP', 1000, 1, 0, 0,'BAP', 'BAP', 0, '12371', 12371,1, '0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c)values(2099, 0, 'S2', 'SYSTEM', 'systemCode.foundation.SystemCode.system_s2','S2', 1000, 1, 0, 0,'S2', 'S2', 0, '12372', 12372,2, '0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c)values(2100, 0, 'PIMS', 'SYSTEM', 'systemCode.foundation.SystemCode.system_pims','PIMS', 1000, 1, 0, 0,'PIMS', 'PIMS', 0, '12373', 12373,3, '0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c)values(2101, 0, 'MESIP', 'SYSTEM', 'systemCode.foundation.SystemCode.system_mesip','MESIP', 1000, 1, 0, 0,'MESIP', 'MESIP', 0, '12374', 12374,4, '0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c, memo)values(2102, 0, 'MAINFRAME', 'EXTENSION_ZONETYPE', 'systemCode.foundation.extension.zone.mainFrame','主界面', 1000, 1, 0, 0,'MAINFRAME', '主界面', 0, '12375', 12375,1, '0', '主界面');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c, memo)values(2103, 0, 'PERSONALIZE', 'EXTENSION_ZONETYPE', 'systemCode.foundation.extension.zone.personalize','个性化设置', 1000, 1, 0, 0,'PERSONALIZE', '个性化设置', 0, '12376', 12376,1, '0', '个性化设置');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c, memo)values(2104, 0, 'ROLEPERMISSION', 'EXTENSION_ZONETYPE', 'systemCode.foundation.extension.zone.role.permission','角色权限分配界面', 1000, 1, 0, 0,'ROLEPERMISSION', '角色权限分配界面', 0, '12377', 12377,1, '0', '角色权限分配界面');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c, memo)values(2105, 0, 'USERPERMISSION', 'EXTENSION_ZONETYPE', 'systemCode.foundation.extension.zone.user.permission','用户权限分配界面', 1000, 1, 0, 0,'USERPERMISSION', '用户权限分配界面', 0, '12378', 12378,1, '0', '用户权限分配界面');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2106, 0, 'DEPARTMENT_CATEGORY_001', 'DEPARTMENT_CATEGORY', 'systemCode.foundation.SystemCode.DEPARTMENT_CATEGORY_001','职能部门', 1000, 1, 0, 0,'DEPARTMENT_CATEGORY_001', '职能部门', 0, '12379', 12379,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2107, 0, 'DEPARTMENT_CATEGORY_002', 'DEPARTMENT_CATEGORY', 'systemCode.foundation.SystemCode.DEPARTMENT_CATEGORY_002','车间', 1000, 1, 0, 0,'DEPARTMENT_CATEGORY_002', '车间', 0, '12380', 12380,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c)values(2108, 0, '0', 'SECURITY_CLASS', 'systemCode.foundation.SystemCode.InfoNonDense','非密', 1000, 1, 0, 0,'0', '非密', 0, '12381', 12381,0, '0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c)values(2109, 0, '10', 'SECURITY_CLASS', 'systemCode.foundation.SystemCode.InfoGeneralSecret','一般涉密', 1000, 1, 0, 0,'10', '一般涉密', 0, '12382', 12382,1, '0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c)values(2110, 0, '20', 'SECURITY_CLASS', 'systemCode.foundation.SystemCode.InfoImportantSecret','重要涉密', 1000, 1, 0, 0,'20', '重要涉密', 0, '12383', 12383,2, '0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c)values(2111, 0, '30', 'SECURITY_CLASS', 'systemCode.foundation.SystemCode.InfoCoreSecret','核心涉密', 1000, 1, 0, 0,'30', '核心涉密', 0, '12384', 12384,3, '0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c)values(2112, 0, '5', 'SECRET_CLASS', 'systemCode.foundation.SystemCode.MenuNonDense','非密', 1000, 1, 0, 0,'5', '非密', 0, '12385', 12385,0, '0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c)values(2113, 0, '3', 'SECRET_CLASS', 'systemCode.foundation.SystemCode.MenuSecret','秘密', 1000, 1, 0, 0,'3', '秘密', 0, '12386', 12386,1, '0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c)values(2114, 0, '2', 'SECRET_CLASS', 'systemCode.foundation.SystemCode.MenuConfidential','机密', 1000, 1, 0, 0,'2', '机密', 0, '12387', 12387,2, '0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c)values(2115, 0, '6', 'SECRET_CLASS', 'systemCode.foundation.SystemCode.MenuInternalData','内部资料', 1000, 1, 0, 0,'6', '内部资料', 0, '12388', 12388,3, '0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c)values(2116, 0, '7', 'SECRET_CLASS', 'systemCode.foundation.SystemCode.MenuCoreBussinessSecret','核心商密', 1000, 1, 0, 0,'7', '核心商密', 0, '12389', 12389,4, '0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c, memo)values(2117, 0, 'bsmEquipLayer', 'BASE_CONFIG_LAYERS', 'systemCode.foundation.SystemCode.bsmEquipLayer','公用基础-设备', 1000, 1, 0, 0,'bsmEquipLayer', '公用基础-设备', 0, '12390', 12390,0, '0', '公用基础-设备');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c, memo)values(2118, 0, 'bsmWareHouseLayer', 'BASE_CONFIG_LAYERS', 'systemCode.foundation.SystemCode.bsmWareHouseLayer','公用基础-仓库', 1000, 1, 0, 0,'bsmWareHouseLayer', '公用基础-仓库', 0, '12391', 12391,1, '0', '公用基础-仓库');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c, memo)values(2119, 0, 'bsmAreaLayer', 'BASE_CONFIG_LAYERS', 'systemCode.foundation.SystemCode.bsmAreaLayer','公用基础-区域', 1000, 1, 0, 0,'bsmAreaLayer', '公用基础-区域', 0, '12392', 12392,2, '0', '公用基础-区域');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c, memo)values(2120, 0, 'aRiskAreaLayer', 'BASE_CONFIG_LAYERS', 'systemCode.foundation.SystemCode.aRiskAreaLayer','风险区域', 1000, 1, 0, 0,'aRiskAreaLayer', '风险区域', 0, '12393', 12393,3, '0', '风险区域');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c, memo)values(2121, 0, 'activityLayer', 'BASE_CONFIG_LAYERS', 'systemCode.foundation.SystemCode.activityLayer','风险活动设施', 1000, 1, 0, 0,'activityLayer', '风险活动设施', 0, '12394', 12394,4, '0', '风险活动设施');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c, memo)values(2122, 0, 'alarmFacilityLayer', 'BASE_CONFIG_LAYERS', 'systemCode.foundation.SystemCode.alarmFacilityLayer','报警设备', 1000, 1, 0, 0,'alarmFacilityLayer', '报警设备', 0, '12395', 12395,5, '0', '报警设备');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c, memo)values(2123, 0, 'envirProLayer', 'BASE_CONFIG_LAYERS', 'systemCode.foundation.SystemCode.envirProLayer','环保监测点', 1000, 1, 0, 0,'envirProLayer', '环保监测点', 0, '12396', 12396,6, '0', '环保监测点');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c, memo)values(2124, 0, 'escapLineLayer', 'BASE_CONFIG_LAYERS', 'systemCode.foundation.SystemCode.escapLineLayer','逃生路线', 1000, 1, 0, 0,'escapLineLayer', '逃生路线', 0, '12397', 12397,7, '0', '逃生路线');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c, memo)values(2125, 0, 'fireEquipLayer', 'BASE_CONFIG_LAYERS', 'systemCode.foundation.SystemCode.fireEquipLayer','火灾报警', 1000, 1, 0, 0,'fireEquipLayer', '火灾报警', 0, '12398', 12398,8, '0', '火灾报警');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c, memo)values(2126, 0, 'fireGasEquipLayer', 'BASE_CONFIG_LAYERS', 'systemCode.foundation.SystemCode.fireGasEquipLayer','可燃气报警', 1000, 1, 0, 0,'fireGasEquipLayer', '可燃气报警', 0, '12399', 12399,9, '0', '可燃气报警');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c, memo)values(2127, 0, 'hazardLayer', 'BASE_CONFIG_LAYERS', 'systemCode.foundation.SystemCode.hazardLayer','危险源', 1000, 1, 0, 0,'hazardLayer', '危险源', 0, '12400', 12400,10, '0', '危险源');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c, memo)values(2128, 0, 'inspectLineLayer', 'BASE_CONFIG_LAYERS', 'systemCode.foundation.SystemCode.inspectLineLayer','巡检线', 1000, 1, 0, 0,'inspectLineLayer', '巡检线', 0, '12401', 12401,11, '0', '巡检线');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c, memo)values(2129, 0, 'inspectPointLayer', 'BASE_CONFIG_LAYERS', 'systemCode.foundation.SystemCode.inspectPointLayer','巡检点', 1000, 1, 0, 0,'inspectPointLayer', '巡检点', 0, '12402', 12402,12, '0', '巡检点');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c, memo)values(2130, 0, 'meteGraphLayer', 'BASE_CONFIG_LAYERS', 'systemCode.foundation.SystemCode.meteGraphLayer','气象检测点', 1000, 1, 0, 0,'meteGraphLayer', '气象检测点', 0, '12403', 12403,13, '0', '气象检测点');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c, memo)values(2131, 0, 'toxicGasEquipLayer', 'BASE_CONFIG_LAYERS', 'systemCode.foundation.SystemCode.toxicGasEquipLayer','有毒气报警', 1000, 1, 0, 0,'toxicGasEquipLayer', '有毒气报警', 0, '12404', 12404,14, '0', '有毒气报警');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c, memo)values(2132, 0, 'accidentLayer', 'BASE_CONFIG_LAYERS', 'systemCode.foundation.SystemCode.accidentLayer','事故点', 1000, 1, 0, 0,'accidentLayer', '事故点', 0, '12405', 12405,15, '0', '事故点');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c, memo)values(2133, 0, 'cameraLayer', 'BASE_CONFIG_LAYERS', 'systemCode.foundation.SystemCode.cameraLayer','摄像头', 1000, 1, 0, 0,'cameraLayer', '摄像头', 0, '12406', 12406,16, '0', '摄像头');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c, memo)values(2134, 0, 'emeTeamLayer', 'BASE_CONFIG_LAYERS', 'systemCode.foundation.SystemCode.emeTeamLayer','应急队伍', 1000, 1, 0, 0,'emeTeamLayer', '应急队伍', 0, '12407', 12407,17, '0', '应急队伍');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c, memo)values(2135, 0, 'equipLayer', 'BASE_CONFIG_LAYERS', 'systemCode.foundation.SystemCode.equipLayer','应急设备', 1000, 1, 0, 0,'equipLayer', '应急设备', 0, '12408', 12408,18, '0', '应急设备');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c, memo)values(2136, 0, 'extOrgLayer', 'BASE_CONFIG_LAYERS', 'systemCode.foundation.SystemCode.extOrgLayer','外部机构', 1000, 1, 0, 0,'extOrgLayer', '外部机构', 0, '12409', 12409,19, '0', '外部机构');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c, memo)values(2137, 0, 'gatherPointLayer', 'BASE_CONFIG_LAYERS', 'systemCode.foundation.SystemCode.gatherPointLayer','紧急集合点', 1000, 1, 0, 0,'gatherPointLayer', '紧急集合点', 0, '12410', 12410,20, '0', '紧急集合点');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c, memo)values(2139, 0, 'jobTicketAreaLayer', 'BASE_CONFIG_LAYERS', 'systemCode.foundation.SystemCode.jobTicketAreaLayer','作业区域', 1000, 1, 0, 0,'jobTicketAreaLayer', '作业区域', 0, '12412', 12412,22, '0', '作业区域');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c, memo)values(2140, 0, 'hiddenRiskLayer', 'BASE_CONFIG_LAYERS', 'systemCode.foundation.SystemCode.hiddenRiskLayer','隐患', 1000, 1, 0, 0,'hiddenRiskLayer', '隐患', 0, '12413', 12413,23, '0', '隐患');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c, memo)values(2141, 0, 'emeActionLayer', 'BASE_CONFIG_LAYERS', 'systemCode.foundation.SystemCode.emeActionLayer','应急行动', 1000, 1, 0, 0,'emeActionLayer', '应急行动', 0, '12414', 12414,24, '0', '应急行动');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c, memo)values(2142, 0, 'emeSituationLayer', 'BASE_CONFIG_LAYERS', 'systemCode.foundation.SystemCode.emeSituationLayer','应急态势', 1000, 1, 0, 0,'emeSituationLayer', '应急态势', 0, '12415', 12415,25, '0', '应急态势');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c, memo)values(2144, 0, 'recAlarmLayer', 'BASE_CONFIG_LAYERS', 'systemCode.foundation.SystemCode.recAlarmLayer','接警', 1000, 1, 0, 0,'recAlarmLayer', '接警', 0, '12417', 12417,27, '0', '接警');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c, memo)values(2145, 0, 'drillIncidentLayer', 'BASE_CONFIG_LAYERS', 'systemCode.foundation.SystemCode.drillIncidentLayer','演练事件', 1000, 1, 0, 0,'drillIncidentLayer', '演练事件', 0, '12418', 12418,28, '0', '演练事件');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c, memo)values(2146, 0, 'drillActionLayer', 'BASE_CONFIG_LAYERS', 'systemCode.foundation.SystemCode.drillActionLayer','演练行动', 1000, 1, 0, 0,'drillActionLayer', '演练行动', 0, '12419', 12419,29, '0', '演练行动');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c, memo)values(2147, 0, 'drillActionInstanceLayer', 'BASE_CONFIG_LAYERS', 'systemCode.foundation.SystemCode.drillActionInstanceLayer','演练行动实例', 1000, 1, 0, 0,'drillActionInstanceLayer', '演练行动实例', 0, '12420', 12420,30, '0', '演练行动实例');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c, memo)values(2148, 0, 'drillSituationLayer', 'BASE_CONFIG_LAYERS', 'systemCode.foundation.SystemCode.drillSituationLayer','演练态势', 1000, 1, 0, 0,'drillSituationLayer', '演练态势', 0, '12421', 12421,31, '0', '演练态势');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c, memo)values(2150, 0, 'warningSignLayer', 'BASE_CONFIG_LAYERS', 'systemCode.foundation.SystemCode.warningSignLayer','警示标示', 1000, 1, 0, 0,'warningSignLayer', '警示标示', 0, '12423', 12423,33, '0', '警示标示');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c, memo)values(2151, 0, 'incidentPlanActLayer', 'BASE_CONFIG_LAYERS', 'systemCode.foundation.SystemCode.incidentPlanActLayer','应急指令', 1000, 1, 0, 0,'incidentPlanActLayer', '应急指令', 0, '12424', 12424,34, '0', '应急指令');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c, memo)values(2152, 0, 'occupationalHealthLayer', 'BASE_CONFIG_LAYERS', 'systemCode.foundation.SystemCode.occupationalHealthLayer','职业卫生', 1000, 1, 0, 0,'occupationalHealthLayer', '职业卫生', 0, '12425', 12425,35, '0', '职业卫生');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2015, 0, 'NATION_NATURE_44', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_44','满族', 1000, 1, 0, 0,'NATION_NATURE_44', '满族', 0, '12288', 12288,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2016, 0, 'NATION_NATURE_45', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_45','侗族', 1000, 1, 0, 0,'NATION_NATURE_45', '侗族', 0, '12289', 12289,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2017, 0, 'NATION_NATURE_46', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_46','瑶族', 1000, 1, 0, 0,'NATION_NATURE_46', '瑶族', 0, '12290', 12290,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2018, 0, 'NATION_NATURE_47', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_47','白族', 1000, 1, 0, 0,'NATION_NATURE_47', '白族', 0, '12291', 12291,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2019, 0, 'NATION_NATURE_48', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_48','土家族', 1000, 1, 0, 0,'NATION_NATURE_48', '土家族', 0, '12292', 12292,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2020, 0, 'NATION_NATURE_49', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_49','哈尼族', 1000, 1, 0, 0,'NATION_NATURE_49', '哈尼族', 0, '12293', 12293,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2021, 0, 'NATION_NATURE_50', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_50','哈萨克族', 1000, 1, 0, 0,'NATION_NATURE_50', '哈萨克族', 0, '12294', 12294,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2022, 0, 'NATION_NATURE_51', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_51','傣族', 1000, 1, 0, 0,'NATION_NATURE_51', '傣族', 0, '12295', 12295,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2023, 0, 'NATION_NATURE_52', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_52','黎族', 1000, 1, 0, 0,'NATION_NATURE_52', '黎族', 0, '12296', 12296,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2024, 0, 'NATION_NATURE_53', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_53','僳僳族', 1000, 1, 0, 0,'NATION_NATURE_53', '僳僳族', 0, '12297', 12297,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2025, 0, 'NATION_NATURE_54', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_54','佤族', 1000, 1, 0, 0,'NATION_NATURE_54', '佤族', 0, '12298', 12298,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2026, 0, 'NATION_NATURE_55', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_55','畲族', 1000, 1, 0, 0,'NATION_NATURE_55', '畲族', 0, '12299', 12299,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2027, 0, 'NATION_NATURE_56', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_56','高山族', 1000, 1, 0, 0,'NATION_NATURE_56', '高山族', 0, '12300', 12300,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2028, 0, 'NATION_NATURE_57', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_57','拉祜族', 1000, 1, 0, 0,'NATION_NATURE_57', '拉祜族', 0, '12301', 12301,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2029, 0, 'NATION_NATURE_58', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_58','水族', 1000, 1, 0, 0,'NATION_NATURE_58', '水族', 0, '12302', 12302,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2030, 0, 'NATION_NATURE_59', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_59','东乡族', 1000, 1, 0, 0,'NATION_NATURE_59', '东乡族', 0, '12303', 12303,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2031, 0, 'NATION_NATURE_60', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_60','纳西族', 1000, 1, 0, 0,'NATION_NATURE_60', '纳西族', 0, '12304', 12304,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2032, 0, 'NATION_NATURE_61', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_61','景颇族', 1000, 1, 0, 0,'NATION_NATURE_61', '景颇族', 0, '12305', 12305,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2033, 0, 'NATION_NATURE_62', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_62','柯尔克孜族', 1000, 1, 0, 0,'NATION_NATURE_62', '柯尔克孜族', 0, '12306', 12306,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2034, 0, 'NATION_NATURE_63', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_63','土族', 1000, 1, 0, 0,'NATION_NATURE_63', '土族', 0, '12307', 12307,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2035, 0, 'NATION_NATURE_64', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_64','达斡尔族', 1000, 1, 0, 0,'NATION_NATURE_64', '达斡尔族', 0, '12308', 12308,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2036, 0, 'NATION_NATURE_65', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_65','仫佬族', 1000, 1, 0, 0,'NATION_NATURE_65', '仫佬族', 0, '12309', 12309,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2037, 0, 'NATION_NATURE_66', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_66','羌族', 1000, 1, 0, 0,'NATION_NATURE_66', '羌族', 0, '12310', 12310,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2038, 0, 'NATION_NATURE_67', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_67','布朗族', 1000, 1, 0, 0,'NATION_NATURE_67', '布朗族', 0, '12311', 12311,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2039, 0, 'NATION_NATURE_68', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_68','撒拉族', 1000, 1, 0, 0,'NATION_NATURE_68', '撒拉族', 0, '12312', 12312,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2040, 0, 'NATION_NATURE_69', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_69','毛南族', 1000, 1, 0, 0,'NATION_NATURE_69', '毛南族', 0, '12313', 12313,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2041, 0, 'NATION_NATURE_70', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_70','哈尼族', 1000, 1, 0, 0,'NATION_NATURE_70', '哈尼族', 0, '12314', 12314,'0');

insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c, memo)values(2153, 0, 'hiddenRiskHeatAppLayer', 'BASE_CONFIG_LAYERS', 'systemCode.foundation.SystemCode.hiddenRiskHeatAppLayer','隐患热力图', 1000, 1, 0, 0,'hiddenRiskHeatAppLayer', '隐患热力图', 0, '12426', 12426,36, '0', '隐患热力图');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c, memo)values(2154, 0, 'dynamicCarLayer', 'BASE_CONFIG_LAYERS', 'systemCode.foundation.SystemCode.dynamicCarLayer','车辆定位', 1000, 1, 0, 0,'dynamicCarLayer', '车辆定位', 0, '12427', 12427,37, '0', '车辆定位');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c, memo)values(2155, 0, 'dynamicPersonLayer', 'BASE_CONFIG_LAYERS', 'systemCode.foundation.SystemCode.dynamicPersonLayer','人员定位', 1000, 1, 0, 0,'dynamicPersonLayer', '人员定位', 0, '12428', 12428,38, '0', '人员定位');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c, memo)values(2156, 0, 'electricFenceLayer', 'BASE_CONFIG_LAYERS', 'systemCode.foundation.SystemCode.electricFenceLayer','电子围栏', 1000, 1, 0, 0,'electricFenceLayer', '电子围栏', 0, '12429', 12429,39, '0', '电子围栏');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c)values(2157, 0, '01', 'APP_TYPE', 'systemCode.foundation.mobile.AppType01','默认', 1000, 1, 0, 0,'01', '默认', 0, '12430', 12430,0, '0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c)values(2158, 0, '02', 'APP_TYPE', 'systemCode.foundation.mobile.AppType02','移动视图', 1000, 1, 0, 0,'02', '移动视图', 0, '12431', 12431,1, '0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c)values(2159, 0, '03', 'APP_TYPE', 'systemCode.foundation.mobile.AppType03','外部链接', 1000, 1, 0, 0,'03', '外部链接', 0, '12432', 12432,2, '0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c)values(2160, 0, '04', 'APP_TYPE', 'systemCode.foundation.mobile.AppType04','APP跳转', 1000, 1, 0, 0,'04', 'APP跳转', 0, '12433', 12433,3, '0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c)values(2161, 0, '05', 'APP_TYPE', 'systemCode.foundation.mobile.AppType05','其他(扩展)', 1000, 1, 0, 0,'05', '其他(扩展)', 0, '12434', 12434,4, '0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c)values(2162, 0, '01', 'SCREEN_TYPE', 'systemCode.foundation.mobile.ScreenType01','默认竖屏(可以转动)', 1000, 1, 0, 0,'01', '默认竖屏(可以转动)', 0, '12435', 12435,0, '0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c)values(2163, 0, '02', 'SCREEN_TYPE', 'systemCode.foundation.mobile.ScreenType02','默认横屏(可以转动)', 1000, 1, 0, 0,'02', '默认横屏(可以转动)', 0, '12436', 12436,1, '0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c)values(2164, 0, '03', 'SCREEN_TYPE', 'systemCode.foundation.mobile.ScreenType03','锁定竖屏', 1000, 1, 0, 0,'03', '锁定竖屏', 0, '12437', 12437,2, '0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c)values(2165, 0, '04', 'SCREEN_TYPE', 'systemCode.foundation.mobile.ScreenType04','锁定横屏', 1000, 1, 0, 0,'04', '锁定横屏', 0, '12438', 12438,3, '0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2166, 0, 'COMPANY_NATURE_001', 'COMPANY_NATURE', 'systemCode.foundation.SystemCode.COMPANY_NATURE_001','国有企业', 1000, 1, 0, 0,'COMPANY_NATURE_001', '国有企业', 0, '12230', 12230,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2167, 0, 'COMPANY_NATURE_002', 'COMPANY_NATURE', 'systemCode.foundation.SystemCode.COMPANY_NATURE_002','集体企业', 1000, 1, 0, 0,'COMPANY_NATURE_002', '集体企业', 0, '12231', 12231,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2168, 0, 'COMPANY_NATURE_005', 'COMPANY_NATURE', 'systemCode.foundation.SystemCode.COMPANY_NATURE_005','私营企业', 1000, 1, 0, 0,'COMPANY_NATURE_005', '私营企业', 0, '12232', 12232,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2169, 0, 'COMPANY_NATURE_006', 'COMPANY_NATURE', 'systemCode.foundation.SystemCode.COMPANY_NATURE_006','个体户', 1000, 1, 0, 0,'COMPANY_NATURE_006', '个体户', 0, '12233', 12233,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2170, 0, 'COMPANY_NATURE_009', 'COMPANY_NATURE', 'systemCode.foundation.SystemCode.COMPANY_NATURE_009','股份有限公司', 1000, 1, 0, 0,'COMPANY_NATURE_009', '股份有限公司', 0, '12234', 12234,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2171, 0, 'COMPANY_NATURE_003', 'COMPANY_NATURE', 'systemCode.foundation.SystemCode.COMPANY_NATURE_003','联营企业', 1000, 1, 0, 0,'COMPANY_NATURE_003', '联营企业', 0, '12235', 12235,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2172, 0, 'COMPANY_NATURE_007', 'COMPANY_NATURE', 'systemCode.foundation.SystemCode.COMPANY_NATURE_007','合伙企业', 1000, 1, 0, 0,'COMPANY_NATURE_007', '合伙企业', 0, '12236', 12236,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2173, 0, 'COMPANY_NATURE_004', 'COMPANY_NATURE', 'systemCode.foundation.SystemCode.COMPANY_NATURE_004','股份合作制企业', 1000, 1, 0, 0,'COMPANY_NATURE_004', '股份合作制企业', 0, '12237', 12237,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2174, 0, 'COMPANY_NATURE_008', 'COMPANY_NATURE', 'systemCode.foundation.SystemCode.COMPANY_NATURE_008','有限责任公司', 1000, 1, 0, 0,'COMPANY_NATURE_008', '有限责任公司', 0, '12238', 12238,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2175, 0, 'POSITION_CATEGORY_001', 'POSITION_CATEGORY', 'systemCode.foundation.SystemCode.POSITION_CATEGORY_001','一线岗位', 1000, 1, 0, 0,'POSITION_CATEGORY_001', '一线岗位', 0, '12239', 12239,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2176, 0, 'POSITION_CATEGORY_003', 'POSITION_CATEGORY', 'systemCode.foundation.SystemCode.POSITION_CATEGORY_003','三线岗位', 1000, 1, 0, 0,'POSITION_CATEGORY_003', '三线岗位', 0, '12240', 12240,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2177, 0, 'POSITION_CATEGORY_002', 'POSITION_CATEGORY', 'systemCode.foundation.SystemCode.POSITION_CATEGORY_002','二线岗位', 1000, 1, 0, 0,'POSITION_CATEGORY_002', '二线岗位', 0, '12241', 12241,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2178, 0, 'EDUCATIONAL_NATURE_01', 'EDUCATIONAL_NATURE', 'systemCode.foundation.SystemCode.EDUCATIONAL_NATURE_01','小学', 1000, 1, 0, 0,'EDUCATIONAL_NATURE_01', '小学', 0, '12242', 12242,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2179, 0, 'EDUCATIONAL_NATURE_04', 'EDUCATIONAL_NATURE', 'systemCode.foundation.SystemCode.EDUCATIONAL_NATURE_04','中专', 1000, 1, 0, 0,'EDUCATIONAL_NATURE_04', '中专', 0, '12243', 12243,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2180, 0, 'EDUCATIONAL_NATURE_05', 'EDUCATIONAL_NATURE', 'systemCode.foundation.SystemCode.EDUCATIONAL_NATURE_05','大专', 1000, 1, 0, 0,'EDUCATIONAL_NATURE_05', '大专', 0, '12244', 12244,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2181, 0, 'EDUCATIONAL_NATURE_02', 'EDUCATIONAL_NATURE', 'systemCode.foundation.SystemCode.EDUCATIONAL_NATURE_02','初中', 1000, 1, 0, 0,'EDUCATIONAL_NATURE_02', '初中', 0, '12245', 12245,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2182, 0, 'EDUCATIONAL_NATURE_07', 'EDUCATIONAL_NATURE', 'systemCode.foundation.SystemCode.EDUCATIONAL_NATURE_07','研究生', 1000, 1, 0, 0,'EDUCATIONAL_NATURE_07', '研究生', 0, '12246', 12246,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2183, 0, 'EDUCATIONAL_NATURE_03', 'EDUCATIONAL_NATURE', 'systemCode.foundation.SystemCode.EDUCATIONAL_NATURE_03','高中', 1000, 1, 0, 0,'EDUCATIONAL_NATURE_03', '高中', 0, '12247', 12247,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2185, 0, 'MARRIAGE_NATURE_01', 'MARRIAGE_NATURE', 'systemCode.foundation.SystemCode.MARRIAGE_NATURE_01','未婚', 1000, 1, 0, 0,'MARRIAGE_NATURE_01', '未婚', 0, '12249', 12249,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2186, 0, 'MARRIAGE_NATURE_02', 'MARRIAGE_NATURE', 'systemCode.foundation.SystemCode.MARRIAGE_NATURE_02','已婚', 1000, 1, 0, 0,'MARRIAGE_NATURE_02', '已婚', 0, '12250', 12250,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2187, 0, 'POLITICSINFO_NATURE_02', 'POLITICSINFO_NATURE', 'systemCode.foundation.SystemCode.POLITICSINFO_NATURE_02','党员', 1000, 1, 0, 0,'POLITICSINFO_NATURE_02', '党员', 0, '12251', 12251,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2188, 0, 'POLITICSINFO_NATURE_04', 'POLITICSINFO_NATURE', 'systemCode.foundation.SystemCode.POLITICSINFO_NATURE_04','民主人士', 1000, 1, 0, 0,'POLITICSINFO_NATURE_04', '民主人士', 0, '12252', 12252,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2190, 0, 'POLITICSINFO_NATURE_01', 'POLITICSINFO_NATURE', 'systemCode.foundation.SystemCode.POLITICSINFO_NATURE_01','团员', 1000, 1, 0, 0,'POLITICSINFO_NATURE_01', '团员', 0, '12254', 12254,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2191, 0, 'HUKOUXZ_NATURE_02', 'HUKOUXZ_NATURE', 'systemCode.foundation.SystemCode.HUKOUXZ_NATURE_02','本地农村', 1000, 1, 0, 0,'HUKOUXZ_NATURE_02', '本地农村', 0, '12255', 12255,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2192, 0, 'HUKOUXZ_NATURE_03', 'HUKOUXZ_NATURE', 'systemCode.foundation.SystemCode.HUKOUXZ_NATURE_03','外地城镇', 1000, 1, 0, 0,'HUKOUXZ_NATURE_03', '外地城镇', 0, '12256', 12256,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2193, 0, 'HUKOUXZ_NATURE_04', 'HUKOUXZ_NATURE', 'systemCode.foundation.SystemCode.HUKOUXZ_NATURE_04','外地农村', 1000, 1, 0, 0,'HUKOUXZ_NATURE_04', '外地农村', 0, '12257', 12257,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2195, 0, 'NATION_NATURE_102', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_102','京族', 1000, 1, 0, 0,'NATION_NATURE_102', '京族', 0, '12259', 12259,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2196, 0, 'NATION_NATURE_78', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_78','水族', 1000, 1, 0, 0,'NATION_NATURE_78', '水族', 0, '12260', 12260,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2197, 0, 'NATION_NATURE_79', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_79','东乡族', 1000, 1, 0, 0,'NATION_NATURE_79', '东乡族', 0, '12261', 12261,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2198, 0, 'NATION_NATURE_80', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_80','纳西族', 1000, 1, 0, 0,'NATION_NATURE_80', '纳西族', 0, '12262', 12262,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2199, 0, 'NATION_NATURE_81', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_81','景颇族', 1000, 1, 0, 0,'NATION_NATURE_81', '景颇族', 0, '12263', 12263,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2201, 0, 'NATION_NATURE_84', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_84','达斡尔族', 1000, 1, 0, 0,'NATION_NATURE_84', '达斡尔族', 0, '12265', 12265,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2202, 0, 'NATION_NATURE_85', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_85','仫佬族', 1000, 1, 0, 0,'NATION_NATURE_85', '仫佬族', 0, '12266', 12266,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2203, 0, 'NATION_NATURE_87', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_87','布朗族', 1000, 1, 0, 0,'NATION_NATURE_87', '布朗族', 0, '12267', 12267,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2204, 0, 'NATION_NATURE_88', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_88','撒拉族', 1000, 1, 0, 0,'NATION_NATURE_88', '撒拉族', 0, '12268', 12268,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2205, 0, 'NATION_NATURE_90', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_90','仡佬族', 1000, 1, 0, 0,'NATION_NATURE_90', '仡佬族', 0, '12269', 12269,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2207, 0, 'NATION_NATURE_92', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_92','阿昌族', 1000, 1, 0, 0,'NATION_NATURE_92', '阿昌族', 0, '12271', 12271,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2208, 0, 'NATION_NATURE_94', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_94','塔吉克族', 1000, 1, 0, 0,'NATION_NATURE_94', '塔吉克族', 0, '12272', 12272,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2080, 0, 'CONTRACTCLASS_02', 'CONTRACTCLASS_NATURE', 'systemCode.foundation.SystemCode.CONTRACTCLASS_02','非全日制', 1000, 1, 0, 0,'CONTRACTCLASS_02', '非全日制', 0, '12353', 12353,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2086, 0, 'NB_ZJ', 'ZHICHENG_NATURE', 'systemCode.foundation.SystemCode.NB_ZJ','中级', 1000, 1, 0, 0,'NB_ZJ', '中级', 0, '12359', 12359,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c)values(2091, 0, 'LWPQYG_02', 'YGXS', 'systemCode.foundation.SystemCode.LWPQYG_02','非劳务派遣工', 1000, 1, 0, 0,'LWPQYG_02', '非劳务派遣工', 0, '12364', 12364,1, '0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c, memo)values(2138, 0, 'jobTicketLayer', 'BASE_CONFIG_LAYERS', 'systemCode.foundation.SystemCode.jobTicketLayer','作业', 1000, 1, 0, 0,'jobTicketLayer', '作业', 0, '12411', 12411,21, '0', '作业');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c, memo)values(2143, 0, 'incidentLayer', 'BASE_CONFIG_LAYERS', 'systemCode.foundation.SystemCode.incidentLayer','应急事件', 1000, 1, 0, 0,'incidentLayer', '应急事件', 0, '12416', 12416,26, '0', '应急事件');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c, memo)values(2149, 0, 'drillSituationInstanceLayer', 'BASE_CONFIG_LAYERS', 'systemCode.foundation.SystemCode.drillSituationInstanceLayer','演练态势实例', 1000, 1, 0, 0,'drillSituationInstanceLayer', '演练态势实例', 0, '12422', 12422,32, '0', '演练态势实例');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2184, 0, 'EDUCATIONAL_NATURE_06', 'EDUCATIONAL_NATURE', 'systemCode.foundation.SystemCode.EDUCATIONAL_NATURE_06','本科', 1000, 1, 0, 0,'EDUCATIONAL_NATURE_06', '本科', 0, '12248', 12248,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2189, 0, 'POLITICSINFO_NATURE_03', 'POLITICSINFO_NATURE', 'systemCode.foundation.SystemCode.POLITICSINFO_NATURE_03','群众', 1000, 1, 0, 0,'POLITICSINFO_NATURE_03', '群众', 0, '12253', 12253,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2194, 0, 'HUKOUXZ_NATURE_01', 'HUKOUXZ_NATURE', 'systemCode.foundation.SystemCode.HUKOUXZ_NATURE_01','本地城镇', 1000, 1, 0, 0,'HUKOUXZ_NATURE_01', '本地城镇', 0, '12258', 12258,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2200, 0, 'NATION_NATURE_82', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_82','柯尔克孜族', 1000, 1, 0, 0,'NATION_NATURE_82', '柯尔克孜族', 0, '12264', 12264,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,des_c)values(2206, 0, 'NATION_NATURE_91', 'NATION_NATURE', 'systemCode.foundation.SystemCode.NATION_NATURE_91','锡伯族', 1000, 1, 0, 0,'NATION_NATURE_91', '锡伯族', 0, '12270', 12270,'0');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c, memo)values(2210, 0, 'equipMonitLayer', 'BASE_CONFIG_LAYERS', 'systemCode.foundation.SystemCode.equipMonitLayer','设备监控', 1000, 1, 0, 0,'equipMonitLayer', '设备监控', 0, '2210', 2210,34, '0', '设备监控');

## 1.0.1
insert into sys_entity(id,code,name,display_name,type,module_id,cid,sys_default,memo) values(1010,'base_AppType','systemCode.base_AppType','应用类型','list','sys',1000,1,'系统默认');
insert into sys_code(id,code,name,display_name,entity_code,cid,default_flag,full_path,parent_name,lay_no,sort,memo) values(1036,'01','systemCode.01','H5(有导航条)','base_AppType',1000,1,'01','systemCode.base_AppType',1,1,'系统默认');
insert into sys_code(id,code,name,display_name,entity_code,cid,default_flag,full_path,parent_name,lay_no,sort,memo) values(1037,'02','systemCode.02','原生','base_AppType',1000,0,'02','systemCode.base_AppType',1,2,'系统默认');
insert into sys_code(id,code,name,display_name,entity_code,cid,default_flag,full_path,parent_name,lay_no,sort,memo) values(1038,'03','systemCode.03','H5(无导航条)','base_AppType',1000,0,'03','systemCode.base_AppType',1,3,'系统默认');

## 1.0.2
CREATE OR REPLACE VIEW BASE_SYSTEMCODE (ID, VERSION, CODE, TYPE, ENTITY_CODE, VALUE, VALUE_ZH_CN, CID, VALID, LEAF, DEFAULT_FLAG, FULL_PATH_NAME, PARENT_ID, LAY_NO, LAY_REC, SEQ_ID, SORT, CODE_DESA, CODE_DESB, CODE_DESC, MEMO, CREATE_STAFF_ID, MODIFY_STAFF_ID, DELETE_STAFF_ID, CREATE_TIME, MODIFY_TIME, DELETE_TIME, ATTRIBUTE) AS
  (
select
  concat(concat(s1.entity_code,'/'),s1.code) as id,
  s1.row_version as version,
  s1.code,
  s1.type,
  s1.entity_code,
  s1.name as value,
  s1.display_name as value_zh_cn,
  s1.cid,
  s1.valid,
  s1.leaf,
  s1.default_flag,
  s1.full_path_name,
  (CASE WHEN s2.id IS NULL THEN NULL
    ELSE (concat(concat(s2.entity_code,'/'),s2.code))
  END) AS parent_id,
  s1.lay_no,
  s1.lay_rec,
  s1.seq_id,
  s1.sort,
  s1.des_a as code_desa,
  s1.des_b as code_desb,
  s1.des_c as code_desc,
  s1.memo,
  NULL as create_staff_id,
  NULL as modify_staff_id,
  NULL as delete_staff_id,
  s1.create_time,
  s1.modify_time,
  NULL as delete_time,
  NULL AS ATTRIBUTE
from sys_code s1
LEFT JOIN sys_code s2 ON
  s1.parent_id = s2.id
WHERE s1.valid = 1
);
## 1.0.3
Insert into SYS_CODE
   (ID, ROW_VERSION, CODE, ENTITY_CODE, NAME,
    DISPLAY_NAME, CID, VALID, LEAF, DEFAULT_FLAG,
    FULL_PATH, FULL_PATH_NAME, LAY_NO, LAY_REC, SEQ_ID,
    SORT, DES_C, MEMO)
 Values
   (2212, 0, 'KRSLayer', 'BASE_CONFIG_LAYERS', 'systemCode.foundation.SystemCode.KRSLayer',
    '辐射监测', 1000, 1, 0, 0,
    'KRSLayer', '辐射监测', 0, '2212', 2212,
    35, '0', '辐射监测');
Insert into SYS_CODE
   (ID, ROW_VERSION, CODE, ENTITY_CODE, NAME,
    DISPLAY_NAME, CID, VALID, LEAF, DEFAULT_FLAG,
    FULL_PATH, FULL_PATH_NAME, LAY_NO, LAY_REC, SEQ_ID,
    SORT, DES_C, MEMO)
 Values
   (2214, 0, 'beaconLayer', 'BASE_CONFIG_LAYERS', 'systemCode.foundation.SystemCode.beaconLayer',
    '定位信标', 1000, 1, 0, 0,
    'beaconLayer', '定位信标', 0, '2214', 2214,
    36, '0', '定位信标');
Insert into SYS_CODE
   (ID, ROW_VERSION, CODE, ENTITY_CODE, NAME,
    DISPLAY_NAME, CID, VALID, LEAF, DEFAULT_FLAG,
    FULL_PATH, FULL_PATH_NAME, LAY_NO, LAY_REC, SEQ_ID,
    SORT, DES_C, MEMO)
 Values
   (2216, 0, 'customerXNFHAreaLayer', 'BASE_CONFIG_LAYERS', 'systemCode.foundation.SystemCode.customerXNFHAreaLayer',
    '视频区域', 1000, 1, 0, 0,
    'customerXNFHAreaLayer', '视频区域', 0, '2216', 2216,
    37, '0', '视频区域');
Insert into SYS_CODE
   (ID, ROW_VERSION, CODE, ENTITY_CODE, NAME,
    DISPLAY_NAME, CID, VALID, LEAF, DEFAULT_FLAG,
    FULL_PATH, FULL_PATH_NAME, LAY_NO, LAY_REC, SEQ_ID,
    SORT, DES_C, MEMO)
 Values
   (2218, 0, 'drivingLineLayer', 'BASE_CONFIG_LAYERS', 'systemCode.foundation.SystemCode.drivingLineLayer',
    '车辆-行车线路', 1000, 1, 0, 0,
    'drivingLineLayer', '车辆-行车线路', 0, '2218', 2218,
    37, '0', '车辆-行车线路');
Insert into SYS_CODE
   (ID, ROW_VERSION, CODE, ENTITY_CODE, NAME,
    DISPLAY_NAME, CID, VALID, LEAF, DEFAULT_FLAG,
    FULL_PATH, FULL_PATH_NAME, LAY_NO, LAY_REC, SEQ_ID,
    SORT, DES_C, MEMO)
 Values
   (2220, 0, 'loadingZoneLayer', 'BASE_CONFIG_LAYERS', 'systemCode.foundation.SystemCode.loadingZoneLayer',
    '车辆-装载区', 1000, 1, 0, 0,
    'loadingZoneLayer', '车辆-装载区', 0, '2220', 2220,
    38, '0', '车辆-装载区');
Insert into SYS_CODE
   (ID, ROW_VERSION, CODE, ENTITY_CODE, NAME,
    DISPLAY_NAME, CID, VALID, LEAF, DEFAULT_FLAG,
    FULL_PATH, FULL_PATH_NAME, LAY_NO, LAY_REC, SEQ_ID,
    SORT, DES_C, MEMO)
 Values
   (2222, 0, 'loginInLayer', 'BASE_CONFIG_LAYERS', 'systemCode.foundation.SystemCode.loginInLayer',
    '人员签到', 1000, 1, 0, 0,
    'loginInLayer', '人员签到', 0, '2222', 2222,
    39, '0', '人员签到');
Insert into SYS_CODE
   (ID, ROW_VERSION, CODE, ENTITY_CODE, NAME,
    DISPLAY_NAME, CID, VALID, LEAF, DEFAULT_FLAG,
    FULL_PATH, FULL_PATH_NAME, LAY_NO, LAY_REC, SEQ_ID,
    SORT, DES_C, MEMO)
 Values
   (2224, 0, 'monitoringAreaLayer', 'BASE_CONFIG_LAYERS', 'systemCode.foundation.SystemCode.monitoringAreaLayer',
    '监控区域', 1000, 1, 0, 0,
    'monitoringAreaLayer', '监控区域', 0, '2224', 2224,
    40, '0', '监控区域');
Insert into SYS_CODE
   (ID, ROW_VERSION, CODE, ENTITY_CODE, NAME,
    DISPLAY_NAME, CID, VALID, LEAF, DEFAULT_FLAG,
    FULL_PATH, FULL_PATH_NAME, LAY_NO, LAY_REC, SEQ_ID,
    SORT, DES_C, MEMO)
 Values
   (2226, 0, 'personnelCountingLayer', 'BASE_CONFIG_LAYERS', 'systemCode.foundation.SystemCode.personnelCountingLayer',
    '人员清点集合区域', 1000, 1, 0, 0,
    'personnelCountingLayer', '人员清点集合区域', 0, '2226', 2226,
    41, '0', '人员清点集合区域');
Insert into SYS_CODE
   (ID, ROW_VERSION, CODE, ENTITY_CODE, NAME,
    DISPLAY_NAME, CID, VALID, LEAF, DEFAULT_FLAG,
    FULL_PATH, FULL_PATH_NAME, LAY_NO, LAY_REC, SEQ_ID,
    SORT, DES_C, MEMO)
 Values
   (2228, 0, 'weatherLayer', 'BASE_CONFIG_LAYERS', 'systemCode.foundation.SystemCode.weatherLayer',
    '气象塔', 1000, 1, 0, 0,
    'weatherLayer', '气象塔', 0, '2228', 2228,
    42, '0', '气象塔');
Insert into SYS_CODE
   (ID, ROW_VERSION, CODE, ENTITY_CODE, NAME,
    DISPLAY_NAME, CID, VALID, LEAF, DEFAULT_FLAG,
    FULL_PATH, FULL_PATH_NAME, LAY_NO, LAY_REC, SEQ_ID,
    SORT, DES_C, MEMO)
 Values
   (2230, 0, 'roadLayer', 'BASE_CONFIG_LAYERS', 'systemCode.foundation.SystemCode.roadLayer',
    '行政道路', 1000, 1, 0, 0,
    'roadLayer', '行政道路', 0, '2230', 2230,
    43, '0', '行政道路');
Insert into SYS_CODE
   (ID, ROW_VERSION, CODE, ENTITY_CODE, NAME,
    DISPLAY_NAME, CID, VALID, LEAF, DEFAULT_FLAG,
    FULL_PATH, FULL_PATH_NAME, LAY_NO, LAY_REC, SEQ_ID,
    SORT, DES_C, MEMO)
 Values
   (2232, 0, 'villageLayer', 'BASE_CONFIG_LAYERS', 'systemCode.foundation.SystemCode.villageLayer',
    '行政区域', 1000, 1, 0, 0,
    'villageLayer', '行政区域', 0, '2232', 2232,
    44, '0', '行政区域');
COMMIT;

## 1.0.4
create or replace
algorithm = UNDEFINED view `base_systemcode` as (
select
    concat(concat(`s1`.`entity_code`, '/'), `s1`.`code`) as `id`,
    `s1`.`row_version` as `version`,
    `s1`.`code` as `code`,
    `s1`.`type` as `type`,
    `s1`.`entity_code` as `entity_code`,
    `s1`.`name` as `value`,
    `s1`.`display_name` as `value_zh_cn`,
    `s1`.`cid` as `cid`,
    `s1`.`valid` as `valid`,
    `s1`.`leaf` as `leaf`,
    `s1`.`default_flag` as `default_flag`,
    `s1`.`full_path_name` as `full_path_name`,
    case
        when `s2`.`id` is null then null
        else `s2`.`entity_code` + '/' + `s2`.`code`
    end as `parent_id`,
    `s1`.`lay_no` as `lay_no`,
    `s1`.`lay_rec` as `lay_rec`,
    `s1`.`seq_id` as `seq_id`,
    `s1`.`sort` as `sort`,
    `s1`.`des_a` as `code_desa`,
    `s1`.`des_b` as `code_desb`,
    `s1`.`des_c` as `code_desc`,
    `s1`.`memo` as `memo`,
    null as `create_staff_id`,
    null as `modify_staff_id`,
    null as `delete_staff_id`,
    `s1`.`create_time` as `create_time`,
    `s1`.`modify_time` as `modify_time`,
    null as `delete_time`,
    null as `ATTRIBUTE`
from
    (`sys_code` `s1`
left join `sys_code` `s2` on
    (`s1`.`parent_id` = `s2`.`id`))
where
    `s1`.`valid` = 1);

create or replace
algorithm = UNDEFINED view `base_systementity` as (
select
    `sys_entity`.`id` as `id`,
    `sys_entity`.`row_version` as `version`,
    `sys_entity`.`type` as `list_type`,
    `sys_entity`.`code` as `code`,
    `sys_entity`.`name` as `name`,
    `sys_entity`.`module_id` as `module_code`,
    `sys_entity`.`cid` as `cid`,
    `sys_entity`.`valid` as `valid`,
    `sys_entity`.`multi_flag` as `multi_flag`,
    `sys_entity`.`sys_default` as `sys_default`,
    `sys_entity`.`memo` as `memo`,
    null as `create_staff_id`,
    null as `modify_staff_id`,
    null as `delete_staff_id`,
    `sys_entity`.`create_time` as `create_time`,
    `sys_entity`.`modify_time` as `modify_time`,
    null as `delete_time`,
    null as `CODE_DESC`,
    null as `CODE_DESB`,
    null as `CODE_DESA`,
    null as `SYS_CLASS_CODE`
from
    `sys_entity`
where
    `sys_entity`.`valid` = 1);

## 1.0.5
create or replace
algorithm = UNDEFINED view `base_systemcode` as (
select
    concat(concat(`s1`.`entity_code`, '/'), `s1`.`code`) as `id`,
    `s1`.`row_version` as `version`,
    `s1`.`code` as `code`,
    `s1`.`type` as `type`,
    `s1`.`entity_code` as `entity_code`,
    `s1`.`name` as `value`,
    `s1`.`display_name` as `value_zh_cn`,
    `s1`.`cid` as `cid`,
    `s1`.`valid` as `valid`,
    `s1`.`leaf` as `leaf`,
    `s1`.`default_flag` as `default_flag`,
    `s1`.`full_path_name` as `full_path_name`,
    case
        when `s2`.`id` is null then null
        else (concat(concat(`s2`.`entity_code`,'/'),`s2`.`code`))
    end as `parent_id`,
    `s1`.`lay_no` as `lay_no`,
    `s1`.`lay_rec` as `lay_rec`,
    `s1`.`seq_id` as `seq_id`,
    `s1`.`sort` as `sort`,
    `s1`.`des_a` as `code_desa`,
    `s1`.`des_b` as `code_desb`,
    `s1`.`des_c` as `code_desc`,
    `s1`.`memo` as `memo`,
    null as `create_staff_id`,
    null as `modify_staff_id`,
    null as `delete_staff_id`,
    `s1`.`create_time` as `create_time`,
    `s1`.`modify_time` as `modify_time`,
    null as `delete_time`,
    null as `ATTRIBUTE`
from
    (`sys_code` `s1`
left join `sys_code` `s2` on
    (`s1`.`parent_id` = `s2`.`id`))
where
    `s1`.`valid` = 1);


## 1.0.6
alter table sys_entity add source varchar(10) default null;

alter table sys_entity modify column modify_time timestamp not null default current_timestamp;
alter table sys_code modify column modify_time timestamp not null default current_timestamp;
update sys_entity se set se.modify_time = current_timestamp where se.modify_time is null or se.modify_time = '0000-00-00 00:00:00.000000';
update sys_code sc set sc.modify_time = current_timestamp where sc.modify_time is null or sc.modify_time = '0000-00-00 00:00:00.000000';

## 1.0.8
create or replace
algorithm = UNDEFINED view `base_systementity` as (
select
    `sys_entity`.`id` as `id`,
    `sys_entity`.`row_version` as `version`,
    `sys_entity`.`type` as `list_type`,
    `sys_entity`.`code` as `code`,
    `sys_entity`.`name` as `name`,
    `sys_entity`.`module_id` as `module_code`,
    `sys_entity`.`cid` as `cid`,
    `sys_entity`.`valid` as `valid`,
    `sys_entity`.`multi_flag` as `multi_flag`,
    `sys_entity`.`sys_default` as `sys_default`,
    `sys_entity`.`memo` as `memo`,
    null as `create_staff_id`,
    null as `modify_staff_id`,
    null as `delete_staff_id`,
    `sys_entity`.`create_time` as `create_time`,
    `sys_entity`.`modify_time` as `modify_time`,
    null as `delete_time`,
    null as `CODE_DESC`,
    null as `CODE_DESB`,
    null as `CODE_DESA`,
    null as `SYS_CLASS_CODE`
from
    `sys_entity` where valid=1);


create or replace
algorithm = UNDEFINED view `base_systemcode` as (
select
    concat(concat(`s1`.`entity_code`, '/'), `s1`.`code`) as `id`,
    `s1`.`row_version` as `version`,
    `s1`.`code` as `code`,
    `s1`.`type` as `type`,
    `s1`.`entity_code` as `entity_code`,
    `s1`.`name` as `value`,
    `s1`.`display_name` as `value_zh_cn`,
    `s1`.`cid` as `cid`,
    `s1`.`valid` as `valid`,
    `s1`.`leaf` as `leaf`,
    `s1`.`default_flag` as `default_flag`,
    `s1`.`full_path_name` as `full_path_name`,
    case
        when `s2`.`id` is null then null
        else (concat(concat(`s2`.`entity_code`,'/'),`s2`.`code`))
    end as `parent_id`,
    `s1`.`lay_no` as `lay_no`,
    `s1`.`lay_rec` as `lay_rec`,
    `s1`.`seq_id` as `seq_id`,
    `s1`.`sort` as `sort`,
    `s1`.`des_a` as `code_desa`,
    `s1`.`des_b` as `code_desb`,
    `s1`.`des_c` as `code_desc`,
    `s1`.`memo` as `memo`,
    null as `create_staff_id`,
    null as `modify_staff_id`,
    null as `delete_staff_id`,
    `s1`.`create_time` as `create_time`,
    `s1`.`modify_time` as `modify_time`,
    null as `delete_time`,
    null as `ATTRIBUTE`
from
    (`sys_code` `s1`
left join `sys_code` `s2` on
    (`s1`.`parent_id` = `s2`.`id`) ) where
    `s1`.`valid` = 1);

## 1.0.9
insert into sys_entity(id,code,name,display_name,type,module_id,cid,sys_default,memo) values(1011,'sys_person_title','sys.sys_person_title','职称','list','sys',1000,1,'系统默认');
insert into sys_code(id,code,name,display_name,entity_code,cid,default_flag,full_path,parent_name,lay_no,sort,memo) values(1039,'elementary','sys.elementary','初级','sys_person_title',1000,1,'elementary','sys.sys_person_title',1,1,'系统默认');
insert into sys_code(id,code,name,display_name,entity_code,cid,default_flag,full_path,parent_name,lay_no,sort,memo) values(1040,'intermediate','sys.intermediate','中级','sys_person_title',1000,0,'intermediate','sys.sys_person_title',1,2,'系统默认');
insert into sys_code(id,code,name,display_name,entity_code,cid,default_flag,full_path,parent_name,lay_no,sort,memo) values(1041,'advanced','sys.advanced','高级','sys_person_title',1000,0,'advanced','sys.sys_person_title',1,3,'系统默认');

insert into sys_code(id,code,name,display_name,entity_code,cid,default_flag,full_path,parent_name,lay_no,sort,memo) values(1116,'highSecondary','systemCode.highSecondary','高中/中专','sys_education',1000,0,'highSecondary','systemCode.sys_education',1,6,'系统默认');
insert into sys_code(id,code,name,display_name,entity_code,cid,default_flag,full_path,parent_name,lay_no,sort,memo) values(1117,'middleOrOther','systemCode.middleOrOther','初中及以下','sys_education',1000,0,'middleOrOther','systemCode.sys_education',1,7,'系统默认');

## 1.1.0
delete from sys_code where id = 1006;
insert into sys_code(id,code,name,display_name,entity_code,cid,default_flag,full_path,parent_name,lay_no,sort,memo) values(1118,'leave','systemCode.leave','休假','sys_person_status',1000,0,'leave','systemCode.sys_person_status',1,3,'系统默认');
## 1.1.1
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c, memo) values
(1200, 0, 'warningObjectLayer', 'BASE_CONFIG_LAYERS', 'systemCode.foundation.SystemCode.warningObjectLayer','预警对象', 1000, 1, 0, 0,'warningObjectLayer', '预警对象', 0, '13001',13001,45, '0', '预警对象');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c, memo) values
(1201, 0, 'frequentPlaceLayer', 'BASE_CONFIG_LAYERS', 'systemCode.foundation.SystemCode.frequentPlaceLayer','常用地址', 1000, 1, 0, 0,'frequentPlaceLayer', '常用地址', 0, '13002',13002,46, '0', '常用地址');
insert into sys_code(id, row_version, code, entity_code, name,display_name, cid, valid, leaf, default_flag,full_path, full_path_name, lay_no, lay_rec, seq_id,sort, des_c, memo) values
(1203, 0, 'hazardAreaLayer', 'BASE_CONFIG_LAYERS', 'systemCode.foundation.SystemCode.hazardAreaLayer','危险源区域', 1000, 1, 0, 0,'hazardAreaLayer', '危险源区域', 0, '13004',13004,48, '0', '危险源区域');