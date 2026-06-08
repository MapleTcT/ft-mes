## 1.0.0
/*===================================================================*/
/* table: 用户表(auth_user)                */
/*===================================================================*/
create table  auth_user                                            -- comment ='认证用户表';
(
    id                bigint       not null  ,                     -- comment '主键id',
    user_name         varchar(256)     not null ,                  -- comment '用户名',
    company_id        bigint       default null ,                  -- comment '公司id',
    current_company_id        bigint       default null ,                  -- comment '当前公司id',
    password          varchar(256)     not null ,                  -- comment '密码',
    face_url          varchar(256)     default null ,              -- comment '头像',
    person_id         bigint        default null ,                 -- comment '人员id',
    has_lock          tinyint       default 0 ,                    -- comment '是否锁定 1 锁定 0 未锁定',
    login_first       tinyint       default 1 ,                    -- comment '是否锁定 0 不是第一次登录 1 第一次登录',
    valid             tinyint       default 1 ,                    -- comment '是否删除 0 删除 1 未删除',
    user_type         tinyint       default 0 ,                    -- comment '0:普通用户,1:系统管理员,2:安全员,3:审计员',
    ldap_user_name    varchar(256)  default null,                  -- comment 'LDAP用户名',
    user_directory_id bigint        default null,                  -- comment '用户目录ID',
    lock_reason       tinyint       default null,                  -- comment '0:人为锁定,1:用户目录禁用',
    time_zone         varchar(64)      default null ,              -- comment '时区',
    description       varchar(512)     default null ,              -- comment '描述',
    creator           varchar(32)      default null ,              -- comment '创建者',
    modifier          varchar(32)      default null ,              -- comment '修改者',
    create_time       datetime        default getutcdate()   null, -- comment '创建时间',
    modify_time       datetime ,                                   -- comment '修改时间',
    create_staff_id   bigint      default null, --comment '创建者人员id',
    modify_staff_id   bigint      default null, --comment '修改者人员id',
    primary key (id)
);

-- 添加索引
create index idx_user_username_company on auth_user(user_name,company_id);
create index idx_user_ldap_user_name on auth_user(ldap_user_name);
create index idx_user_staff on auth_user(person_id);
create unique index idx_user_name on auth_user(user_name);


/*===================================================================*/
/* table: 用户角色表(auth_user_role)                */
/*===================================================================*/
create table  auth_user_role                             -- comment ='用户角色表';
(
    id              bigint          not null  ,                    -- comment '主键id',
    user_id         bigint          not null ,                     -- comment '用户id',
    role_id         bigint          not null ,                     -- comment '角色id',
    role_type       tinyint       default 1 ,                  --comment '1:用户角色,2:岗位角色,3:用户岗位重复',
    creator         varchar(32)     default null ,                 -- comment '创建者',
    modifier        varchar(32)     default null ,                 -- comment '修改者',
    create_time     datetime        default getutcdate()   null,   -- comment '创建时间',
    modify_time     datetime ,       -- comment '修改时间',
    create_staff_id bigint      default null, --comment '创建者人员id',
    modify_staff_id bigint      default null, --comment '修改者人员id',
    primary key (id)
);
-- 添加索引
create index idx_user_role on auth_user_role(user_id);



/*===================================================================*/
/* Table: 用户目录表(auth_user_directory)                */
/*===================================================================*/
create table  auth_user_directory                        -- comment ='用户目录表';
(
    id              bigint          not null ,              -- comment '主键ID',
    directory_name  varchar(256)    not null ,              -- comment '目录名称',
    directory_type  varchar(256)    default null ,          -- comment '目录类型',
    sort            decimal(26,6)   default null ,          -- comment '排序',
    enabled         tinyint         default 0 ,             -- comment '是否启用 1 启用 0 停用',
    valid           tinyint         default 1 ,             -- comment '是否删除 0 删除 1 未删除',
    description     varchar(512)    default null ,          -- comment '描述',
    hostname        varchar(256)    default null ,          -- comment '主机名',
    port            int             default null ,          -- comment '端口号',
    enable_ssl      tinyint         default 0 ,             -- comment '是否启用SSL 0 未启用 1 启用',
    user_name        varchar(256)   default null ,          -- comment '用户名',
    password        varchar(256)    default null ,          -- comment '密码',
    base_dn         varchar(512)    default null ,          -- comment '基本DN',
    attach_user_dn  varchar(512)    default null ,          -- comment '附加用户DN',
    attach_group_dn varchar(512)    default null ,          -- comment '附加组DN',
    permission      varchar(256)    default null ,          -- comment 'LDAP权限',
    default_roles   varchar(512)    default null ,          -- comment '默认角色，使用半角逗号分隔',
    company_id      bigint          not null ,              -- comment '企业ID',
    creator         varchar(32)     default null ,          -- comment '创建者',
    modifier        varchar(32)     default null ,          -- comment '修改者',
    create_time     datetime        default getutcdate(),	-- comment '创建时间',
    modify_time     datetime,                               -- comment '修改时间',
    create_staff_id bigint      default null, --comment '创建者人员id',
    modify_staff_id bigint      default null, --comment '修改者人员id',
    primary key (id)
);
create  index idx_user_directory_name on auth_user_directory(directory_name);


/*===================================================================*/
/* Table: 在线用户表(auth_online_user)                */
/*===================================================================*/
create table  auth_online_user (
    id              bigint      not null ,                  -- comment '主键ID',
    user_id         bigint      default null ,                  -- comment '用户ID',
    user_name       varchar(256)      default null ,            -- comment '用户名',
    person_id       bigint      default null ,                  -- comment '人员ID',
    person_code     varchar(50)    default null ,           -- comment  '人员编号',
    person_name     varchar(50)    default null ,           -- comment  '人员名称',
    company_id      bigint      default null ,              -- comment '公司ID',
    ticket          varchar(3000)    not null ,             -- comment '用户会话凭证',
    login_ip        varchar(256)    not null ,              -- comment '登录IP',
    login_time      datetime       not null ,               -- comment '登录时间',
    creator         varchar(32)     default null ,          -- comment '创建者',
    modifier        varchar(32)     default null ,          -- comment '修改者',
    create_time     datetime        default getutcdate(),	-- comment '创建时间',
    modify_time     datetime,                               -- comment '修改时间',
    create_staff_id bigint      default null, --comment '创建者人员id',
    modify_staff_id bigint      default null, --comment '修改者人员id',
    primary key (id)
) ;
create  index idx_online_user_username on auth_online_user(user_name);

alter table auth_online_user add column `access_token` varchar(4000)  -- COMMENT '访问令牌';



/*===================================================================*/
/* Table: 身份提供者表(auth_identity_provider)                */
/*===================================================================*/
create table  auth_identity_provider (
    id              bigint      not null ,                  -- comment '主键ID',
    name            varchar(256)    not null ,              -- comment '身份提供商名称',
    protocol_type   varchar(50)     not null ,              -- comment '协议类型 系统编码',
    auth_url        varchar(256)    not null ,              -- comment '授权地址',
    token_url       varchar(256)    default null ,          -- comment '获取token地址',
    profile_url     varchar(256)    default null ,          -- comment '获取用户信息地址',
    client_id       varchar(256)    default null ,          -- comment '客户端id',
    client_secret   varchar(256)    default null ,          -- comment '客户端密钥',
    redirect_uri    varchar(256)    default null ,          -- comment '回调地址',
    scope           varchar(256)    default null ,          -- comment '授权作用域',
    enabled         tinyint      default 0,                 -- comment '是否启用 1 启用 0 停用',
    description     varchar(512)    default null ,          -- comment '描述',
    valid           tinyint      default 1,                 -- comment '是否删除 0 删除 1 未删除',
    creator         varchar(32)     default null ,          -- comment '创建者',
    modifier        varchar(32)     default null ,          -- comment '修改者',
    create_time     datetime        default getutcdate(),	-- comment '创建时间',
    modify_time     datetime,                               -- comment '修改时间',
    create_staff_id bigint      default null, --comment '创建者人员id',
    modify_staff_id bigint      default null, --comment '修改者人员id',
    primary key (id)
) ;                                                         -- comment ='身份提供者表';
create index idx_identity_provider_name on auth_identity_provider(name);

/*===================================================================*/
/* Table: 认证中心表(auth_center)                */
/*===================================================================*/
create table  auth_center (
    id              bigint          not null ,             -- comment '主键ID',
    name            varchar(256)    not null ,             -- comment '认证中心名称',
    protocol_type   varchar(50)     not null ,             -- comment '协议类型 系统编码',
    auth_url        varchar(256)    not null ,             -- comment '授权地址',
    token_url       varchar(256)    default null ,         -- comment '获取token地址',
    profile_url     varchar(256)    default null ,         -- comment '获取用户信息地址',
    description     varchar(512)    default null ,         -- comment '描述',
    valid           tinyint         default 1 ,            -- comment '是否删除 0 删除 1 未删除',
    creator         varchar(32)     default null ,         -- comment '创建者',
    modifier        varchar(32)     default null ,         -- comment '修改者',
    create_time     datetime        default getutcdate(),  -- comment '创建时间',
    modify_time     datetime,                              -- comment '修改时间',
    create_staff_id bigint      default null, --comment '创建者人员id',
    modify_staff_id bigint      default null, --comment '修改者人员id',
     primary key (id)
) ;                                         -- comment ='认证中心表';
create index idx_auth_center_name on auth_center(name);

/*===================================================================*/
/* Table: 认证客户端表(auth_oauth_client)                */
/*===================================================================*/
create table  auth_oauth_client (
    id              bigint      not null,                   -- comment '主键ID',
    auth_center_id  bigint      not null,                   -- comment '认证中心id',
    name            varchar(256)    not null,               -- comment '客户端名称',
    grant_type      varchar(50)     not null,               -- comment '授权类型 系统编码',
    client_id       varchar(256)    not null,               -- comment '客户端id',
    client_secret   varchar(256)    default null,           -- comment '客户端密钥',
    redirect_uri    varchar(256)    default null,           -- comment '回调地址',
    scope           varchar(256)    default null,           -- comment '授权作用域',
    expires_in      int         default null,               -- comment '有效期',
    auth_method     varchar(50)     not null,               -- comment '认证方式 系统编码',
    enabled         tinyint      default 0,                 -- comment '是否启用 1 启用 0 停用',
    description     varchar(512)    default null,           -- comment '描述',
    valid           tinyint      default 1,                 -- comment '是否删除 0 删除 1 未删除',
    creator         varchar(32)     default null,           -- comment '创建者',
    modifier        varchar(32)     default null,           -- comment '修改者',
    create_time     datetime       default getutcdate(),	-- comment '创建时间',
    modify_time     datetime,                               -- comment '修改时间',
    create_staff_id bigint      default null, --comment '创建者人员id',
    modify_staff_id bigint      default null, --comment '修改者人员id',
    primary key (id)
) ;                                             -- comment ='认证客户端表';
create index idx_oauth_client_name on auth_oauth_client(name);
create index idx_oauth_client_client_id on auth_oauth_client(client_id);

/*===================================================================*/
/* Table: Excel导入记录表(auth_excel)           */
/*===================================================================*/
create table  auth_excel (
    id BIGINT NOT NULL,                           -- comment '主键ID',
    status TINYINT NOT NULL ,                     -- comment 'Excel导入状态, 1进行中, 2成功, 3失败',
    file_name VARCHAR(256) DEFAULT NULL,          -- comment '文件名',
    add_num int DEFAULT NULL,                     -- comment '添加数目l',
    update_num int DEFAULT NULL,                  -- comment '更新数目',
    error_message VARCHAR(512) DEFAULT NULL,      -- comment '错误消息',
    excel_type VARCHAR(64) DEFAULT NULL,          -- comment '类型 import, export',
    creator VARCHAR(32) DEFAULT NULL,             -- comment '创建者',
    modifier VARCHAR(32) DEFAULT NULL,            -- comment '修改者',
    create_time datetime,                         -- comment '创建时间',
    modify_time datetime,                         -- comment '修改时间',
    create_staff_id bigint      default null, --comment '创建者人员id',
    modify_staff_id bigint      default null, --comment '修改者人员id',
    PRIMARY KEY (id)
);                                                -- comment='Excel导入记录表';

/*==============================================================*/
/* Table: auth_user_config_dashboard                             */
/*==============================================================*/
create table auth_user_config_dashboard
(
   user_id             bigint not null,
   mkey                varchar(255) not null,
   fields              varchar(255) not null,
   config_info         varchar(max),
   CONSTRAINT pk_userId_mkey PRIMARY KEY (user_id, mkey)
);


## 2.0.2
/*===================================================================*/
/* table: 密码规则表(auth_passwd_rules)           */
/*===================================================================*/
create table auth_passwd_rules
(
   id              bigint 			not null,
   min_length      int 				not null,
   max_length      int 				not null,
   rule_type        tinyint,
   contain_letter_case  tinyint,
   contain_numbers    tinyint,
   contain_special_char  tinyint,
   regular_expression  varchar(100),
   hint          varchar(100),
   find_pwd_switch      tinyint 	default 1,
    creator         varchar(32)     default null ,
    modifier        varchar(32)     default null ,
    create_time     datetime        default getutcdate(),
    modify_time     datetime,
    create_staff_id bigint      	default null,
    modify_staff_id bigint      	default null,
   primary key(id)
);
EXECUTE SP_ADDEXTENDEDPROPERTY N'MS_DESCRIPTION', '密码规则表', N'USER', N'DBO', N'TABLE', N'AUTH_PASSWD_RULES', NULL, NULL;
EXECUTE SP_ADDEXTENDEDPROPERTY N'MS_DESCRIPTION', '密码最小长度', N'USER', N'DBO', N'TABLE', N'AUTH_PASSWD_RULES', N'COLUMN', N'min_length';
EXECUTE SP_ADDEXTENDEDPROPERTY N'MS_DESCRIPTION', '密码最大长度', N'USER', N'DBO', N'TABLE', N'AUTH_PASSWD_RULES', N'COLUMN', N'max_length';
EXECUTE SP_ADDEXTENDEDPROPERTY N'MS_DESCRIPTION', '规则类型', N'USER', N'DBO', N'TABLE', N'AUTH_PASSWD_RULES', N'COLUMN', N'rule_type';
EXECUTE SP_ADDEXTENDEDPROPERTY N'MS_DESCRIPTION', '大小写', N'USER', N'DBO', N'TABLE', N'AUTH_PASSWD_RULES', N'COLUMN', N'contain_letter_case';
EXECUTE SP_ADDEXTENDEDPROPERTY N'MS_DESCRIPTION', '数字', N'USER', N'DBO', N'TABLE', N'AUTH_PASSWD_RULES', N'COLUMN', N'contain_numbers';
EXECUTE SP_ADDEXTENDEDPROPERTY N'MS_DESCRIPTION', '特殊字符', N'USER', N'DBO', N'TABLE', N'AUTH_PASSWD_RULES', N'COLUMN', N'contain_special_char';
EXECUTE SP_ADDEXTENDEDPROPERTY N'MS_DESCRIPTION', '正则表达式', N'USER', N'DBO', N'TABLE', N'AUTH_PASSWD_RULES', N'COLUMN', N'regular_expression';
EXECUTE SP_ADDEXTENDEDPROPERTY N'MS_DESCRIPTION', '校验提示语', N'USER', N'DBO', N'TABLE', N'AUTH_PASSWD_RULES', N'COLUMN', N'hint';
EXECUTE SP_ADDEXTENDEDPROPERTY N'MS_DESCRIPTION', '是否开启找回密码', N'USER', N'DBO', N'TABLE', N'AUTH_PASSWD_RULES', N'COLUMN', N'find_pwd_switch';
alter table auth_user drop column user_directory_id;
alter table auth_user drop column ldap_user_name;

if not exists (select * from auth_user where user_name='admin')  insert into auth_user(id,user_name,company_id,password,user_type,person_id) values(1,'admin',1000,'$2a$10$QEd181jr.RNYME6hz/.xpONiMe3uGkI5sI8fjH5DQWwgwBKEs0/Cy',1,1);

## 2.0.3
alter table auth_online_user add access_token varchar(4000);
alter table auth_online_user add device_type varchar(256);
## 2.0.4
alter table auth_user add person_code varchar(50) default null;
alter table auth_user add person_name varchar(50) default null;
drop index idx_user_name on auth_user;
create index idx_user_name on auth_user (user_name);
## 2.0.5
insert into auth_passwd_rules
(id, min_length, max_length,rule_type,contain_letter_case, contain_numbers, contain_special_char, find_pwd_switch)
values(1,8,32,0,1,1,1,1);
##2.0.6
alter table auth_online_user drop column access_token


##2.0.7
create view base_userinfo (id, version, name, password, staff_id, locked, valid, timezone, create_staff_id, modify_staff_id, delete_staff_id, create_time, modify_time, delete_time, uuid, user_type, u_password, token_sn, three_role, state, pims_safe_access_id, passwordmodify_time, login_time, login_num, lock_time, language, is_allow_remote_access, remote_access_modified_flag) as
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

##2.0.8
create table  auth_ip_black_white (
    id              bigint          not null ,              -- comment '主键ID',
    company_id      bigint          default null ,          -- comment '公司ID',
    ip              varchar(256)    not null ,              -- comment '访问IP',
    control_type    tinyint        not null ,               -- comment '管控模式 0:黑名单 1:白名单',
    creator         varchar(32)     default null ,          -- comment '创建者',
    modifier        varchar(32)     default null ,          -- comment '修改者',
    create_time     datetime        default getutcdate(),	-- comment '创建时间',
    modify_time     datetime ,                              -- comment '修改时间',
    create_staff_id bigint      default null,               --comment '创建者人员id',
    modify_staff_id bigint      default null,               --comment '修改者人员id',
   primary key (id)
);
create index idx_ip_black_white_ip on auth_ip_black_white(ip);
create unique index idx_company_ip on auth_ip_black_white(company_id, ip);

##2.0.9
create view base_userinfo (id, version, name, password, staff_id, locked, valid, timezone, create_staff_id, modify_staff_id, delete_staff_id, create_time, modify_time, delete_time, uuid, user_type, u_password, token_sn, three_role, state, pims_safe_access_id, passwordmodify_time, login_time, login_num, lock_time, language, is_allow_remote_access, remote_access_modified_flag) as
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

##2.1.1
alter table auth_user add login_first tinyint default '1' ;
alter table auth_user add face_url varchar(256)  default null;

##2.1.2
alter table auth_online_user drop column access_token;
##3.0.7
create table identity_center_config
(
    id              bigint                        not null,
    oauth_name      varchar(192)                  not null, -- comment '第三方oauth中心名称',
    protocol_type   varchar(64)                   null,     -- comment '协议类型',
    system_name     varchar(192)                  not null, -- comment '应用名',
    system_flag     tinyint      default 0        null,     -- comment '内置',
    enable          tinyint      default 0        null,-- comment '启用',
    app_id          varchar(512)  default null,
    app_secret      varchar(512)  default null,
    oauth_url       varchar(256) default null,              -- comment 'oauth url',
    token_url       varchar(256) default null,-- comment '获取access tokenurl',
    userinfo_url    varchar(256) default null,-- comment '获取用户信息',
    qrcode_url      varchar(256) default null,-- comment '扫码登入的url',
    logout_url      varchar(256) default null,-- comment '登出',
    refresh_url     varchar(256) default null,-- comment 'refresh token ',
    redirect_url    varchar(256)                  null,-- comment 'redirect_url token ',
    qrcode_appid    varchar(128) default null,-- comment '金蝶云qrcode_appid',
    valid           tinyint      default 1        null,-- comment '逻辑删除,是否有效0:无效 1:有效',
    description     varchar(256) default null,
    creator         varchar(32)  default 'system' not null,-- comment '创建者',
    modifier        varchar(32)  default null,-- comment '修改者',
    create_time     datetime,-- comment '创建时间',
    modify_time     datetime,                               -- comment '修改时间',
    create_staff_id bigint                        null,-- comment '创建者人员id',
    modify_staff_id bigint                        null,-- comment '修改者人员id',
    PRIMARY KEY (id)
);
##3.0.8
drop table auth_identity_provider;
##4.0.0
alter table auth_user add third_identity varchar(128) default null; -- comment '第三方身份'
alter table auth_user add third_source varchar(128) default null; -- comment '第三方来源'
##4.1.0
create table auth_login_log (								-- comment '登录日志表',
    id                  bigint            not null,       -- comment '主键id',
    user_id             bigint            not null,       -- comment '用户id',
    user_name           varchar(256)          not null,       -- comment '用户名',
    ticket              varchar(50)           not null,       -- comment '用户会话凭证',
    login_ip            varchar(256)          not null,       -- comment '登录ip',
	device_type         varchar(32)           not null,       -- comment '设备类型',
	login_type         	varchar(32)           not null,       -- comment '登录类型',
	logout_type         varchar(32)       	  default null,   -- comment '登出类型',
    login_time          datetime             not null,       -- comment '登录时间',
	logout_time         datetime             null,   		 -- comment '登出时间',
    creator         varchar(32)     default null ,  		-- comment '创建者',
    modifier        varchar(32)     default null ,  		-- comment '修改者',
    create_time     datetime,								-- comment '创建时间',
    modify_time     datetime,       						-- comment '修改时间',
    create_staff_id   bigint      default null, 		--comment '创建者人员id',
    modify_staff_id   bigint      default null, 		--comment '修改者人员id',
    primary key (id)
);
create  index idx_login_log_username on auth_login_log(user_name);