## 1.0.0
/*===================================================================*/
/* Table: 用户表(auth_user)                */
/*===================================================================*/
create table if not exists auth_user
(
    id                  bigint(20)            not null  comment '主键id',
    user_name           varchar(256)          not null comment '用户名',
    company_id          bigint(20)            default null comment '公司id',
    current_company_id  bigint(20)            default null comment '当前公司id',
    password            varchar(256)          not null comment '密码',
    person_id           bigint(20)            default null comment '人员id',
    has_lock            tinyint(1)            default 0 comment '是否锁定 1 锁定 0 未锁定',
    valid               tinyint(1)            default 1 comment '是否删除 0 删除 1 未删除',
    user_type           tinyint(4)            default 0 comment '0:普通用户,1:系统管理员,2:安全员,3:审计员',
    ldap_user_name      varchar(256)          default null comment 'ldap用户名',
    user_directory_id   bigint(20)            default null comment '用户目录id',
    lock_reason         tinyint(4)            default null comment '0:人为锁定,1:用户目录禁用',
    time_zone           varchar(64)           default null comment '时区',
    description         varchar(512)          default null comment '描述',
    creator             varchar(32)           not null default 'system' comment '创建者',
    modifier            varchar(32)           default null comment '修改者',
    create_time         timestamp             not null default current_timestamp comment '创建时间',
    modify_time         timestamp             not null default current_timestamp comment '修改时间',
    create_staff_id     bigint(20)            default null comment '创建者人员id',
    modify_staff_id     bigint(20)            default null comment '修改者人员id',
    constraint pk_user_id primary key (id),
    index idx_user_ldap_user_name (ldap_user_name),
    index idx_user_username_company (user_name,company_id),
    index idx_user_staff (person_id),
    unique index idx_user_name (user_name)
) engine = innodb  default charset = utf8 comment ='认证用户表';

alter table auth_user add column `login_first` tinyint(1) not null default '1' comment '是否锁定 0 不是第一次登录 1 第一次登录';
alter table auth_user add column `face_url` varchar(256)  default null comment '头像';

/*===================================================================*/
/* Table: 用户角色表(auth_user_role)                */
/*===================================================================*/
create table if not exists auth_user_role
(
    id                  bigint(20)            not null  comment '主键id',
    user_id             bigint(20)            not null comment '用户id',
    role_id             bigint(20)            not null comment '角色id',
    creator             varchar(32)           not null default 'system' comment '创建者',
    modifier            varchar(32)           default null comment '修改者',
    create_time         timestamp             not null default current_timestamp comment '创建时间',
    modify_time         timestamp             not null default current_timestamp comment '修改时间',
    create_staff_id     bigint(20)            default null comment '创建者人员id',
    modify_staff_id     bigint(20)            default null comment '修改者人员id',
    constraint pk_user_id primary key (id),
    index idx_user_role (user_id)
)engine = innodb
 default charset = utf8
 comment ='用户角色表';

alter table auth_user_role add column `role_type` tinyint(4)  default 1 comment '1:用户角色,2:岗位角色,3:用户岗位重复';

/*===================================================================*/
/* Table: 用户目录表(auth_user_directory)                */
/*===================================================================*/
create table if not exists auth_user_directory
(
    id                  bigint(20)            not null       comment '主键id',
    directory_name      varchar(256)          not null       comment '目录名称',
    directory_type      varchar(256)          default null   comment '目录类型',
    sort                double(26,6)          default null   comment '排序',
    enabled             tinyint(1)            default 0      comment '是否启用 1 启用 0 停用',
    valid               tinyint(1)            default 1      comment '是否删除 0 删除 1 未删除',
    description         varchar(512)          default null   comment '描述',
    hostname            varchar(256)          default null   comment '主机名',
    port                int                   default null   comment '端口号',
    enable_ssl          tinyint(1)            default 0      comment '是否启用ssl 0 未启用 1 启用',
    user_name           varchar(256)          default null   comment '用户名',
    password            varchar(256)          default null   comment '密码',
    base_dn             varchar(512)          default null   comment '基本dn',
    attach_user_dn      varchar(512)          default null   comment '附加用户dn',
    attach_group_dn     varchar(512)          default null   comment '附加组dn',
    permission          varchar(256)          default null   comment 'ldap权限',
    default_roles       varchar(512)          default null   comment '默认角色，使用半角逗号分隔',
    company_id          bigint(20)            not null       comment '企业id',
    creator             varchar(32)           not null default 'system' comment '创建者',
    modifier            varchar(32)           default null comment '修改者',
    create_time         timestamp             not null default current_timestamp comment '创建时间',
    modify_time         timestamp             not null default current_timestamp comment '修改时间',
    create_staff_id     bigint(20)            default null comment '创建者人员id',
    modify_staff_id     bigint(20)            default null comment '修改者人员id',
    constraint pk_user_directory_id primary key (id),
    index idx_user_directory_name (directory_name)
) engine = innodb  default charset = utf8 comment ='用户目录表';

/*===================================================================*/
/* Table: 在线用户表(auth_online_user)                */
/*===================================================================*/
create table if not exists auth_online_user (
    id                  bigint(20)            not null       comment '主键id',
    user_id             bigint(20)            not null       comment '用户id',
    user_name           varchar(256)          not null       comment '用户名',
    person_id           bigint(20)            default null       comment '人员id',
    person_code         varchar(50)           default null   comment  '人员编号',
    person_name         varchar(50)           default null   comment  '人员名称',
    company_id          bigint(20)            default null   comment '公司id',
    ticket              varchar(50)           not null       comment '用户会话凭证',
    login_ip            varchar(256)          not null       comment '登录ip',
    login_time          timestamp             not null       comment '登录时间',
    creator             varchar(32)           not null default 'system' comment '创建者',
    modifier            varchar(32)           default null comment '修改者',
    create_time         timestamp             not null default current_timestamp comment '创建时间',
    modify_time         timestamp             not null default current_timestamp comment '修改时间',
    create_staff_id     bigint(20)            default null comment '创建者人员id',
    modify_staff_id     bigint(20)            default null comment '修改者人员id',
    constraint pk_online_user_id primary key (id),
    index idx_online_user_username (user_name)
) engine = innodb  default charset = utf8 comment ='在线用户表';

alter table auth_online_user add column `device_type` varchar(256)   comment '1:用户角色,2:岗位角色,3:用户岗位重复';
alter table auth_online_user add column `access_token` varchar(4000)   comment '访问令牌';

/*===================================================================*/
/* Table: ip黑白名单表(auth_ip_black_white)                */
/*===================================================================*/
create table if not exists auth_ip_black_white (
    id                  bigint(20)            not null       comment '主键id',
    company_id          bigint(20)            default null   comment '公司id',
    ip                  varchar(256)          not null       comment '访问ip',
    control_type        tinyint(1)            not null       comment '管控模式 0:黑名单 1:白名单',
    creator             varchar(32)           not null default 'system' comment '创建者',
    modifier            varchar(32)           default null comment '修改者',
    create_time         timestamp             not null default current_timestamp comment '创建时间',
    modify_time         timestamp             not null default current_timestamp comment '修改时间',
    create_staff_id     bigint(20)            default null comment '创建者人员id',
    modify_staff_id     bigint(20)            default null comment '修改者人员id',
    constraint pk_ip_black_white_id primary key (id),
    unique index idx_company_ip (company_id, ip)
) engine = innodb  default charset = utf8 comment ='ip黑白名单表';

/*===================================================================*/
/* table: 身份提供者表(auth_identity_provider)                */
/*===================================================================*/
create table if not exists auth_identity_provider (
    id                  bigint(20)            not null        comment '主键id',
    name                varchar(256)          not null        comment '身份提供商名称',
    protocol_type       varchar(50)           not null        comment '协议类型 系统编码',
    auth_url            varchar(256)          not null        comment '授权地址',
    token_url           varchar(256)          default null    comment '获取token地址',
    profile_url         varchar(256)          default null    comment '获取用户信息地址',
    client_id           varchar(256)          default null    comment '客户端id',
    client_secret       varchar(256)          default null    comment '客户端密钥',
    redirect_uri        varchar(256)          default null    comment '回调地址',
    scope               varchar(256)          default null    comment '授权作用域',
    enabled             tinyint(1)            default 0       comment '是否启用 1 启用 0 停用',
    description         varchar(512)          default null    comment '描述',
    valid               tinyint(1)            default 1       comment '是否删除 0 删除 1 未删除',
    creator             varchar(32)           not null default 'system' comment '创建者',
    modifier            varchar(32)           default null comment '修改者',
    create_time         timestamp             not null default current_timestamp comment '创建时间',
    modify_time         timestamp             not null default current_timestamp comment '修改时间',
    create_staff_id     bigint(20)            default null comment '创建者人员id',
    modify_staff_id     bigint(20)            default null comment '修改者人员id',
    constraint pk_identity_provider_id primary key (id),
    index idx_identity_provider_name (name)
) engine = innodb  default charset = utf8 comment ='身份提供者表';

/*===================================================================*/
/* Table: 认证中心表(auth_center)                */
/*===================================================================*/
create table if not exists auth_center (
    id                  bigint(20)            not null       comment '主键id',
    name                varchar(256)          not null        comment '认证中心名称',
    protocol_type       varchar(50)           not null        comment '协议类型 系统编码',
    auth_url            varchar(256)          not null        comment '授权地址',
    token_url           varchar(256)          default null    comment '获取token地址',
    profile_url         varchar(256)          default null    comment '获取用户信息地址',
    description         varchar(512)          default null    comment '描述',
    valid               tinyint(1)            default 1       comment '是否删除 0 删除 1 未删除',
    creator             varchar(32)           not null default 'system' comment '创建者',
    modifier            varchar(32)           default null comment '修改者',
    create_time         timestamp             not null default current_timestamp comment '创建时间',
    modify_time         timestamp             not null default current_timestamp comment '修改时间',
    create_staff_id     bigint(20)            default null comment '创建者人员id',
    modify_staff_id     bigint(20)            default null comment '修改者人员id',
    constraint pk_auth_center_id primary key (id),
    index idx_auth_center_name (name)
) engine = innodb  default charset = utf8 comment ='认证中心表';

/*===================================================================*/
/* Table: 认证客户端表(auth_oauth_client)                */
/*===================================================================*/
create table if not exists auth_oauth_client (
    id                  bigint(20)            not null        comment '主键id',
    auth_center_id      bigint(20)            not null        comment '认证中心id',
    name                varchar(256)          not null        comment '客户端名称',
    grant_type          varchar(50)           not null        comment '授权类型 系统编码',
    client_id           varchar(256)          not null        comment '客户端id',
    client_secret       varchar(256)          default null    comment '客户端密钥',
    redirect_uri        varchar(256)          default null    comment '回调地址',
    scope               varchar(256)          default null    comment '授权作用域',
    expires_in          int(11)               default null    comment '有效期',
    auth_method         varchar(50)           not null        comment '认证方式 系统编码',
    enabled             tinyint(1)            default 0       comment '是否启用 1 启用 0 停用',
    description         varchar(512)          default null    comment '描述',
    valid               tinyint(1)            default 1       comment '是否删除 0 删除 1 未删除',
    creator             varchar(32)           not null default 'system' comment '创建者',
    modifier            varchar(32)           default null comment '修改者',
    create_time         timestamp             not null default current_timestamp comment '创建时间',
    modify_time         timestamp             not null default current_timestamp comment '修改时间',
    create_staff_id     bigint(20)            default null comment '创建者人员id',
    modify_staff_id     bigint(20)            default null comment '修改者人员id',
    constraint pk_oauth_client_id primary key (id),
    index idx_oauth_client_name (name),
    index idx_oauth_client_client_id (client_id)
) engine = innodb  default charset = utf8 comment ='认证客户端表';

insert into auth_user(id,user_name,company_id,password,user_type,person_id) select 1,'admin',1000,'$2a$10$QEd181jr.RNYME6hz/.xpONiMe3uGkI5sI8fjH5DQWwgwBKEs0/Cy',1,1 from dual where not exists(select user_name from auth_user where id=1);
update auth_user set person_id=1 where id=1;
/* Table: Excel导入记录表(auth_excel)           */
/*===================================================================*/
create table if not exists auth_excel (
    id                  bigint                not null comment '主键id',
    status              tinyint               not null  comment 'excel导入状态, 1进行中, 2成功, 3失败',
    file_name           varchar(256)          default null comment '文件名',
    add_num             int                   default null    comment '添加数目l',
    update_num          int                   default null  comment '更新数目',
    error_message       varchar(512)          default null comment '错误消息',
    excel_type          varchar(64)           default null comment '类型 import, export',
    creator             varchar(32)           not null default 'system' comment '创建者',
    modifier            varchar(32)           default null comment '修改者',
    create_time         timestamp             not null default current_timestamp comment '创建时间',
    modify_time         timestamp             not null default current_timestamp comment '修改时间',
    create_staff_id     bigint(20)            default null comment '创建者人员id',
    modify_staff_id     bigint(20)            default null comment '修改者人员id',
    primary key (id)
)
engine=innodb
default charset=utf8
comment='excel导入记录表';

/*==============================================================*/
/* Table: auth_user_config_dashboard                             */
/*==============================================================*/
create table if not exists auth_user_config_dashboard
(
   user_id             bigint(20)   not null,
   mkey                varchar(255) not null,
   fields              varchar(255) not null,
   config_info         text,
   constraint pk_userid_mkey primary key (user_id, mkey)
) engine=innodb default charset=utf8;

create or replace view base_userinfo (id, version, name, password, staff_id, locked, valid, timezone, create_staff_id, modify_staff_id, delete_staff_id, create_time, modify_time, delete_time, uuid, user_type, u_password, token_sn, three_role, state, pims_safe_access_id, passwordmodify_time, login_time, login_num, lock_time, language, is_allow_remote_access, remote_access_modified_flag) as
(
select
	id,
	0 as version,
	user_name as name,
	password,
	person_id as staff_id,
	has_lock as locked,
	valid,
	time_zone as timezone,
	null as create_staff_id,
	null as modify_staff_id,
	null as delete_staff_id,
	create_time,
	modify_time,
	null as delete_time,
	user_name as uuid,
	1 as user_type,
	null as u_password,
	null as token_sn,
	(case when user_type > 0 then 1 else 0 end) as three_role,
	0 as state,
	0 as pims_safe_access_id,
	null as passwordmodify_time,
	null as login_time,
	null as login_num,
	null as lock_time,
	'zh_CN' as language,
	0 as is_allow_remote_access,
	0 as remote_access_modified_flag
from auth_user
);

## 2.0.2
alter table auth_user drop ldap_user_name;
alter table auth_user drop user_directory_id;
create table if not exists auth_passwd_rules
(
    id                   bigint(20)          not null  comment '主键id',
    min_length           int                 not null comment '密码最小长度',
    max_length           int                 not null comment '密码最大长度',
    rule_type            tinyint(1)         comment '规则类型',
    contain_letter_case  tinyint(1)         comment '大小写',
    contain_numbers     tinyint(1)         comment '数字',
    contain_special_char tinyint(1)         comment '数字',
    regular_expression   varchar(100)       comment '正则表达式',
    hint          varchar(100)       comment '正则表达式',
    find_pwd_switch       tinyint(1)         default 1 comment '是否开启找回密码',
    creator              varchar(32)           not null default 'system' comment '创建者',
    modifier             varchar(32)           default null comment '修改者',
    create_time          timestamp             not null default current_timestamp comment '创建时间',
    modify_time          timestamp             not null default current_timestamp comment '修改时间',
    create_staff_id      bigint(20)            default null comment '创建者人员id',
    modify_staff_id      bigint(20)            default null comment '修改者人员id',
    primary key (id)
    ) engine = innodb  default charset = utf8 comment ='密码规则表';
insert into auth_passwd_rules (id, min_length, max_length,rule_type,contain_letter_case, contain_numbers, contain_special_char, find_pwd_switch) values(1,8,32,0,1,1,1,1);

## 3.0.2
alter table auth_user drop index idx_user_name;
alter table auth_user add  index idx_user_name (user_name);
alter table auth_user add column person_code varchar(50) default null;
alter table auth_user add column person_name varchar(50) default null;
##3.0.3
alter table auth_online_user add column `device_type` varchar(256)   comment '1:用户角色,2:岗位角色,3:用户岗位重复';
alter table auth_online_user add column `access_token` varchar(4000)   comment '访问令牌';
##3.0.5
alter table auth_user add column login_first tinyint(1) default '1' ;
alter table auth_user add column face_url varchar(256)  default null;
##3.0.6
alter table auth_online_user drop column access_token;
##3.0.7
create table identity_center_config(
    id              bigint                                  not null  primary key,
    oauth_name      varchar(64)                             null comment '第三方oauth中心名称',
    protocol_type   varchar(64)                             null comment '协议类型',
    system_name     varchar(64)                             null comment '应用名',
    system_flag     tinyint(1)  default 0                   null comment '内置',
    enable          tinyint(1)  default 0                   null comment '启用',
    app_id          varchar(512)                             null,
    app_secret      varchar(512)                             null,
    oauth_url       varchar(256)                            null comment 'oauth url',
    token_url       varchar(256)                            null comment '获取access tokenurl',
    userinfo_url    varchar(256)                            null comment '获取用户信息',
    qrcode_url      varchar(256)                            null comment '扫码登入的url',
    logout_url      varchar(256)                            null comment '登出',
    refresh_url     varchar(256)                            null comment 'refresh token ',
    redirect_url    varchar(256)                            null comment 'redirect_url token ',
    valid           tinyint(1)  default 1                   null comment '逻辑删除,是否有效0:无效 1:有效',
    qrcode_appid    varchar(128)                            null comment '金蝶云qrcode_appid',
    description     varchar(256)                            null,
    creator         varchar(32) default 'system'            not null comment '创建者',
    modifier        varchar(32)                             null comment '修改者',
    create_time     timestamp   default current_timestamp() not null comment '创建时间',
    modify_time     timestamp   default current_timestamp() not null comment '修改时间',
    create_staff_id bigint                                  null comment '创建者人员id',
    modify_staff_id bigint                                  null comment '修改者人员id'
) engine = innodb default charset = utf8 comment ='第三方认证中心配置表';

##3.0.8
drop table auth_identity_provider;
##4.0.0
alter table auth_user add third_identity varchar(128) null comment '第三方身份';
alter table auth_user add third_source varchar(128) null comment '第三方来源';
##4.1.0
/*====================================================*/
/* table: 登录日志表                                       */
/*====================================================*/
create table if not exists auth_login_log (
    id                  bigint(20)            not null       comment '主键id',
    user_id             bigint(20)            not null       comment '用户id',
    user_name           varchar(256)          not null       comment '用户名',
    ticket              varchar(50)           not null       comment '用户会话凭证',
    login_ip            varchar(256)          not null       comment '登录ip',
	device_type         varchar(32)           not null       comment '设备类型',
	login_type         	varchar(32)           not null       comment '登录类型',
	logout_type         varchar(32)       	  default null   comment '登出类型',
    login_time          timestamp             not null       comment '登录时间',
	logout_time         timestamp             null   		 comment '登出时间',
    creator             varchar(32)           not null default 'system' comment '创建者',
    modifier            varchar(32)           default null comment '修改者',
    create_time         timestamp             not null default current_timestamp comment '创建时间',
    modify_time         timestamp             not null default current_timestamp comment '修改时间',
    create_staff_id     bigint(20)            default null comment '创建者人员id',
    modify_staff_id     bigint(20)            default null comment '修改者人员id',
    constraint pk_login_log_id primary key (id),
    index idx_login_log_username (user_name)
) engine = innodb  default charset = utf8 comment ='登录日志表';