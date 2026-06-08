## 1.0.0
/*===================================================================*/
/* Table: 多公司模式组织架构公司表(org_company)                */
/*===================================================================*/
create table if not exists org_company (
    id bigint not null comment '主键id',
    row_version bigint default 0 comment '版本号',
    code varchar(256) not null comment '公司编码',
    description varchar(512) default null comment '公司描述',
    short_name varchar(256) not null comment '公司简称',
    full_name varchar(512) not null comment '公司全称',
    full_path varchar(4096) not null comment '组织全路径',
    address varchar(512) default null comment '公司地址',
    lay_no int not null comment '组织层级',
    lay_rec varchar(4096) not null comment 'id全路径',
    sort double not null comment '同层级洗下节点顺序',
    parent_id bigint default null comment '上级公司id',
    old_id varchar(256) not null comment '老板的name',
    valid tinyint(1) default 1 comment '逻辑删除,是否有效0:无效 1:有效',
    creator varchar(32) not null default 'system' comment '创建者',
    modifier varchar(32) default null comment '修改者',
    create_time timestamp not null default current_timestamp comment '创建时间',
    modify_time timestamp not null default current_timestamp comment '修改时间',
    create_staff_id bigint(20) default null comment '创建者人员id',
    modify_staff_id bigint(20) default null comment '修改者人员id',
    primary key (id),
    unique key idx_company_old_id (old_id),
    key idx_company_code_valid (code, valid)
)
engine=innodb
default charset=utf8
comment='多公司模式组织架构公司表';
insert into org_company(id, code, short_name, full_name, lay_no, full_path, sort, old_id, lay_rec)values(1000, 'default_org_company', '默认公司', '默认公司', 0, '/默认公司', 0, 'Company_default_org_company', '1000') on duplicate key update lay_no = 0;
/*===================================================================*/
/* Table: 多公司模式组织架构部门表(org_department)                    */
/*===================================================================*/
create table if not exists org_department (
    id bigint not null comment '主键id',
    row_version bigint default 0 comment '版本号',
    code varchar(256) not null comment '部门编码',
    name varchar(256) not null comment '部门名称',
    dept_type varchar(64) default 'general' comment '部门类型 general:普通部门,emergency:应急部门',
    description varchar(512) default null comment '部门描述',
    full_path varchar(4096) not null comment '组织全路径',
    lay_rec varchar(4096) not null comment 'id全路径',
    lay_no int not null comment '组织层级',
    sort double not null comment '同层级洗下节点顺序',
    company_id bigint not null comment '所属公司id',
    parent_id bigint default null comment '上级部门id',
    valid tinyint(1) default 1 comment '逻辑删除,是否有效0:无效 1:有效',
    sys_flag tinyint(1) default 0 comment '是否是系统默认，默认为0非默认，1为默认',
    old_id varchar(256) not null comment '旧的name别名',
    leaf tinyint default 1 comment '是否是叶子节点　0:不是，1:是',
    creator varchar(32) not null default 'system' comment '创建者',
    modifier varchar(32) default null comment '修改者',
    create_time timestamp not null default current_timestamp comment '创建时间',
    modify_time timestamp not null default current_timestamp comment '修改时间',
    create_staff_id bigint(20) default null comment '创建者人员id',
    modify_staff_id bigint(20) default null comment '修改者人员id',
    primary key (id),
    unique key idx_dept_old_id (old_id),
    key idx_dept_code_valid (code, valid)
)
engine=innodb
default charset=utf8
comment='多公司模式组织架构部门表';
insert into org_department(id, code, name, dept_type, full_path, lay_rec, lay_no, sort, company_id, sys_flag, old_id)values(1, 'default_department', '虚拟部门', 'sys_department_type/general', '/默认公司/虚拟部门', '1', 1, 1000, 1000, 1, 'Department_1') on duplicate key update lay_no = 0;
/*===================================================================*/
/* Table: 多公司模式组织架构岗位表(org_position)                    */
/*===================================================================*/
create table if not exists org_position (
    id bigint not null comment '主键id',
    row_version bigint default 0 comment '版本号',
    code varchar(256) not null comment '岗位编码',
    name varchar(256) not null comment '岗位名称',
    description varchar(512) default null comment '岗位描述',
    full_path varchar(4096) not null comment '组织全路径',
    lay_rec varchar(4096) not null comment 'id全路径',
    lay_no int not null comment '组织层级',
    sort double not null comment '同层级洗下节点顺序',
    company_id bigint not null comment '所属公司id',
    dep_id bigint not null comment '关联部门id',
    parent_id bigint default null comment '上级岗位id',
    valid tinyint(1) default 1 comment '逻辑删除,是否有效0:无效 1:有效',
    sys_flag tinyint(1) default 0 comment '是否是系统默认，默认为0非默认，1为默认',
    leaf tinyint default 1 comment '是否是叶子节点　0:不是，1:是',
    old_id varchar(256) not null comment '旧的name别名',
    creator varchar(32) not null default 'system' comment '创建者',
    modifier varchar(32) default null comment '修改者',
    create_time timestamp not null default current_timestamp comment '创建时间',
    modify_time timestamp not null default current_timestamp comment '修改时间',
    create_staff_id bigint(20) default null comment '创建者人员id',
    modify_staff_id bigint(20) default null comment '修改者人员id',
    primary key (id),
    unique key idx_pos_old_id (old_id),
    key idx_pos_code_valid (code, valid)
)
engine=innodb
default charset=utf8
comment='多公司模式组织架构岗位表';
insert into org_position(id, code, name, full_path, lay_rec, lay_no, sort, company_id, dep_id, sys_flag, old_id)values(1,'default_position', '虚拟岗位', '/默认公司/虚拟岗位', '1', 1, 1000, 1000, 1, 1, 'Position_1') on  duplicate key update lay_no = 0;
/*===================================================================*/
/* Table: 多公司模式组织架构组表(org_group)                    */
/*===================================================================*/
create table if not exists org_group (
    id bigint not null comment '主键id',
    row_version bigint default 0 comment '版本号',
    code varchar(256) not null comment '组编码',
    name varchar(256) not null comment '组名称',
    description varchar(512) default null comment '组描述',
    full_path varchar(4096)  null comment '组织全路径',
    sort double not null comment '同层级洗下节点顺序',
    company_id bigint not null comment '所属公司id',
    manager_id bigint default null comment '组负责人id',
    manager_name varchar(256) default null comment '组负责人名称',
    valid tinyint(1) default 1 comment '逻辑删除,是否有效0:无效 1:有效',
    creator varchar(32) not null default 'system' comment '创建者',
    modifier varchar(32) default null comment '修改者',
    create_time timestamp not null default current_timestamp comment '创建时间',
    modify_time timestamp not null default current_timestamp comment '修改时间',
    create_staff_id bigint(20) default null comment '创建者人员id',
    modify_staff_id bigint(20) default null comment '修改者人员id',
    primary key (id),
    unique key udx_grp_org_code (company_id, code)
)
engine=innodb
default charset=utf8
comment='多公司模式组织架构组表';
/*===================================================================*/
/* Table: 多公司模式组织架构人员表(org_person)                          */
/*===================================================================*/
create table if not exists org_person (
    id bigint not null comment '主键id',
    row_version bigint default 0 comment '版本号',
    code varchar(50) not null comment '人员编号',
    name varchar(50) not null comment '人员名称(对应老版本的showname)',
    old_id varchar(50) default null comment '(老版本的唯一标识name, 格式为person_id)',
    description varchar(512) default null comment '人员描述',
    gender varchar(200) null comment '性别(female:女,male:男)',
    phone varchar(256) null comment '手机号?',
    email varchar(256) null comment '邮箱?',
    status varchar(200) null comment '状态(参考系统编码)',
    classified_level varchar(200) null comment '涉密等级(参考系统编码)',
    valid tinyint(1) default 1 comment '逻辑删除,是否有效0:无效 1:有效',
    sys_flag tinyint(1) default 0 comment '是否是系统默认，默认为0非默认，1为默认',
    main_position bigint default null comment '主岗id',
    direct_leader_id bigint default null comment '直接领导',
    grand_leader_id bigint default null comment '隔级领导',
    create_user tinyint(1) default 0 comment '是否同时创建了用户0:不是,1:是',
    creator varchar(32) not null default 'system' comment '创建者',
    modifier varchar(32) default null comment '修改者',
    create_time timestamp not null default current_timestamp comment '创建时间',
    modify_time timestamp not null default current_timestamp comment '修改时间',
    create_staff_id bigint(20) default null comment '创建者人员id',
    modify_staff_id bigint(20) default null comment '修改者人员id',
    primary key (id),
    unique key udx_persion_old_id (old_id)
)
engine=innodb
default charset=utf8
comment='多公司模式组织架构人员表';

insert into org_person(id, code, name, old_id, description, status, sys_flag, main_position)values(1, 'default_person', '虚拟人员', 'Person_1', '虚拟人员', 'sys_person_status/onWork', 1, 1) on duplicate key update description = '虚拟人员';
/*===================================================================*/
/* Table: 多公司模式组织架构岗位人员关联表(org_person_position)           */
/*===================================================================*/
create table if not exists org_person_position (
    id bigint not null comment '主键id',
    row_version bigint default 0 comment '版本号',
    position_id bigint not null  comment '岗位id',
    person_id bigint not null  comment '人员id',
    work_time date comment '上岗时间',
    off_time date comment '岗位调离时间',
    remark varchar(256) default null comment '备注',
    valid tinyint(1) default 1 comment '逻辑删除,是否有效0:无效 1:有效',
    creator varchar(32) not null default 'system' comment '创建者',
    modifier varchar(32) default null comment '修改者',
    create_time timestamp not null default current_timestamp comment '创建时间',
    modify_time timestamp not null default current_timestamp comment '修改时间',
    create_staff_id bigint(20) default null comment '创建者人员id',
    modify_staff_id bigint(20) default null comment '修改者人员id',
    primary key (id),
    key idx_person_position (valid, position_id, person_id)
)
engine=innodb
default charset=utf8
comment='多公司模式组织架构岗位人员关联表';
insert into org_person_position(id, position_id, person_id)values(1, 1, 1) on duplicate key update person_id = 1;
/*===================================================================*/
/* Table: 标签表(org_tag)           */
/*===================================================================*/
create table if not exists org_tag (
    id bigint not null comment '主键id',
    row_version bigint default 0 comment '版本号',
    tag_type varchar(32) default null  comment '标签类型',
    name varchar(128) not null comment '标签名',
    company_id bigint not null comment '公司id',
    valid tinyint(1) default 1 comment '逻辑删除,是否有效0:无效 1:有效',
    creator varchar(32) not null default 'system' comment '创建者',
    modifier varchar(32) default null comment '修改者',
    create_time timestamp not null default current_timestamp comment '创建时间',
    modify_time timestamp not null default current_timestamp comment '修改时间',
    create_staff_id bigint(20) default null comment '创建者人员id',
    modify_staff_id bigint(20) default null comment '修改者人员id',
    primary key (id),
    unique key udx_tag_cid_tag_type (company_id, name, tag_type)
)
engine=innodb
default charset=utf8
comment='多公司模式组织架构岗位人员关联表';
/*===================================================================*/
/* Table: 多公司模式组织架构部门,岗位,组负责人关联表(org_manager)           */
/*===================================================================*/
create table if not exists org_manager (
    id bigint not null comment '主键id',
    row_version bigint default 0 comment '版本号',
    org_id bigint not null  comment '组织id',
    manager_id bigint not null  comment '负责人id',
    manager_name varchar(50) not null comment '负责人姓名',
    manager_type varchar(16) not null comment '部门:department,岗位:position,组:group',
    creator varchar(32) not null default 'system' comment '创建者',
    modifier varchar(32) default null comment '修改者',
    create_time timestamp not null default current_timestamp comment '创建时间',
    modify_time timestamp not null default current_timestamp comment '修改时间',
    create_staff_id bigint(20) default null comment '创建者人员id',
    modify_staff_id bigint(20) default null comment '修改者人员id',
    primary key (id),
    unique key udx_mgr_oid_mid_mtype (org_id, manager_id, manager_type)
)
engine=innodb
default charset=utf8
comment='多公司模式组织架构负责人关联表';

/*===================================================================*/
/* Table: 多公司模式组织架构组管理人员关联表(org_group_person)           */
/*===================================================================*/
create table if not exists org_group_person (
    id bigint not null comment '主键id',
    row_version bigint default 0 comment '版本号',
    group_id bigint not null  comment '组id',
    person_id bigint not null  comment '人员id',
    valid tinyint(1) default 1 comment '逻辑删除,是否有效0:无效 1:有效',
    creator varchar(32) not null default 'system' comment '创建者',
    modifier varchar(32) default null comment '修改者',
    create_time timestamp not null default current_timestamp comment '创建时间',
    modify_time timestamp not null default current_timestamp comment '修改时间',
    create_staff_id bigint(20) default null comment '创建者人员id',
    modify_staff_id bigint(20) default null comment '修改者人员id',
    primary key (id),
    unique key udx_grp_person_ids (group_id, person_id)
)
engine=innodb
default charset=utf8
comment='多公司模式组织架构组管理人员关联表';
/*===================================================================*/
/* Table: Excel导入记录表(org_excel)           */
/*===================================================================*/
create table if not exists org_excel (
    id bigint not null comment '主键id',
    status tinyint not null  comment 'excel导入状态, 1进行中, 2成功, 3失败',
    file_name varchar(256) default null comment '文件名',
    error_file varchar(256) default null comment '错误文件名',
    error_message varchar(512) default null comment '错误消息',
    excel_type varchar(64) default null comment '类型 import, export',
    creator varchar(32) not null default 'system' comment '创建者',
    modifier varchar(32) default null comment '修改者',
    create_time timestamp not null default current_timestamp comment '创建时间',
    modify_time timestamp not null default current_timestamp comment '修改时间',
    create_staff_id bigint(20) default null comment '创建者人员id',
    modify_staff_id bigint(20) default null comment '修改者人员id',
    primary key (id)
)
engine=innodb
default charset=utf8
comment='excel导入记录表';
/*===================================================================*/
/* Table: 岗位角色表(org_position_role)           */
/*===================================================================*/
create table if not exists org_position_role (
    id bigint not null comment '主键id',
    position_id bigint not null  comment '岗位id',
    role_id bigint not null comment '角色id',
    creator varchar(32) not null default 'system' comment '创建者',
    modifier varchar(32) default null comment '修改者',
    create_time timestamp not null default current_timestamp comment '创建时间',
    modify_time timestamp not null default current_timestamp comment '修改时间',
    create_staff_id bigint(20) default null comment '创建者人员id',
    modify_staff_id bigint(20) default null comment '修改者人员id',
    primary key (id),
    unique key udx_pos_role_ids (position_id, role_id)
)
engine=innodb
default charset=utf8
comment='岗位角色表';
/*===================================================================*/
/* Table: 多公司模式组织架构部门人员关联表(org_person_department)           */
/*===================================================================*/
create table if not exists org_person_department  (
    id bigint not null comment '主键id',
    row_version bigint default 0 comment '版本号',
    dept_id bigint not null comment ' 部门id',
    person_id bigint not null comment '人员id',
    position_id bigint not null  comment '岗位id',
    valid tinyint(1) default 1 comment '逻辑删除,是否有效0:无效 1:有效',
    creator varchar(32) not null default 'system' comment '创建者',
    modifier varchar(32) default null comment '修改者',
    create_time timestamp not null default current_timestamp comment '创建时间',
    modify_time timestamp not null default current_timestamp comment '修改时间',
    create_staff_id bigint(20) default null comment '创建者人员id',
    modify_staff_id bigint(20) default null comment '修改者人员id',
    primary key (id),
    key idx_person_dept_ids (valid, dept_id, person_id, position_id)
)
engine=innodb
default charset=utf8
comment='多公司模式组织架构部门人员关联表';

insert into org_person_department(id, dept_id, position_id, person_id)values(1, 1, 1, 1) on duplicate key update person_id = 1;
/*===================================================================*/
/* Table: 多公司模式组织架构公司人员关联表(org_person_company)           */
/*===================================================================*/
create table if not exists org_person_company  (
    id bigint not null comment '主键id',
    row_version bigint default 0 comment '版本号',
    company_id bigint not null comment ' 公司id',
    person_id bigint not null comment '人员id',
    position_id bigint not null  comment '岗位id',
    valid tinyint(1) default 1 comment '逻辑删除,是否有效0:无效 1:有效',
    creator varchar(32) not null default 'system' comment '创建者',
    modifier varchar(32) default null comment '修改者',
    create_time timestamp not null default current_timestamp comment '创建时间',
    modify_time timestamp not null default current_timestamp comment '修改时间',
    create_staff_id bigint(20) default null comment '创建者人员id',
    modify_staff_id bigint(20) default null comment '修改者人员id',
    primary key (id),
    key idx_person_company_ids (valid, company_id, person_id, position_id)
)
engine=innodb
default charset=utf8
comment='多公司模式组织架构公司人员关联表';
insert into org_person_company(id, company_id, position_id, person_id)values(1, 1000, 1, 1) on duplicate key update person_id = 1;
/*==============================================================*/
/* Table:组织架构助记码(org_mnecode)                */
/*==============================================================*/
create table if not exists org_mnecode
(
    id                  bigint not null comment '主键id',
    row_version         bigint not null default 0 comment 'update时+1',
    language            varchar(510) comment '语言',
    org_id              bigint comment '组织id',
    mne_code            varchar(510),
    creator varchar(32) not null default 'system' comment '创建者',
    modifier varchar(32) default null comment '修改者',
    create_time timestamp not null default current_timestamp comment '创建时间',
    modify_time timestamp not null default current_timestamp comment '修改时间',
    create_staff_id bigint(20) default null comment '创建者人员id',
    modify_staff_id bigint(20) default null comment '修改者人员id',
    primary key(id)
)
engine=innodb
default charset=utf8
comment='菜单助记码';
## 1.0.1
alter table org_person add direct_leader_id bigint default null;
alter table org_person add grand_leader_id bigint default null;
alter table org_department add sys_flag number(1,0) default 0;
alter table org_position add sys_flag number(1,0) default 0;
alter table org_person add sys_flag number(1,0) default 0;
/*===================================================================*/
/* Table: 视图        */
/*===================================================================*/
create or replace  view base_company (id, version, code, description, short_name, name, address, sort, parent_id, lay_rec, valid, create_staff_id, modify_staff_id, delete_staff_id, create_time, modify_time, delete_time, is_default, customer_field2, customer_field1, uuid, sc_nature, email, site, fax, telephone, post_code, type) as
(
select
	id,
	row_version as version,
	code,
	description,
	short_name,
	full_name as name,
	address,
	sort,
	parent_id,
	lay_rec,
	valid,
	null as create_staff_id,
	null as modify_staff_id,
	null as delete_staff_id,
	create_time,
	modify_time,
	null as delete_time,
	1 as is_default,
	null as customer_field2,
	null as customer_field1,
	null as uuid,
	null as sc_nature,
	null as email,
	null as site,
	null as fax,
	null as telephone,
	null as post_code,
	'UNIT' as type
from org_company
);


create or replace  view base_companystaff (id, version, cid, staff_id, valid, transfer_out_deal_time, transfer_in_deal_time, transfer_out_dealer_id, transfer_in_dealer_id, edit_date, end_time, start_time) as
(
select
	id,
	row_version as version,
	company_id as cid,
	person_id as staff_id,
	valid,
	null as transfer_out_deal_time,
	null as transfer_in_deal_time,
	null as transfer_out_dealer_id,
	null as transfer_in_dealer_id,
	null as edit_date,
	null as end_time,
	null as start_time
from org_person_company
);


create or replace  view base_custom_group (id, version, code, name_cn, description, sort, cid, valid, create_staff_id, modify_staff_id, delete_staff_id, create_time, modify_time, delete_time) as
  ( select id, row_version as version, code, name as name_cn, description, sort, company_id as cid, valid, null as create_staff_id, null as modify_staff_id, null as delete_staff_id, create_time, modify_time, current_timestamp  as delete_time from org_group );


create or replace  view base_custom_groupmember (id, version, related_custom_group_id, related_staff_id, valid, create_staff_id, modify_staff_id, delete_staff_id, create_time, modify_time, cid, delete_time) as
  ( select gp.id as id, gp.row_version as version, gp.group_id as related_custom_group_id, gp.person_id as related_staff_id, gp.valid as valid, gp.creator as create_staff_id, gp.modifier as modify_staff_id, null as delete_staff_id, gp.create_time, gp.modify_time, g.company_id as cid, null as delete_time from org_group_person gp left join org_group g on gp.group_id = g.id );


create or replace  view base_department (id, version, code, name, description, full_path_name, lay_no, lay_rec, sort, cid, parent_id, valid, leaf, create_staff_id, modify_staff_id, delete_staff_id, create_time, modify_time, delete_time, staff_id, objparame, objparamd, objparamc, objparamb, objparama, res_scparame, res_scparamd, res_scparamc, res_scparamb, res_scparama, charparame, charparamd, charparamc, charparamb, charparama, numberparame, numberparamd, numberparamc, numberparamb, numberparama, dateparame, dateparamd, dateparamc, dateparamb, dateparama, integerparame, integerparamd, integerparamc, integerparamb, integerparama, customer_field2, customer_field1, sc_nature, uuid, is_virtual) as
(
select
	id,
	row_version as version,
	code,
	name,
	description,
	full_path as full_path_name,
	lay_no,
	lay_rec,
	sort,
	company_id as cid,
	parent_id,
	valid,
	leaf,
	null as create_staff_id,
	null as modify_staff_id,
	null as delete_staff_id,
	create_time,
	modify_time,
	null as delete_time,
	null as staff_id,
	null as objparame,
	null as objparamd,
	null as objparamc,
	null as objparamb,
	null as objparama,
	null as res_scparame,
	null as res_scparamd,
	null as res_scparamc,
	null as res_scparamb,
	null as res_scparama,
	null as charparame,
	null as charparamd,
	null as charparamc,
	null as charparamb,
	null as charparama,
	null as numberparame,
	null as numberparamd,
	null as numberparamc,
	null as numberparamb,
	null as numberparama,
	null as dateparame,
	null as dateparamd,
	null as dateparamc,
	null as dateparamb,
	null as dateparama,
	null as integerparame,
	null as integerparamd,
	null as integerparamc,
	null as integerparamb,
	null as integerparama,
	null as customer_field2,
	null as customer_field1,
	null as sc_nature,
	null as uuid,
	sys_flag as is_virtual
from org_department
);

create or replace  view base_departmentwork (id, version, department_id, staff_id, valid, transfer_out_deal_time, transfer_in_deal_time, transfer_out_dealer_id, transfer_in_dealer_id, edit_date, end_time, start_time) as
(
select
	id,
	row_version as version,
	dept_id as department_id,
	person_id as staff_id,
	valid,
	null as transfer_out_deal_time,
	null as transfer_in_deal_time,
	null as transfer_out_dealer_id,
	null as transfer_in_dealer_id,
	null as edit_date,
	null as end_time,
	null as start_time
from org_person_department
);

create or replace  view base_position (id, version, code, name, description, full_path_name, lay_no, lay_rec, sort, cid, department_id, parent_id, valid, leaf, create_staff_id, modify_staff_id, delete_staff_id, create_time, modify_time, delete_time, objparame, objparamd, objparamc, objparamb, objparama, res_scparame, res_scparamd, res_scparamc, res_scparamb, res_scparama, charparame, charparamd, charparamc, charparamb, charparama, numberparame, numberparamd, numberparamc, numberparamb, numberparama, dateparame, dateparamd, dateparamc, dateparamb, dateparama, integerparame, integerparamd, integerparamc, integerparamb, integerparama, uuid, is_virtual, sc_nature) as
(
select
	id,
	row_version as version,
	code,
	name,
	description,
	full_path as full_path_name,
	lay_no,
	lay_rec,
	sort,
	company_id as cid,
	dep_id as department_id,
	parent_id,
	valid,
	leaf,
	null as create_staff_id,
	null as modify_staff_id,
	null as delete_staff_id,
	create_time,
	modify_time,
	null as delete_time,
	null as objparame,
	null as objparamd,
	null as objparamc,
	null as objparamb,
	null as objparama,
	null as res_scparame,
	null as res_scparamd,
	null as res_scparamc,
	null as res_scparamb,
	null as res_scparama,
	null as charparame,
	null as charparamd,
	null as charparamc,
	null as charparamb,
	null as charparama,
	null as numberparame,
	null as numberparamd,
	null as numberparamc,
	null as numberparamb,
	null as numberparama,
	null as dateparame,
	null as dateparamd,
	null as dateparamc,
	null as dateparamb,
	null as dateparama,
	null as integerparame,
	null as integerparamd,
	null as integerparamc,
	null as integerparamb,
	null as integerparama,
	null as uuid,
	sys_flag as is_virtual,
	null as sc_nature
from org_position
);

create or replace  view base_positionwork (id, version, position_id, staff_id, start_time, valid, transfer_out_deal_time, transfer_in_deal_time, transfer_out_dealer_id, transfer_in_dealer_id, edit_date, main_posi_flag, parent_pw_id, transfer_out_des, transfer_in_des, end_time) as
(
select
	id,
	row_version as version,
	position_id,
	person_id as staff_id,create or replace  view base_staff_mnecode as (
select
	m.id as id,
	m.row_version as version,
	m.org_id as staff,
	m.mne_code
from
	org_mnecode m
inner join org_person p on
	m.org_id = p.id );

create or replace  view base_department_mnecode as (
select
	m.id as id,
	m.row_version as version,
	m.org_id as department,
	m.mne_code
from
	org_mnecode m
inner join org_department d on
	m.org_id = d.id );

create or replace  view base_position_mnecode as (
select
	m.id as id,
	m.row_version as version,
	m.org_id as position,
	m.mne_code
from
	org_mnecode m
inner join org_position p on
	m.org_id = p.id );

create or replace  view base_custom_group_mnecode as (
select
	m.id as id,
	m.row_version as version,
	m.org_id as custom_group,
	m.mne_code
from
	org_mnecode m
inner join org_group g on
	m.org_id = g.id );

create or replace view base_linkinfo as (
select
  id as base_staff_id,
  phone as mobile,
  null as jabber,
  email
from org_person
);

create or replace view base_workinfo as (
select
  null as worktime,
  null as jzkst,
  null as jgssj,
  null as lrsj,
  null as rzsj,
  null as zzsj,
  null as conpanyname,
  null as bigdepartment,
  null as department,
  null as positionname,
  null as positiontype,
  null as zwmc,
  null as gsemail,
  null as zkxnh,
  null as zxhm,
  null as fjh,
  null as txlbz,
  null as fjhm,
  op.id as base_staff_id,
  null as yjzzsj
from org_person op
);

## 1.0.2
create or replace  view base_staff (id, version, code, name, sex, mobile, email, work_status, security_class, valid, user_id, main_position_id, create_staff_id, modify_staff_id, delete_staff_id, create_time, modify_time, delete_time, memo, customer_field2, customer_field1, uuid, sort, sys_flag, higher_leader_staff_id, leader_staff_id, ygxs, hukouxingzhi, hukoudi, dangandi, degree, educational, profession, biye_time, school, foreign_language, compute_level, height, virtual_mobile, politics_info, nation, marriage, id_card, native_place, birthday) as
  (
select
	op.id as id,
	row_version as version,
	code,
	name,
	gender as sex,
	phone as mobile,
	email,
	status as work_status,
	classified_level as security_class,
	op.valid as valid,
	au.id as user_id,
	main_position as main_position_id,
	null as create_staff_id,
	null as modify_staff_id,
	null as delete_staff_id,
	op.create_time as create_time,
	op.modify_time as modify_time,
	null as delete_time,
	op.description  as memo,
	null as customer_field2,
	null as customer_field1,
	null as uuid,
	null as sort,
	op.sys_flag as sys_flag,
	op.grand_leader_id as higher_leader_staff_id,
	op.direct_leader_id as leader_staff_id,
	null as ygxs,
	null as hukouxingzhi,
	null as hukoudi,
	null as dangandi,
	null as degree,
	null as educational,
	null as profession,
	null as biye_time,
	null as school,
	null as foreign_language,
	null as compute_level,
	null as height,
	null as virtual_mobile,
	null as politics_info,
	null as nation,
	null as marriage,
	null as id_card,
	null as native_place,
	null as birthday
from
	org_person op
left outer join
	auth_user au
on op.id=au.person_id and au.valid = 1
);
	work_time as start_time,
	valid,
	null as transfer_out_deal_time,
	null as transfer_in_deal_time,
	null as transfer_out_dealer_id,
	null as transfer_in_dealer_id,
	null as edit_date,
	null as main_posi_flag,
	null as parent_pw_id,
	null as transfer_out_des,
	null as transfer_in_des,
	null as end_time
from org_person_position
);


create or replace  view base_roleposition (id, version, position_id, role_id, valid) as ( select id, 0 as version, position_id, role_id, 1 as valid from org_position_role );

create or replace  view base_staff (id, version, code, name, sex, mobile, email, work_status, security_class, valid, user_id, main_position_id, create_staff_id, modify_staff_id, delete_staff_id, create_time, modify_time, delete_time, memo, customer_field2, customer_field1, uuid, sort, sys_flag, higher_leader_staff_id, leader_staff_id, ygxs, hukouxingzhi, hukoudi, dangandi, degree, educational, profession, biye_time, school, foreign_language, compute_level, height, virtual_mobile, politics_info, nation, marriage, id_card, native_place, birthday) as
(
select
	op.id as id,
	row_version as version,
	code,
	name,
	gender as sex,
	phone as mobile,
	email,
	status as work_status,
	classified_level as security_class,
	op.valid as valid,
	au.id as user_id,
	main_position as main_position_id,
	null as create_staff_id,
	null as modify_staff_id,
	null as delete_staff_id,
	op.create_time as create_time,
	op.modify_time as modify_time,
	null as delete_time,
	op.description  as memo,
	null as customer_field2,
	null as customer_field1,
	null as uuid,
	null as sort,
	op.sys_flag as sys_flag,
	null as higher_leader_staff_id,
	null as leader_staff_id,
	null as ygxs,
	null as hukouxingzhi,
	null as hukoudi,
	null as dangandi,
	null as degree,
	null as educational,
	null as profession,
	null as biye_time,
	null as school,
	null as foreign_language,
	null as compute_level,
	null as height,
	null as virtual_mobile,
	null as politics_info,
	null as nation,
	null as marriage,
	null as id_card,
	null as native_place,
	null as birthday
from
	org_person op
left outer join
	auth_user au
on op.id=au.person_id and au.valid = 1
);

create or replace  view base_staff_mnecode as (
select
	m.id as id,
	m.row_version as version,
	m.org_id as staff,
	m.mne_code
from
	org_mnecode m
inner join org_person p on
	m.org_id = p.id );

create or replace  view base_department_mnecode as (
select
	m.id as id,
	m.row_version as version,
	m.org_id as department,
	m.mne_code
from
	org_mnecode m
inner join org_department d on
	m.org_id = d.id );

create or replace  view base_position_mnecode as (
select
	m.id as id,
	m.row_version as version,
	m.org_id as position,
	m.mne_code
from
	org_mnecode m
inner join org_position p on
	m.org_id = p.id );

create or replace  view base_custom_group_mnecode as (
select
	m.id as id,
	m.row_version as version,
	m.org_id as custom_group,
	m.mne_code
from
	org_mnecode m
inner join org_group g on
	m.org_id = g.id );

create or replace view base_linkinfo as (
select
  id as base_staff_id,
  phone as mobile,
  null as jabber,
  email
from org_person
);

create or replace view base_workinfo as (
select
  null as worktime,
  null as jzkst,
  null as jgssj,
  null as lrsj,
  null as rzsj,
  null as zzsj,
  null as conpanyname,
  null as bigdepartment,
  null as department,
  null as positionname,
  null as positiontype,
  null as zwmc,
  null as gsemail,
  null as zkxnh,
  null as zxhm,
  null as fjh,
  null as txlbz,
  null as fjhm,
  op.id as base_staff_id,
  null as yjzzsj
from org_person op
);

## 1.0.2
create or replace  view base_staff (id, version, code, name, sex, mobile, email, work_status, security_class, valid, user_id, main_position_id, create_staff_id, modify_staff_id, delete_staff_id, create_time, modify_time, delete_time, memo, customer_field2, customer_field1, uuid, sort, sys_flag, higher_leader_staff_id, leader_staff_id, ygxs, hukouxingzhi, hukoudi, dangandi, degree, educational, profession, biye_time, school, foreign_language, compute_level, height, virtual_mobile, politics_info, nation, marriage, id_card, native_place, birthday) as
  (
select
	op.id as id,
	row_version as version,
	code,
	name,
	gender as sex,
	phone as mobile,
	email,
	status as work_status,
	classified_level as security_class,
	op.valid as valid,
	au.id as user_id,
	main_position as main_position_id,
	null as create_staff_id,
	null as modify_staff_id,
	null as delete_staff_id,
	op.create_time as create_time,
	op.modify_time as modify_time,
	null as delete_time,
	op.description  as memo,
	null as customer_field2,
	null as customer_field1,
	null as uuid,
	null as sort,
	op.sys_flag as sys_flag,
	op.grand_leader_id as higher_leader_staff_id,
	op.direct_leader_id as leader_staff_id,
	null as ygxs,
	null as hukouxingzhi,
	null as hukoudi,
	null as dangandi,
	null as degree,
	null as educational,
	null as profession,
	null as biye_time,
	null as school,
	null as foreign_language,
	null as compute_level,
	null as height,
	null as virtual_mobile,
	null as politics_info,
	null as nation,
	null as marriage,
	null as id_card,
	null as native_place,
	null as birthday
from
	org_person op
left outer join
	auth_user au
on op.id=au.person_id and au.valid = 1
);

## 1.0.3

/*==============================================================*/
/* Table:公司助记码(org_company_mnecode)                */
/*==============================================================*/
create table if not exists org_company_mnecode
(
    id                  bigint not null comment '主键id',
    row_version         bigint not null default 0 comment 'update时+1',
    language            varchar(510) comment '语言',
    mne_code            varchar(510) comment '助记码',
    company_id          bigint comment '公司id',
    company_short_name  varchar(510) comment '公司简称',
    creator varchar(32) default null comment '创建者',
    modifier varchar(32) default null comment '修改者',
    create_time timestamp comment '创建时间',
    modify_time timestamp comment '修改时间',
    create_staff_id bigint(20) default null comment '创建者人员id',
    modify_staff_id bigint(20) default null comment '修改者人员id',
    primary key(id)
)
engine=innodb
default charset=utf8
comment='公司助记码';

/*==============================================================*/
/* Table:部门助记码(org_department_mnecode)                */
/*==============================================================*/
create table if not exists org_department_mnecode
(
    id                  bigint not null comment '主键id',
    row_version         bigint not null default 0 comment 'update时+1',
    language            varchar(510) comment '语言',
    mne_code            varchar(510) comment '助记码',
    dept_id          bigint comment '部门id',
    dept_name  varchar(510) comment '部门名称',
    creator varchar(32) default null comment '创建者',
    modifier varchar(32) default null comment '修改者',
    create_time timestamp comment '创建时间',
    modify_time timestamp comment '修改时间',
    create_staff_id bigint(20) default null comment '创建者人员id',
    modify_staff_id bigint(20) default null comment '修改者人员id',
    primary key(id)
)
engine=innodb
default charset=utf8
comment='部门助记码';

/*==============================================================*/
/* Table:岗位助记码(org_position_mnecode)                */
/*==============================================================*/
create table if not exists org_position_mnecode
(
    id                  bigint not null comment '主键id',
    row_version         bigint not null default 0 comment 'update时+1',
    language            varchar(510) comment '语言',
    mne_code            varchar(510) comment '助记码',
    position_id          bigint comment '岗位id',
    position_name  varchar(510) comment '岗位名称',
    creator varchar(32) default null comment '创建者',
    modifier varchar(32) default null comment '修改者',
    create_time timestamp comment '创建时间',
    modify_time timestamp comment '修改时间',
    create_staff_id bigint(20) default null comment '创建者人员id',
    modify_staff_id bigint(20) default null comment '修改者人员id',
    primary key(id)
)
engine=innodb
default charset=utf8
comment='岗位助记码';

/*==============================================================*/
/* Table:人员助记码(org_person_mnecode)                */
/*==============================================================*/
create table if not exists org_person_mnecode
(
    id                  bigint not null comment '主键id',
    row_version         bigint not null default 0 comment 'update时+1',
    language            varchar(510) comment '语言',
    mne_code            varchar(510) comment '助记码',
    person_id          bigint comment '人员id',
    person_name  varchar(510) comment '人员名称',
    creator varchar(32) default null comment '创建者',
    modifier varchar(32) default null comment '修改者',
    create_time timestamp comment '创建时间',
    modify_time timestamp comment '修改时间',
    create_staff_id bigint(20) default null comment '创建者人员id',
    modify_staff_id bigint(20) default null comment '修改者人员id',
    primary key(id)
)
engine=innodb
default charset=utf8
comment='人员助记码';


CREATE OR REPLACE VIEW base_staff_mnecode AS (
SELECT
	m.id AS id,
	m.row_version AS version,
	m.person_id AS staff,
	m.mne_code
FROM
	org_person_mnecode m);

CREATE OR REPLACE VIEW base_department_mnecode AS (
SELECT
	m.id AS id,
	m.row_version AS version,
	m.dept_id AS department,
	m.mne_code
FROM
	org_department_mnecode m);

CREATE OR REPLACE VIEW base_position_mnecode AS (
SELECT
	m.id AS id,
	m.row_version AS version,
	m.position_id AS position,
	m.mne_code
FROM
	org_position_mnecode m);

## 1.0.4

create or replace algorithm = UNDEFINED view `base_company` as (
select
    `org_company`.`id` as `id`,
    `org_company`.`row_version` as `version`,
    `org_company`.`code` as `code`,
    `org_company`.`description` as `description`,
    `org_company`.`short_name` as `short_name`,
    `org_company`.`full_name` as `name`,
    `org_company`.`address` as `address`,
    `org_company`.`sort` as `sort`,
    `org_company`.`parent_id` as `parent_id`,
    `org_company`.`lay_rec` as `lay_rec`,
    `org_company`.`valid` as `valid`,
    null as `create_staff_id`,
    null as `modify_staff_id`,
    null as `delete_staff_id`,
    `org_company`.`create_time` as `create_time`,
    `org_company`.`modify_time` as `modify_time`,
    null as `delete_time`,
    1 as `IS_DEFAULT`,
    null as `CUSTOMER_FIELD2`,
    null as `CUSTOMER_FIELD1`,
    null as `UUID`,
    null as `SC_NATURE`,
    null as `EMAIL`,
    null as `SITE`,
    null as `FAX`,
    null as `TELEPHONE`,
    null as `POST_CODE`,
    'UNIT' as `TYPE`
from
    `org_company`);

create or replace
algorithm = UNDEFINED view `base_companystaff` as (
select
    `org_person_company`.`id` as `id`,
    `org_person_company`.`row_version` as `version`,
    `org_person_company`.`company_id` as `cid`,
    `org_person_company`.`person_id` as `staff_id`,
    `org_person_company`.`valid` as `valid`,
    null as `TRANSFER_OUT_DEAL_TIME`,
    null as `TRANSFER_IN_DEAL_TIME`,
    null as `TRANSFER_OUT_DEALER_ID`,
    null as `TRANSFER_IN_DEALER_ID`,
    null as `EDIT_DATE`,
    null as `END_TIME`,
    null as `START_TIME`
from
    `org_person_company`);

create or replace
algorithm = UNDEFINED view `base_custom_group` as (
select
    `org_group`.`id` as `id`,
    `org_group`.`row_version` as `version`,
    `org_group`.`code` as `code`,
    `org_group`.`name` as `name_cn`,
    `org_group`.`description` as `description`,
    `org_group`.`sort` as `sort`,
    `org_group`.`company_id` as `cid`,
    `org_group`.`valid` as `valid`,
    null as `create_staff_id`,
    null as `modify_staff_id`,
    null as `delete_staff_id`,
    `org_group`.`create_time` as `create_time`,
    `org_group`.`modify_time` as `modify_time`,
    current_timestamp() as `delete_time`
from
    `org_group`);

create or replace
algorithm = UNDEFINED view `base_custom_group_mnecode` as (
select
    `m`.`ID` as `id`,
    `m`.`row_version` as `version`,
    `m`.`ORG_ID` as `custom_group`,
    `m`.`MNE_CODE` as `mne_code`
from
    (`org_mnecode` `m`
join `org_group` `g` on
    (`m`.`ORG_ID` = `g`.`id`)));

create or replace
algorithm = UNDEFINED view `base_custom_groupmember` as (
select
    `gp`.`id` as `id`,
    `gp`.`row_version` as `version`,
    `gp`.`group_id` as `related_custom_group_id`,
    `gp`.`person_id` as `related_staff_id`,
    `gp`.`valid` as `valid`,
    `gp`.`creator` as `create_staff_id`,
    `gp`.`modifier` as `modify_staff_id`,
    null as `delete_staff_id`,
    `gp`.`create_time` as `create_time`,
    `gp`.`modify_time` as `modify_time`,
    `g`.`company_id` as `cid`,
    null as `delete_time`
from
    (`org_group_person` `gp`
left join `org_group` `g` on
    (`gp`.`group_id` = `g`.`id`)));

create or replace
algorithm = UNDEFINED view `base_department` as (
select
    `org_department`.`id` as `id`,
    `org_department`.`row_version` as `version`,
    `org_department`.`code` as `code`,
    `org_department`.`name` as `name`,
    `org_department`.`description` as `description`,
    `org_department`.`full_path` as `full_path_name`,
    `org_department`.`lay_no` as `lay_no`,
    `org_department`.`lay_rec` as `lay_rec`,
    `org_department`.`sort` as `sort`,
    `org_department`.`company_id` as `cid`,
    `org_department`.`parent_id` as `parent_id`,
    `org_department`.`valid` as `valid`,
    `org_department`.`leaf` as `leaf`,
    null as `create_staff_id`,
    null as `modify_staff_id`,
    null as `delete_staff_id`,
    `org_department`.`create_time` as `create_time`,
    `org_department`.`modify_time` as `modify_time`,
    null as `delete_time`,
    null as `staff_id`,
    null as `OBJPARAME`,
    null as `OBJPARAMD`,
    null as `OBJPARAMC`,
    null as `OBJPARAMB`,
    null as `OBJPARAMA`,
    null as `RES_SCPARAME`,
    null as `RES_SCPARAMD`,
    null as `RES_SCPARAMC`,
    null as `RES_SCPARAMB`,
    null as `RES_SCPARAMA`,
    null as `CHARPARAME`,
    null as `CHARPARAMD`,
    null as `CHARPARAMC`,
    null as `CHARPARAMB`,
    null as `CHARPARAMA`,
    null as `NUMBERPARAME`,
    null as `NUMBERPARAMD`,
    null as `NUMBERPARAMC`,
    null as `NUMBERPARAMB`,
    null as `NUMBERPARAMA`,
    null as `DATEPARAME`,
    null as `DATEPARAMD`,
    null as `DATEPARAMC`,
    null as `DATEPARAMB`,
    null as `DATEPARAMA`,
    null as `INTEGERPARAME`,
    null as `INTEGERPARAMD`,
    null as `INTEGERPARAMC`,
    null as `INTEGERPARAMB`,
    null as `INTEGERPARAMA`,
    null as `CUSTOMER_FIELD2`,
    null as `CUSTOMER_FIELD1`,
    null as `SC_NATURE`,
    null as `UUID`,
    `org_department`.`sys_flag` as `IS_VIRTUAL`
from
    `org_department`);

create or replace
algorithm = UNDEFINED view `base_department_mnecode` as (
select
    `m`.`ID` as `id`,
    `m`.`row_version` as `version`,
    `m`.`ORG_ID` as `department`,
    `m`.`MNE_CODE` as `mne_code`
from
    (`org_mnecode` `m`
join `org_department` `d` on
    (`m`.`ORG_ID` = `d`.`id`)));

create or replace
algorithm = UNDEFINED view `base_departmentwork` as (
select
    `org_person_department`.`id` as `id`,
    `org_person_department`.`row_version` as `version`,
    `org_person_department`.`dept_id` as `department_id`,
    `org_person_department`.`person_id` as `staff_id`,
    `org_person_department`.`valid` as `valid`,
    null as `TRANSFER_OUT_DEAL_TIME`,
    null as `TRANSFER_IN_DEAL_TIME`,
    null as `TRANSFER_OUT_DEALER_ID`,
    null as `TRANSFER_IN_DEALER_ID`,
    null as `EDIT_DATE`,
    null as `END_TIME`,
    null as `START_TIME`
from
    `org_person_department`);

create or replace
algorithm = UNDEFINED view `base_linkinfo` as (
select
    `org_person`.`id` as `base_staff_id`,
    `org_person`.`phone` as `mobile`,
    null as `jabber`,
    `org_person`.`email` as `email`
from
    `org_person`);

create or replace
algorithm = UNDEFINED view `base_position` as (
select
    `org_position`.`id` as `id`,
    `org_position`.`row_version` as `version`,
    `org_position`.`code` as `code`,
    `org_position`.`name` as `name`,
    `org_position`.`description` as `description`,
    `org_position`.`full_path` as `full_path_name`,
    `org_position`.`lay_no` as `lay_no`,
    `org_position`.`lay_rec` as `lay_rec`,
    `org_position`.`sort` as `sort`,
    `org_position`.`company_id` as `cid`,
    `org_position`.`dep_id` as `department_id`,
    `org_position`.`parent_id` as `parent_id`,
    `org_position`.`valid` as `valid`,
    `org_position`.`leaf` as `leaf`,
    null as `create_staff_id`,
    null as `modify_staff_id`,
    null as `delete_staff_id`,
    `org_position`.`create_time` as `create_time`,
    `org_position`.`modify_time` as `modify_time`,
    null as `delete_time`,
    null as `OBJPARAME`,
    null as `OBJPARAMD`,
    null as `OBJPARAMC`,
    null as `OBJPARAMB`,
    null as `OBJPARAMA`,
    null as `RES_SCPARAME`,
    null as `RES_SCPARAMD`,
    null as `RES_SCPARAMC`,
    null as `RES_SCPARAMB`,
    null as `RES_SCPARAMA`,
    null as `CHARPARAME`,
    null as `CHARPARAMD`,
    null as `CHARPARAMC`,
    null as `CHARPARAMB`,
    null as `CHARPARAMA`,
    null as `NUMBERPARAME`,
    null as `NUMBERPARAMD`,
    null as `NUMBERPARAMC`,
    null as `NUMBERPARAMB`,
    null as `NUMBERPARAMA`,
    null as `DATEPARAME`,
    null as `DATEPARAMD`,
    null as `DATEPARAMC`,
    null as `DATEPARAMB`,
    null as `DATEPARAMA`,
    null as `INTEGERPARAME`,
    null as `INTEGERPARAMD`,
    null as `INTEGERPARAMC`,
    null as `INTEGERPARAMB`,
    null as `INTEGERPARAMA`,
    null as `UUID`,
    `org_position`.`sys_flag` as `IS_VIRTUAL`,
    null as `SC_NATURE`
from
    `org_position`);

create or replace
algorithm = UNDEFINED view `base_position_mnecode` as (
select
    `m`.`ID` as `id`,
    `m`.`row_version` as `version`,
    `m`.`ORG_ID` as `position`,
    `m`.`MNE_CODE` as `mne_code`
from
    (`org_mnecode` `m`
join `org_position` `p` on
    (`m`.`ORG_ID` = `p`.`id`)));

create or replace
algorithm = UNDEFINED view `base_positionwork` as (
select
    `org_person_position`.`id` as `id`,
    `org_person_position`.`row_version` as `version`,
    `org_person_position`.`position_id` as `position_id`,
    `org_person_position`.`person_id` as `staff_id`,
    `org_person_position`.`work_time` as `start_time`,
    `org_person_position`.`valid` as `valid`,
    null as `TRANSFER_OUT_DEAL_TIME`,
    null as `TRANSFER_IN_DEAL_TIME`,
    null as `TRANSFER_OUT_DEALER_ID`,
    null as `TRANSFER_IN_DEALER_ID`,
    null as `EDIT_DATE`,
    null as `MAIN_POSI_FLAG`,
    null as `PARENT_PW_ID`,
    null as `TRANSFER_OUT_DES`,
    null as `TRANSFER_IN_DES`,
    null as `END_TIME`
from
    `org_person_position`);

create or replace
algorithm = UNDEFINED view `base_roleposition` as (
select
    `org_position_role`.`id` as `id`,
    0 as `version`,
    `org_position_role`.`position_id` as `position_id`,
    `org_position_role`.`role_id` as `role_id`,
    1 as `valid`
from
    `org_position_role`);

create or replace
algorithm = UNDEFINED view `base_staff` as (
select
    `op`.`id` as `id`,
    `op`.`row_version` as `version`,
    `op`.`code` as `code`,
    `op`.`name` as `name`,
    `op`.`gender` as `SEX`,
    `op`.`phone` as `mobile`,
    `op`.`email` as `email`,
    `op`.`status` as `work_status`,
    `op`.`classified_level` as `security_class`,
    `op`.`valid` as `valid`,
    `au`.`id` as `user_id`,
    `op`.`main_position` as `main_position_id`,
    null as `create_staff_id`,
    null as `modify_staff_id`,
    null as `delete_staff_id`,
    `op`.`create_time` as `create_time`,
    `op`.`modify_time` as `modify_time`,
    null as `delete_time`,
    `op`.`description` as `memo`,
    null as `CUSTOMER_FIELD2`,
    null as `CUSTOMER_FIELD1`,
    null as `UUID`,
    null as `SORT`,
    `op`.`sys_flag` as `SYS_FLAG`,
    `op`.`grand_leader_id` as `HIGHER_LEADER_STAFF_ID`,
    `op`.`direct_leader_id` as `LEADER_STAFF_ID`,
    null as `YGXS`,
    null as `HUKOUXINGZHI`,
    null as `HUKOUDI`,
    null as `DANGANDI`,
    null as `DEGREE`,
    null as `EDUCATIONAL`,
    null as `PROFESSION`,
    null as `BIYE_TIME`,
    null as `SCHOOL`,
    null as `FOREIGN_LANGUAGE`,
    null as `COMPUTE_LEVEL`,
    null as `HEIGHT`,
    null as `VIRTUAL_MOBILE`,
    null as `POLITICS_INFO`,
    null as `NATION`,
    null as `MARRIAGE`,
    null as `ID_CARD`,
    null as `NATIVE_PLACE`,
    null as `BIRTHDAY`
from
    (`org_person` `op`
left join `auth_user` `au` on
    (`op`.`id` = `au`.`person_id`
    and `au`.`valid` = 1)));

create or replace
algorithm = UNDEFINED view `base_staff_mnecode` as (
select
    `m`.`ID` as `id`,
    `m`.`row_version` as `version`,
    `m`.`ORG_ID` as `staff`,
    `m`.`MNE_CODE` as `mne_code`
from
    (`org_mnecode` `m`
join `org_person` `p` on
    (`m`.`ORG_ID` = `p`.`id`)));

create or replace
algorithm = UNDEFINED view `base_workinfo` as (
select
    null as `worktime`,
    null as `JZKST`,
    null as `JGSSJ`,
    null as `LRSJ`,
    null as `RZSJ`,
    null as `ZZSJ`,
    null as `CONPANYNAME`,
    null as `BIGDEPARTMENT`,
    null as `DEPARTMENT`,
    null as `POSITIONNAME`,
    null as `POSITIONTYPE`,
    null as `ZWMC`,
    null as `GSEMAIL`,
    null as `ZKXNH`,
    null as `ZXHM`,
    null as `FJH`,
    null as `TXLBZ`,
    null as `FJHM`,
    `op`.`id` as `BASE_STAFF_ID`,
    null as `YJZZSJ`
from
    `org_person` `op`);

## 1.0.5

CREATE OR REPLACE VIEW base_staff_mnecode AS (
SELECT
	m.id AS id,
	m.row_version AS version,
	m.person_id AS staff,
	m.mne_code
FROM
	org_person_mnecode m);

CREATE OR REPLACE VIEW base_department_mnecode AS (
SELECT
	m.id AS id,
	m.row_version AS version,
	m.dept_id AS department,
	m.mne_code
FROM
	org_department_mnecode m);

CREATE OR REPLACE VIEW base_position_mnecode AS (
SELECT
	m.id AS id,
	m.row_version AS version,
	m.position_id AS position,
	m.mne_code
FROM
	org_position_mnecode m);

## 1.0.6
alter table org_person add user_id bigint default null;
alter table org_person add user_name varchar(256) default null;

## 1.0.7
CREATE INDEX ORG_COMPANY_LAY_REC_IDX USING BTREE ON ORG_COMPANY (lay_rec);

## 1.0.8
alter table org_company_mnecode modify create_time timestamp not null default current_timestamp;
alter table org_company_mnecode modify modify_time timestamp not null default current_timestamp;
alter table org_department_mnecode modify create_time timestamp not null default current_timestamp;
alter table org_department_mnecode modify modify_time timestamp not null default current_timestamp;
alter table org_position_mnecode modify create_time timestamp not null default current_timestamp;
alter table org_position_mnecode modify modify_time timestamp not null default current_timestamp;
alter table org_person_mnecode modify create_time timestamp not null default current_timestamp;
alter table org_person_mnecode modify modify_time timestamp not null default current_timestamp;

## 1.0.9
update org_position set name = '虚拟岗位' where sys_flag=1;
update org_department set name = '虚拟部门' where sys_flag=1;
update org_person set name = "虚拟人员", gender = null where sys_flag=1;

## 1.0.10
alter table org_person add entry_date varchar(64) default null comment '入职日期';
alter table org_person add title varchar(64) default null comment '职称(系统编码)';
alter table org_person add qualification varchar(256) default null comment '资质';
alter table org_person add education varchar(64) default null comment '学历(系统编码)';
alter table org_person add major varchar(256) default null comment '专业';
alter table org_person add id_number varchar(32) default null comment '身份证号';
alter table org_person add index id_number_index(id_number,valid);

## 1.0.11
alter table org_person add image_url varchar(256) default null;
alter table org_person add sign_pic_url varchar(256) default null;

## 1.0.12
alter table org_person change image_url avatar_url varchar(256) default null;
alter table org_person add entry_date varchar(64) default null comment '入职日期2021-05-26';

## 1.0.13
update org_position set name = '虚拟岗位' where sys_flag=1;
update org_department set name = '虚拟部门' where sys_flag=1;
update org_person set gender = null where sys_flag=1;

## 1.0.14
alter table org_person add index index code_index(code,valid);
alter table org_person_position add index index position_id_index(position_id);
alter table org_person modify column id_number varchar(256) default null;