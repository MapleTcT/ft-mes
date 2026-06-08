## 1.0.0
/*===================================================================*/
/* Table: 多公司模式组织架构公司表(org_company)                */
/*===================================================================*/
create table org_company (
    id BIGINT NOT NULL,
    row_version BIGINT DEFAULT 0,
    code VARCHAR(256) NOT NULL,
    description VARCHAR(2048) DEFAULT NULL,
    short_name VARCHAR(2048) NOT NULL,
    full_name VARCHAR(2048) NOT NULL,
    full_path VARCHAR(4096) NOT NULL,
    address VARCHAR(512) DEFAULT NULL,
    lay_no INT NOT NULL,
    lay_rec VARCHAR(4096) NOT NULL,
    sort decimal(20,6) NOT NULL,
    parent_id BIGINT DEFAULT NULL,
    valid TINYINT DEFAULT 1,
    old_id VARCHAR(256) NOT NULL,
    creator VARCHAR(32) DEFAULT NULL,
    modifier VARCHAR(32) DEFAULT NULL,
    create_time DATETIME,
    modify_time DATETIME,
    create_staff_id bigint default null, --comment '创建者人员id',
    modify_staff_id bigint default null, --comment '修改者人员id',
    PRIMARY KEY (id),
    UNIQUE (old_id)
);
if NOT exists(SELECT id FROM org_company WHERE code = 'default_org_company')
    INSERT INTO org_company(id, code, short_name, full_name, lay_no, full_path, sort, old_id, lay_rec) values(1000, 'default_org_company', '默认公司', '默认公司', 0, '/默认公司', 0, 'Company_default_org_company', '1000');
/*===================================================================*/
/* Table: 多公司模式组织架构部门表(org_department)                    */
/*===================================================================*/
create table org_department (
    id BIGINT NOT NULL,
    row_version BIGINT DEFAULT 0,
    code VARCHAR(256) NOT NULL,
    name VARCHAR(2048) NOT NULL,
    dept_type VARCHAR(64) DEFAULT 'general',
    description VARCHAR(2048) DEFAULT NULL,
    full_path VARCHAR(4096) NOT NULL,
    lay_no INT NOT NULL,
    lay_rec VARCHAR(4096) NOT NULL,
    sort decimal(20,6) NOT NULL,
    company_id BIGINT NOT NULL,
    parent_id BIGINT DEFAULT NULL,
    valid TINYINT DEFAULT 1,
    sys_flag TINYINT DEFAULT 1,
    old_id VARCHAR(256) NOT NULL,
    leaf TINYINT DEFAULT 1,
    creator VARCHAR(32) DEFAULT NULL,
    modifier VARCHAR(32) DEFAULT NULL,
    create_time DATETIME,
    modify_time DATETIME,
    create_staff_id bigint default null, --comment '创建者人员id',
    modify_staff_id bigint default null, --comment '修改者人员id',
    PRIMARY KEY (id),
    UNIQUE (old_id)
);
if NOT exists(SELECT id FROM org_department WHERE id = 1)
    INSERT INTO org_department(id, code, name, dept_type, full_path, lay_rec, lay_no, sort, company_id, sys_flag, old_id)VALUES(1, 'default_department', '虚拟部门', 'sys_department_type/general', '/默认公司/虚拟部门', '1', 1, 1000, 1000, 1, 'Department_1');
/*===================================================================*/
/* Table: 多公司模式组织架构岗位表(org_position)                    */
/*===================================================================*/
create table org_position (
    id BIGINT NOT NULL,
    row_version BIGINT DEFAULT 0,
    code VARCHAR(256) NOT NULL,
    name VARCHAR(2048) NOT NULL,
    description VARCHAR(2048) DEFAULT NULL,
    full_path VARCHAR(4096) NOT NULL,
    lay_no INT NOT NULL,
    lay_rec VARCHAR(4096) NOT NULL,
    sort decimal(20,6) NOT NULL,
    company_id BIGINT NOT NULL,
    dep_id BIGINT NOT NULL,
    parent_id BIGINT DEFAULT NULL,
    valid TINYINT DEFAULT 1,
    sys_flag TINYINT DEFAULT 1,
    leaf TINYINT DEFAULT 1,
    old_id VARCHAR(256) NOT NULL,
    creator VARCHAR(32) DEFAULT NULL,
    modifier VARCHAR(32) DEFAULT NULL,
    create_time DATETIME,
    modify_time DATETIME,
    create_staff_id bigint default null, --comment '创建者人员id',
    modify_staff_id bigint default null, --comment '修改者人员id',
    PRIMARY KEY (id),
    UNIQUE (old_id)
);
if NOT exists(SELECT id FROM org_position WHERE id = 1)
    INSERT INTO org_position(id, code, name, full_path, lay_rec, lay_no, sort, company_id, dep_id, sys_flag, old_id)VALUES(1, 'default_position', '虚拟岗位', '/默认公司/虚拟岗位', '1', 1, 1000, 1000, 1, 1, 'Position_1');
/*===================================================================*/
/* Table: 多公司模式组织架构组表(org_group)                    */
/*===================================================================*/
create table org_group (
    id BIGINT NOT NULL,
    row_version BIGINT DEFAULT 0,
    code VARCHAR(1024) NOT NULL,
    name VARCHAR(1024) NOT NULL,
    description VARCHAR(2048) DEFAULT NULL,
    full_path VARCHAR(4096)  NULL,
    sort decimal(20,6) NOT NULL,
    company_id BIGINT NOT NULL,
    manager_id BIGINT DEFAULT NULL,
    manager_name varchar(256) DEFAULT NULL,
    valid TINYINT DEFAULT 1,
    creator VARCHAR(32) DEFAULT NULL,
    modifier VARCHAR(32) DEFAULT NULL,
    create_time DATETIME,
    modify_time DATETIME,
    create_staff_id bigint default null, --comment '创建者人员id',
    modify_staff_id bigint default null, --comment '修改者人员id',
    PRIMARY KEY (id)
);
/*===================================================================*/
/* Table: 多公司模式组织架构人员表(org_person)                          */
/*===================================================================*/
create table org_person (
    id BIGINT NOT NULL,
    row_version BIGINT DEFAULT 0,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(512) NOT NULL,
    old_id VARCHAR(50) DEFAULT NULL,
    description VARCHAR(2048) DEFAULT NULL,
    gender VARCHAR(200) NULL,
    phone VARCHAR(256) NULL,
    email VARCHAR(256) NULL,
    status VARCHAR(200) NULL,
    classified_level VARCHAR(200) NULL,
    valid TINYINT DEFAULT 1,
    sys_flag TINYINT DEFAULT 1,
    main_position BIGINT DEFAULT NULL,
    direct_leader_id BIGINT DEFAULT NULL,
    grand_leader_id BIGINT DEFAULT NULL,
    create_user TINYINT DEFAULT 0,
    creator VARCHAR(32) DEFAULT NULL,
    modifier VARCHAR(32) DEFAULT NULL,
    create_time DATETIME,
    modify_time DATETIME,
    create_staff_id bigint default null, --comment '创建者人员id',
    modify_staff_id bigint default null, --comment '修改者人员id',
    PRIMARY KEY (id),
    UNIQUE (old_id)
);
if NOT exists(SELECT id FROM org_person WHERE id = 1)
    INSERT INTO org_person(id, code, name, old_id, description, status, sys_flag, main_position)VALUES(1, 'default_person', '虚拟人员', 'Person_1', '虚拟人员', 'sys_person_status/onWork', 1, 1);
/*===================================================================*/
/* Table: 多公司模式组织架构岗位人员关联表(org_person_position)           */
/*===================================================================*/
create table org_person_position (
    id BIGINT NOT NULL,
    row_version BIGINT DEFAULT 0,
    position_id BIGINT NOT NULL ,
    person_id BIGINT NOT NULL ,
    work_time DATE,
    off_time DATE,
    remark VARCHAR(256),
    creator VARCHAR(32) DEFAULT NULL,
    modifier VARCHAR(32) DEFAULT NULL,
    valid TINYINT DEFAULT 1,
    create_time DATETIME,
    modify_time DATETIME,
    create_staff_id bigint default null, --comment '创建者人员id',
    modify_staff_id bigint default null, --comment '修改者人员id',
    PRIMARY KEY (id),
    UNIQUE(position_id, person_id)
);
if NOT exists(SELECT id FROM org_person_position WHERE id = 1)
    INSERT INTO org_person_position(id, position_id, person_id)VALUES(1, 1, 1);
/*===================================================================*/
/* Table: 标签表(org_tag)           */
/*===================================================================*/
create table org_tag (
    id BIGINT NOT NULL,
    row_version BIGINT DEFAULT 0,
    tag_type VARCHAR(32) DEFAULT NULL ,
    name VARCHAR(256) NOT NULL,
    company_id BIGINT NOT NULL,
    creator VARCHAR(32) DEFAULT NULL,
    modifier VARCHAR(32) DEFAULT NULL,
    valid TINYINT DEFAULT 1,
    create_time DATETIME,
    modify_time DATETIME,
    create_staff_id bigint default null, --comment '创建者人员id',
    modify_staff_id bigint default null, --comment '修改者人员id',
    PRIMARY KEY (id),
    UNIQUE(company_id, name, tag_type)
);
/*===================================================================*/
/* Table: 多公司模式组织架构部门,岗位,组负责人关联表(org_manager)           */
/*===================================================================*/
create table org_manager (
    id BIGINT NOT NULL,
    row_version BIGINT DEFAULT 0,
    org_id BIGINT NOT NULL ,
    manager_id BIGINT NOT NULL ,
    manager_name VARCHAR(512) NOT NULL,
    manager_type VARCHAR(16) NOT NULL,
    creator VARCHAR(32) DEFAULT NULL,
    modifier VARCHAR(32) DEFAULT NULL,
    create_time DATETIME,
    modify_time DATETIME,
    create_staff_id bigint default null, --comment '创建者人员id',
    modify_staff_id bigint default null, --comment '修改者人员id',
    PRIMARY KEY (id),
    UNIQUE(org_id, manager_id, manager_type)
);

/*===================================================================*/
/* Table: 多公司模式组织架构组管理人员关联表(org_group_person)           */
/*===================================================================*/
create table org_group_person (
    id BIGINT NOT NULL,
    row_version BIGINT DEFAULT 0,
    group_id BIGINT NOT NULL ,
    person_id BIGINT NOT NULL ,
    creator VARCHAR(32) DEFAULT NULL,
    modifier VARCHAR(32) DEFAULT NULL,
    valid TINYINT DEFAULT 1,
    create_time DATETIME,
    modify_time DATETIME,
    create_staff_id bigint default null, --comment '创建者人员id',
    modify_staff_id bigint default null, --comment '修改者人员id',
    PRIMARY KEY (id),
    UNIQUE(group_id, person_id)
);
/*===================================================================*/
/* Table: Excel导入记录表(org_excel)           */
/*===================================================================*/
create table org_excel (
    id BIGINT NOT NULL,
    status TINYINT NOT NULL ,
    file_name VARCHAR(256) DEFAULT NULL,
    error_file VARCHAR(256) DEFAULT NULL,
    error_message VARCHAR(512) DEFAULT NULL,
    excel_type VARCHAR(64) DEFAULT NULL,
    creator VARCHAR(32) DEFAULT NULL,
    modifier VARCHAR(32) DEFAULT NULL,
    create_time DATETIME,
    modify_time DATETIME,
    create_staff_id bigint default null, --comment '创建者人员id',
    modify_staff_id bigint default null, --comment '修改者人员id',
    PRIMARY KEY (id)
);
/*===================================================================*/
/* Table: 岗位角色表(org_position_role)           */
/*===================================================================*/
create table org_position_role (
    id BIGINT NOT NULL,
    position_id BIGINT NOT NULL ,
    role_id BIGINT NOT NULL,
    creator VARCHAR(32) DEFAULT NULL,
    modifier VARCHAR(32) DEFAULT NULL,
    create_time DATETIME,
    modify_time DATETIME,
    create_staff_id bigint default null, --comment '创建者人员id',
    modify_staff_id bigint default null, --comment '修改者人员id',
    PRIMARY KEY (id),
    UNIQUE(position_id, role_id)
);

/*===================================================================*/
/* Table: 多公司模式组织架构部门人员关联表(org_person_department)           */
/*===================================================================*/
create table org_person_department  (
    id BIGINT NOT NULL,
    row_version BIGINT DEFAULT 0,
    dept_id BIGINT NOT NULL,
    person_id BIGINT NOT NULL,
    position_id BIGINT NOT NULL,
    creator VARCHAR(32) DEFAULT NULL,
    modifier VARCHAR(32) DEFAULT NULL,
    valid TINYINT DEFAULT 1,
    create_time DATETIME,
    modify_time DATETIME,
    create_staff_id bigint default null, --comment '创建者人员id',
    modify_staff_id bigint default null, --comment '修改者人员id',
    PRIMARY KEY (id)
);
if NOT exists(SELECT id FROM org_person_department WHERE id = 1)
    INSERT INTO org_person_department(id, dept_id, position_id, person_id)VALUES(1, 1, 1, 1);
/*===================================================================*/
/* Table: 多公司模式组织架构公司人员关联表(org_person_company)           */
/*===================================================================*/
create table org_person_company  (
    id BIGINT NOT NULL,
    row_version BIGINT DEFAULT 0,
    company_id BIGINT NOT NULL,
    person_id BIGINT NOT NULL,
    position_id BIGINT NOT NULL,
    creator VARCHAR(32) DEFAULT NULL,
    modifier VARCHAR(32) DEFAULT NULL,
    valid TINYINT DEFAULT 1,
    create_time DATETIME,
    modify_time DATETIME,
    create_staff_id bigint default null, --comment '创建者人员id',
    modify_staff_id bigint default null, --comment '修改者人员id',
    PRIMARY KEY (id)
);
if NOT exists(SELECT id FROM org_person_company WHERE id = 1)
    INSERT INTO org_person_company(id, company_id, position_id, person_id)VALUES(1, 1000, 1, 1);
/*==============================================================*/
/* Table:组织架构助记码(org_mnecode)                            */
/*==============================================================*/
CREATE TABLE org_mnecode
(
    ID                  BIGINT NOT NULL ,
    row_version         BIGINT DEFAULT 0 NOT NULL,
    LANGUAGE            VARCHAR(510) ,
    ORG_ID           BIGINT ,
    MNE_CODE            VARCHAR(510) ,
    creator VARCHAR(32) DEFAULT NULL,
    modifier VARCHAR(32) DEFAULT NULL,
    create_time DATETIME,
    modify_time DATETIME,
    create_staff_id bigint default null, --comment '创建者人员id',
    modify_staff_id bigint default null, --comment '修改者人员id',
    PRIMARY KEY(ID)
);
EXECUTE sp_addextendedproperty N'MS_Description', '组织助记码', N'user', N'dbo', N'table', N'org_mnecode', NULL, NULL;
EXECUTE sp_addextendedproperty N'MS_Description', '主键ID', N'user', N'dbo', N'table', N'org_mnecode', N'column', N'ID';
EXECUTE sp_addextendedproperty N'MS_Description', '版本', N'user', N'dbo', N'table', N'org_mnecode', N'column', N'row_version';
EXECUTE sp_addextendedproperty N'MS_Description', '语言', N'user', N'dbo', N'table', N'org_mnecode', N'column', N'LANGUAGE';
EXECUTE sp_addextendedproperty N'MS_Description', '助记码', N'user', N'dbo', N'table', N'org_mnecode', N'column', N'MNE_CODE';
EXECUTE sp_addextendedproperty N'MS_Description', '组织ID', N'user', N'dbo', N'table', N'org_mnecode', N'column', N'ORG_ID';
## 1.0.1
ALTER TABLE org_person ADD direct_leader_id BIGINT DEFAULT NULL;
ALTER TABLE org_person ADD grand_leader_id BIGINT DEFAULT NULL;
ALTER TABLE org_department ADD sys_flag number(1,0) DEFAULT 0;
ALTER TABLE org_position ADD sys_flag number(1,0) DEFAULT 0;
ALTER TABLE org_person ADD sys_flag number(1,0) DEFAULT 0;
/*===================================================================*/
/* Table: 视图        */
/*===================================================================*/
CREATE VIEW "BASE_COMPANY" ("ID", "VERSION", "CODE", "DESCRIPTION", "SHORT_NAME", "NAME", "ADDRESS", "SORT", "PARENT_ID", "LAY_REC", "VALID", "CREATE_STAFF_ID", "MODIFY_STAFF_ID", "DELETE_STAFF_ID", "CREATE_TIME", "MODIFY_TIME", "DELETE_TIME", "IS_DEFAULT", "CUSTOMER_FIELD2", "CUSTOMER_FIELD1", "UUID", "SC_NATURE", "EMAIL", "SITE", "FAX", "TELEPHONE", "POST_CODE", "TYPE") AS
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
	NULL as create_staff_id,
	NULL as modify_staff_id,
	NULL as delete_staff_id,
	create_time,
	modify_time,
	NULL as delete_time,
	1 AS IS_DEFAULT,
	NULL AS CUSTOMER_FIELD2,
	NULL AS CUSTOMER_FIELD1,
	NULL AS UUID,
	NULL AS SC_NATURE,
	NULL AS EMAIL,
	NULL AS SITE,
	NULL AS FAX,
	NULL AS TELEPHONE,
	NULL AS POST_CODE,
	'UNIT' AS TYPE
from
	org_company
);


CREATE VIEW "BASE_COMPANYSTAFF" ("ID", "VERSION", "CID", "STAFF_ID", "VALID", "TRANSFER_OUT_DEAL_TIME", "TRANSFER_IN_DEAL_TIME", "TRANSFER_OUT_DEALER_ID", "TRANSFER_IN_DEALER_ID", "EDIT_DATE", "END_TIME", "START_TIME") AS
  (
select
	id,
	row_version as version,
	company_id as cid,
	person_id as staff_id,
	valid,
	NULL AS TRANSFER_OUT_DEAL_TIME,
	NULL AS TRANSFER_IN_DEAL_TIME,
	NULL AS TRANSFER_OUT_DEALER_ID,
	NULL AS TRANSFER_IN_DEALER_ID,
	NULL AS EDIT_DATE,
	NULL AS END_TIME,
	NULL AS START_TIME
from org_person_company
);


CREATE VIEW "BASE_CUSTOM_GROUP" ("ID", "VERSION", "CODE", "NAME_CN", "DESCRIPTION", "SORT", "CID", "VALID", "CREATE_STAFF_ID", "MODIFY_STAFF_ID", "DELETE_STAFF_ID", "CREATE_TIME", "MODIFY_TIME", "DELETE_TIME") AS
  ( select id, row_version as version, code, name as name_cn, description, sort, company_id as cid, valid, NULL as create_staff_id, NULL as modify_staff_id, NULL as delete_staff_id, create_time, modify_time, NULL  AS delete_time from org_group );

CREATE VIEW "BASE_CUSTOM_GROUPMEMBER" ("ID", "VERSION", "RELATED_CUSTOM_GROUP_ID", "RELATED_STAFF_ID", "VALID", "CREATE_STAFF_ID", "MODIFY_STAFF_ID", "DELETE_STAFF_ID", "CREATE_TIME", "MODIFY_TIME", "CID", "DELETE_TIME") AS
  ( select gp.id as id, gp.row_version as version, gp.group_id as related_custom_group_id, gp.person_id as related_staff_id, gp.valid as valid, CAST (gp.creator AS int) as create_staff_id, CAST (gp.modifier AS int) as modify_staff_id, null as delete_staff_id, gp.create_time, gp.modify_time, g.company_id as cid, NULL AS delete_time from org_group_person gp left join org_group g on gp.group_id = g.id );


 CREATE VIEW "BASE_DEPARTMENT" ("ID", "VERSION", "CODE", "NAME", "DESCRIPTION", "FULL_PATH_NAME", "LAY_NO", "LAY_REC", "SORT", "CID", "PARENT_ID", "VALID", "LEAF", "CREATE_STAFF_ID", "MODIFY_STAFF_ID", "DELETE_STAFF_ID", "CREATE_TIME", "MODIFY_TIME", "DELETE_TIME", "STAFF_ID", "OBJPARAME", "OBJPARAMD", "OBJPARAMC", "OBJPARAMB", "OBJPARAMA", "RES_SCPARAME", "RES_SCPARAMD", "RES_SCPARAMC", "RES_SCPARAMB", "RES_SCPARAMA", "CHARPARAME", "CHARPARAMD", "CHARPARAMC", "CHARPARAMB", "CHARPARAMA", "NUMBERPARAME", "NUMBERPARAMD", "NUMBERPARAMC", "NUMBERPARAMB", "NUMBERPARAMA", "DATEPARAME", "DATEPARAMD", "DATEPARAMC", "DATEPARAMB", "DATEPARAMA", "INTEGERPARAME", "INTEGERPARAMD", "INTEGERPARAMC", "INTEGERPARAMB", "INTEGERPARAMA", "CUSTOMER_FIELD2", "CUSTOMER_FIELD1", "SC_NATURE", "UUID", "IS_VIRTUAL") AS
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
	NULL as create_staff_id,
	NULL as modify_staff_id,
	NULL as delete_staff_id,
	create_time,
	modify_time,
	NULL as delete_time,
	NULL as staff_id,
	NULL AS OBJPARAME,
	NULL AS OBJPARAMD,
	NULL AS OBJPARAMC,
	NULL AS OBJPARAMB,
	NULL AS OBJPARAMA,
	NULL AS RES_SCPARAME,
	NULL AS RES_SCPARAMD,
	NULL AS RES_SCPARAMC,
	NULL AS RES_SCPARAMB,
	NULL AS RES_SCPARAMA,
	NULL AS CHARPARAME,
	NULL AS CHARPARAMD,
	NULL AS CHARPARAMC,
	NULL AS CHARPARAMB,
	NULL AS CHARPARAMA,
	NULL AS NUMBERPARAME,
	NULL AS NUMBERPARAMD,
	NULL AS NUMBERPARAMC,
	NULL AS NUMBERPARAMB,
	NULL AS NUMBERPARAMA,
	NULL AS DATEPARAME,
	NULL AS DATEPARAMD,
	NULL AS DATEPARAMC,
	NULL AS DATEPARAMB,
	NULL AS DATEPARAMA,
	NULL AS INTEGERPARAME,
	NULL AS INTEGERPARAMD,
	NULL AS INTEGERPARAMC,
	NULL AS INTEGERPARAMB,
	NULL AS INTEGERPARAMA,
	NULL AS CUSTOMER_FIELD2,
	NULL AS CUSTOMER_FIELD1,
	NULL AS SC_NATURE,
	NULL AS UUID,
	SYS_FLAG AS IS_VIRTUAL
from
	org_department);

CREATE VIEW "BASE_DEPARTMENTWORK" ("ID", "VERSION", "DEPARTMENT_ID", "STAFF_ID", "VALID", "TRANSFER_OUT_DEAL_TIME", "TRANSFER_IN_DEAL_TIME", "TRANSFER_OUT_DEALER_ID", "TRANSFER_IN_DEALER_ID", "EDIT_DATE", "END_TIME", "START_TIME") AS
  (
select
	id,
	row_version as version,
	dept_id as department_id,
	person_id as staff_id,
	valid,
	NULL AS TRANSFER_OUT_DEAL_TIME,
	NULL AS TRANSFER_IN_DEAL_TIME,
	NULL AS TRANSFER_OUT_DEALER_ID,
	NULL AS TRANSFER_IN_DEALER_ID,
	NULL AS EDIT_DATE,
	NULL AS END_TIME,
	NULL AS START_TIME
from org_person_department
);

CREATE VIEW "BASE_POSITION" ("ID", "VERSION", "CODE", "NAME", "DESCRIPTION", "FULL_PATH_NAME", "LAY_NO", "LAY_REC", "SORT", "CID", "DEPARTMENT_ID", "PARENT_ID", "VALID", "LEAF", "CREATE_STAFF_ID", "MODIFY_STAFF_ID", "DELETE_STAFF_ID", "CREATE_TIME", "MODIFY_TIME", "DELETE_TIME", "OBJPARAME", "OBJPARAMD", "OBJPARAMC", "OBJPARAMB", "OBJPARAMA", "RES_SCPARAME", "RES_SCPARAMD", "RES_SCPARAMC", "RES_SCPARAMB", "RES_SCPARAMA", "CHARPARAME", "CHARPARAMD", "CHARPARAMC", "CHARPARAMB", "CHARPARAMA", "NUMBERPARAME", "NUMBERPARAMD", "NUMBERPARAMC", "NUMBERPARAMB", "NUMBERPARAMA", "DATEPARAME", "DATEPARAMD", "DATEPARAMC", "DATEPARAMB", "DATEPARAMA", "INTEGERPARAME", "INTEGERPARAMD", "INTEGERPARAMC", "INTEGERPARAMB", "INTEGERPARAMA", "UUID", "IS_VIRTUAL", "SC_NATURE") AS
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
	NULL as create_staff_id,
	NULL as modify_staff_id,
	NULL as delete_staff_id,
	create_time,
	modify_time,
	NULL as delete_time,
	NULL AS OBJPARAME,
	NULL AS OBJPARAMD,
	NULL AS OBJPARAMC,
	NULL AS OBJPARAMB,
	NULL AS OBJPARAMA,
	NULL AS RES_SCPARAME,
	NULL AS RES_SCPARAMD,
	NULL AS RES_SCPARAMC,
	NULL AS RES_SCPARAMB,
	NULL AS RES_SCPARAMA,
	NULL AS CHARPARAME,
	NULL AS CHARPARAMD,
	NULL AS CHARPARAMC,
	NULL AS CHARPARAMB,
	NULL AS CHARPARAMA,
	NULL AS NUMBERPARAME,
	NULL AS NUMBERPARAMD,
	NULL AS NUMBERPARAMC,
	NULL AS NUMBERPARAMB,
	NULL AS NUMBERPARAMA,
	NULL AS DATEPARAME,
	NULL AS DATEPARAMD,
	NULL AS DATEPARAMC,
	NULL AS DATEPARAMB,
	NULL AS DATEPARAMA,
	NULL AS INTEGERPARAME,
	NULL AS INTEGERPARAMD,
	NULL AS INTEGERPARAMC,
	NULL AS INTEGERPARAMB,
	NULL AS INTEGERPARAMA,
	NULL AS UUID,
	SYS_FLAG AS IS_VIRTUAL,
	NULL AS SC_NATURE
from
	org_position);

CREATE VIEW "BASE_POSITIONWORK" ("ID", "VERSION", "POSITION_ID", "STAFF_ID", "START_TIME", "VALID", "TRANSFER_OUT_DEAL_TIME", "TRANSFER_IN_DEAL_TIME", "TRANSFER_OUT_DEALER_ID", "TRANSFER_IN_DEALER_ID", "EDIT_DATE", "MAIN_POSI_FLAG", "PARENT_PW_ID", "TRANSFER_OUT_DES", "TRANSFER_IN_DES", "END_TIME") AS
  (
select
	id,
	row_version as version,
	position_id,
	person_id as staff_id,
	work_time as start_time,
	valid,
	NULL AS TRANSFER_OUT_DEAL_TIME,
	NULL AS TRANSFER_IN_DEAL_TIME,
	NULL AS TRANSFER_OUT_DEALER_ID,
	NULL AS TRANSFER_IN_DEALER_ID,
	NULL AS EDIT_DATE,
	NULL AS MAIN_POSI_FLAG,
	NULL AS PARENT_PW_ID,
	NULL AS TRANSFER_OUT_DES,
	NULL AS TRANSFER_IN_DES,
	NULL AS END_TIME
from org_person_position
);


CREATE VIEW "BASE_ROLEPOSITION" ("ID", "VERSION", "POSITION_ID", "ROLE_ID", "VALID") AS ( select id, 0 as version, position_id, role_id, 1 as valid from org_position_role );

CREATE VIEW "BASE_STAFF" ("ID", "VERSION", "CODE", "NAME", "SEX", "MOBILE", "EMAIL", "WORK_STATUS", "SECURITY_CLASS", "VALID", "USER_ID", "MAIN_POSITION_ID", "CREATE_STAFF_ID", "MODIFY_STAFF_ID", "DELETE_STAFF_ID", "CREATE_TIME", "MODIFY_TIME", "DELETE_TIME", "MEMO", "CUSTOMER_FIELD2", "CUSTOMER_FIELD1", "UUID", "SORT", "SYS_FLAG", "HIGHER_LEADER_STAFF_ID", "LEADER_STAFF_ID", "YGXS", "HUKOUXINGZHI", "HUKOUDI", "DANGANDI", "DEGREE", "EDUCATIONAL", "PROFESSION", "BIYE_TIME", "SCHOOL", "FOREIGN_LANGUAGE", "COMPUTE_LEVEL", "HEIGHT", "VIRTUAL_MOBILE", "POLITICS_INFO", "NATION", "MARRIAGE", "ID_CARD", "NATIVE_PLACE", "BIRTHDAY") AS
  (
select
	op.id as id,
	row_version as version,
	code,
	name,
	gender as SEX,
	phone as mobile,
	email,
	status as work_status,
	classified_level as security_class,
	op.valid as valid,
	au.id as user_id,
	main_position as main_position_id,
	NULL as create_staff_id,
	NULL as modify_staff_id,
	NULL as delete_staff_id,
	op.create_time AS create_time,
	op.modify_time AS modify_time,
	NULL as delete_time,
	op.description  as memo,
	NULL AS CUSTOMER_FIELD2,
	NULL AS CUSTOMER_FIELD1,
	NULL AS UUID,
	NULL AS SORT,
	op.sys_flag AS SYS_FLAG,
	NULL AS HIGHER_LEADER_STAFF_ID,
	NULL AS LEADER_STAFF_ID,
	NULL AS YGXS,
	NULL AS HUKOUXINGZHI,
	NULL AS HUKOUDI,
	NULL AS DANGANDI,
	NULL AS DEGREE,
	NULL AS EDUCATIONAL,
	NULL AS PROFESSION,
	NULL AS BIYE_TIME,
	NULL AS SCHOOL,
	NULL AS FOREIGN_LANGUAGE,
	NULL AS COMPUTE_LEVEL,
	NULL AS HEIGHT,
	NULL AS VIRTUAL_MOBILE,
	NULL AS POLITICS_INFO,
	NULL AS NATION,
	NULL AS MARRIAGE,
	NULL AS ID_CARD,
	NULL AS NATIVE_PLACE,
	NULL AS BIRTHDAY
from
	org_person op
left outer join
	auth_user au
on op.id=au.person_id and au.valid = 1
);

CREATE  VIEW base_staff_mnecode AS (
SELECT
	m.id AS id,
	m.row_version AS version,
	m.org_id AS staff,
	m.mne_code
FROM
	org_mnecode m
INNER JOIN org_person p ON
	m.org_id = p.id );

CREATE  VIEW base_department_mnecode AS (
SELECT
	m.id AS id,
	m.row_version AS version,
	m.org_id AS department,
	m.mne_code
FROM
	org_mnecode m
INNER JOIN org_department d ON
	m.org_id = d.id );

CREATE  VIEW base_position_mnecode AS (
SELECT
	m.id AS id,
	m.row_version AS version,
	m.org_id AS position,
	m.mne_code
FROM
	org_mnecode m
INNER JOIN org_position p ON
	m.org_id = p.id );

CREATE  VIEW base_custom_group_mnecode AS (
SELECT
	m.id AS id,
	m.row_version AS version,
	m.org_id AS custom_group,
	m.mne_code
FROM
	org_mnecode m
INNER JOIN org_group g ON
	m.org_id = g.id );

create view base_linkinfo as (
select
  id AS base_staff_id,
  phone AS mobile,
  NULL AS jabber,
  email
from org_person
);

create VIEW base_workinfo AS (
SELECT
  NULL AS worktime,
  NULL AS JZKST,
  NULL AS JGSSJ,
  NULL AS LRSJ,
  NULL AS RZSJ,
  NULL AS ZZSJ,
  NULL AS CONPANYNAME,
  NULL AS BIGDEPARTMENT,
  NULL AS DEPARTMENT,
  NULL AS POSITIONNAME,
  NULL AS POSITIONTYPE,
  NULL AS ZWMC,
  NULL AS GSEMAIL,
  NULL AS ZKXNH,
  NULL AS ZXHM,
  NULL AS FJH,
  NULL AS TXLBZ,
  NULL AS FJHM,
  op.id AS BASE_STAFF_ID,
  NULL AS YJZZSJ
FROM ORG_PERSON op
);

## 1.0.2
drop view BASE_STAFF;
CREATE VIEW "BASE_STAFF" ("ID", "VERSION", "CODE", "NAME", "SEX", "MOBILE", "EMAIL", "WORK_STATUS", "SECURITY_CLASS", "VALID", "USER_ID", "MAIN_POSITION_ID", "CREATE_STAFF_ID", "MODIFY_STAFF_ID", "DELETE_STAFF_ID", "CREATE_TIME", "MODIFY_TIME", "DELETE_TIME", "MEMO", "CUSTOMER_FIELD2", "CUSTOMER_FIELD1", "UUID", "SORT", "SYS_FLAG", "HIGHER_LEADER_STAFF_ID", "LEADER_STAFF_ID", "YGXS", "HUKOUXINGZHI", "HUKOUDI", "DANGANDI", "DEGREE", "EDUCATIONAL", "PROFESSION", "BIYE_TIME", "SCHOOL", "FOREIGN_LANGUAGE", "COMPUTE_LEVEL", "HEIGHT", "VIRTUAL_MOBILE", "POLITICS_INFO", "NATION", "MARRIAGE", "ID_CARD", "NATIVE_PLACE", "BIRTHDAY") AS
  (
select
	op.id as id,
	row_version as version,
	code,
	name,
	gender as SEX,
	phone as mobile,
	email,
	status as work_status,
	classified_level as security_class,
	op.valid as valid,
	au.id as user_id,
	main_position as main_position_id,
	NULL as create_staff_id,
	NULL as modify_staff_id,
	NULL as delete_staff_id,
	op.create_time AS create_time,
	op.modify_time AS modify_time,
	NULL as delete_time,
	op.description  as memo,
	NULL AS CUSTOMER_FIELD2,
	NULL AS CUSTOMER_FIELD1,
	NULL AS UUID,
	NULL AS SORT,
	op.sys_flag AS SYS_FLAG,
	op.grand_leader_id AS HIGHER_LEADER_STAFF_ID,
	op.direct_leader_id AS LEADER_STAFF_ID,
	NULL AS YGXS,
	NULL AS HUKOUXINGZHI,
	NULL AS HUKOUDI,
	NULL AS DANGANDI,
	NULL AS DEGREE,
	NULL AS EDUCATIONAL,
	NULL AS PROFESSION,
	NULL AS BIYE_TIME,
	NULL AS SCHOOL,
	NULL AS FOREIGN_LANGUAGE,
	NULL AS COMPUTE_LEVEL,
	NULL AS HEIGHT,
	NULL AS VIRTUAL_MOBILE,
	NULL AS POLITICS_INFO,
	NULL AS NATION,
	NULL AS MARRIAGE,
	NULL AS ID_CARD,
	NULL AS NATIVE_PLACE,
	NULL AS BIRTHDAY
from
	org_person op
left outer join
	auth_user au
on op.id=au.person_id and au.valid = 1
);


## 1.0.3

/*===================================================================*/
/* Table: 公司助记码(org_company_mnecode)                */
/*===================================================================*/
create table org_company_mnecode (
    id BIGINT NOT NULL,
    row_version BIGINT DEFAULT 0,
    language VARCHAR(510) DEFAULT NULL,
    mne_code VARCHAR(510) NOT NULL,
    company_id BIGINT NOT NULL,
    company_short_name VARCHAR(510) NOT NULL,
    creator VARCHAR(32) DEFAULT NULL,
    modifier VARCHAR(32) DEFAULT NULL,
    create_time DATETIME,
    modify_time DATETIME,
    create_staff_id bigint default null, --comment '创建者人员id',
    modify_staff_id bigint default null, --comment '修改者人员id',
    PRIMARY KEY (id)
);

EXECUTE sp_addextendedproperty N'MS_Description', '公司助记码', N'user', N'dbo', N'table', N'org_company_mnecode', NULL, NULL;
EXECUTE sp_addextendedproperty N'MS_Description', '主键ID', N'user', N'dbo', N'table', N'org_company_mnecode', N'column', N'ID';
EXECUTE sp_addextendedproperty N'MS_Description', '版本', N'user', N'dbo', N'table', N'org_company_mnecode', N'column', N'row_version';
EXECUTE sp_addextendedproperty N'MS_Description', '语言', N'user', N'dbo', N'table', N'org_company_mnecode', N'column', N'language';
EXECUTE sp_addextendedproperty N'MS_Description', '助记码', N'user', N'dbo', N'table', N'org_company_mnecode', N'column', N'mne_code';
EXECUTE sp_addextendedproperty N'MS_Description', '公司id', N'user', N'dbo', N'table', N'org_company_mnecode', N'column', N'company_id';
EXECUTE sp_addextendedproperty N'MS_Description', '公司简称', N'user', N'dbo', N'table', N'org_company_mnecode', N'column', N'company_short_name';


/*===================================================================*/
/* Table: 部门助记码(org_department_mnecode)                */
/*===================================================================*/
create table org_department_mnecode (
    id BIGINT NOT NULL,
    row_version BIGINT DEFAULT 0,
    language VARCHAR(510) DEFAULT NULL,
    mne_code VARCHAR(510) NOT NULL,
    dept_id BIGINT NOT NULL,
    dept_name VARCHAR(510) NOT NULL,
    creator VARCHAR(32) DEFAULT NULL,
    modifier VARCHAR(32) DEFAULT NULL,
    create_time DATETIME,
    modify_time DATETIME,
    create_staff_id bigint default null, --comment '创建者人员id',
    modify_staff_id bigint default null, --comment '修改者人员id',
    PRIMARY KEY (id)
);

EXECUTE sp_addextendedproperty N'MS_Description', '公司助记码', N'user', N'dbo', N'table', N'org_department_mnecode', NULL, NULL;
EXECUTE sp_addextendedproperty N'MS_Description', '主键ID', N'user', N'dbo', N'table', N'org_department_mnecode', N'column', N'ID';
EXECUTE sp_addextendedproperty N'MS_Description', '版本', N'user', N'dbo', N'table', N'org_department_mnecode', N'column', N'row_version';
EXECUTE sp_addextendedproperty N'MS_Description', '语言', N'user', N'dbo', N'table', N'org_department_mnecode', N'column', N'language';
EXECUTE sp_addextendedproperty N'MS_Description', '助记码', N'user', N'dbo', N'table', N'org_department_mnecode', N'column', N'mne_code';
EXECUTE sp_addextendedproperty N'MS_Description', '部门id', N'user', N'dbo', N'table', N'org_department_mnecode', N'column', N'dept_id';
EXECUTE sp_addextendedproperty N'MS_Description', '部门名称', N'user', N'dbo', N'table', N'org_department_mnecode', N'column', N'dept_name';


/*===================================================================*/
/* Table: 岗位助记码(org_position_mnecode)                */
/*===================================================================*/
create table org_position_mnecode (
    id BIGINT NOT NULL,
    row_version BIGINT DEFAULT 0,
    language VARCHAR(510) DEFAULT NULL,
    mne_code VARCHAR(510) NOT NULL,
    position_id BIGINT NOT NULL,
    position_name VARCHAR(510) NOT NULL,
    creator VARCHAR(32) DEFAULT NULL,
    modifier VARCHAR(32) DEFAULT NULL,
    create_time DATETIME,
    modify_time DATETIME,
    create_staff_id bigint default null, --comment '创建者人员id',
    modify_staff_id bigint default null, --comment '修改者人员id',
    PRIMARY KEY (id)
);

EXECUTE sp_addextendedproperty N'MS_Description', '公司助记码', N'user', N'dbo', N'table', N'org_position_mnecode', NULL, NULL;
EXECUTE sp_addextendedproperty N'MS_Description', '主键ID', N'user', N'dbo', N'table', N'org_position_mnecode', N'column', N'ID';
EXECUTE sp_addextendedproperty N'MS_Description', '版本', N'user', N'dbo', N'table', N'org_position_mnecode', N'column', N'row_version';
EXECUTE sp_addextendedproperty N'MS_Description', '语言', N'user', N'dbo', N'table', N'org_position_mnecode', N'column', N'language';
EXECUTE sp_addextendedproperty N'MS_Description', '助记码', N'user', N'dbo', N'table', N'org_position_mnecode', N'column', N'mne_code';
EXECUTE sp_addextendedproperty N'MS_Description', '岗位id', N'user', N'dbo', N'table', N'org_position_mnecode', N'column', N'position_id';
EXECUTE sp_addextendedproperty N'MS_Description', '岗位名称', N'user', N'dbo', N'table', N'org_position_mnecode', N'column', N'position_name';

/*===================================================================*/
/* Table: 人员助记码(org_position_mnecode)                */
/*===================================================================*/
create table org_person_mnecode(
    id BIGINT NOT NULL,
    row_version BIGINT DEFAULT 0,
    language VARCHAR(510) DEFAULT NULL,
    mne_code VARCHAR(510) NOT NULL,
    person_id BIGINT NOT NULL,
    person_name VARCHAR(510) NOT NULL,
    creator VARCHAR(32) DEFAULT NULL,
    modifier VARCHAR(32) DEFAULT NULL,
    create_time DATETIME,
    modify_time DATETIME,
    create_staff_id bigint default null, --comment '创建者人员id',
    modify_staff_id bigint default null, --comment '修改者人员id',
    PRIMARY KEY (id)
);

EXECUTE sp_addextendedproperty N'MS_Description', '公司助记码', N'user', N'dbo', N'table', N'org_person_mnecode', NULL, NULL;
EXECUTE sp_addextendedproperty N'MS_Description', '主键ID', N'user', N'dbo', N'table', N'org_person_mnecode', N'column', N'ID';
EXECUTE sp_addextendedproperty N'MS_Description', '版本', N'user', N'dbo', N'table', N'org_person_mnecode', N'column', N'row_version';
EXECUTE sp_addextendedproperty N'MS_Description', '语言', N'user', N'dbo', N'table', N'org_person_mnecode', N'column', N'language';
EXECUTE sp_addextendedproperty N'MS_Description', '助记码', N'user', N'dbo', N'table', N'org_person_mnecode', N'column', N'mne_code';
EXECUTE sp_addextendedproperty N'MS_Description', '人员id', N'user', N'dbo', N'table', N'org_person_mnecode', N'column', N'person_id';
EXECUTE sp_addextendedproperty N'MS_Description', '人员名称', N'user', N'dbo', N'table', N'org_person_mnecode', N'column', N'person_name';


DROP view base_staff_mnecode;
CREATE  VIEW base_staff_mnecode AS (
SELECT
	m.id AS id,
	m.row_version AS version,
	m.person_id AS staff,
	m.mne_code
FROM
	org_person_mnecode m);

DROP view base_department_mnecode;
CREATE  VIEW base_department_mnecode AS (
SELECT
	m.id AS id,
	m.row_version AS version,
	m.dept_id AS department,
	m.mne_code
FROM
	org_department_mnecode m);

DROP view base_position_mnecode;
CREATE  VIEW base_position_mnecode AS (
SELECT
	m.id AS id,
	m.row_version AS version,
	m.position_id AS position,
	m.mne_code
FROM
	org_position_mnecode m);

## 1.0.4
alter table org_person add user_id bigint default null;
alter table org_person add user_name varchar(256) default null;

## 1.0.5
CREATE INDEX ORG_COMPANY_LAY_REC_IDX ON ORG_COMPANY (LAY_REC) GO;

## 1.0.6
update org_position set name = '虚拟岗位' where sys_flag=1;
update org_department set name = '虚拟部门' where sys_flag=1;
update org_person set name = "虚拟人员", gender = null where sys_flag=1;

## 1.0.7
alter table org_person add entry_date varchar(64) default null;
alter table org_person add title varchar(64) default null;
alter table org_person add qualification varchar(256) default null;
alter table org_person add education varchar(64) default null;
alter table org_person add major varchar(256) default null;
alter table org_person add id_number varchar(32) default null;
create index id_number_index on org_person(id_number,valid);

## 1.0.8
alter table org_person add image_url varchar(256) default null;
alter table org_person add sign_pic_url varchar(256) default null;
alter table org_person alter column qualification varchar(1024) default null;
alter table org_person alter column major varchar(1024) default null;
alter table org_person alter column id_number varchar(1024) default null;

## 1.0.9
exec sp_rename 'org_person.image_url','avatar_url','column';
alter table org_person add entry_date varchar(64) default null;

## 1.0.10
alter table org_person alter column image_url varchar(2048);
alter table org_person alter column sign_pic_url varchar(2048);
alter table org_person alter column qualification varchar(2048);
alter table org_person alter column major varchar(2048);
alter table org_person alter column id_number varchar(2048);

## 1.0.11
create index code_index on org_person(code,valid);
create index position_id_index on org_person_position(position_id);

## 1.0.12
update org_position set name = '虚拟岗位' where sys_flag=1;
update org_department set name = '虚拟部门' where sys_flag=1;
update org_person set gender = null where sys_flag=1;