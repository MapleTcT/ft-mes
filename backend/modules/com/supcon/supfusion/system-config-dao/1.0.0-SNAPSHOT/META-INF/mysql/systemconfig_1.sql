## 1.0.0
/*===================================================================*/
/* table: 配置表                                                      */
/*===================================================================*/
create table if not exists systemconfig_config_info(
    id              bigint(20)            not null comment '主键',
    catalog_id      bigint(20)            default null comment '配置中心分类id',
    sort            double                default null comment '顺序',
    code            varchar(56)           default null comment '业务唯一性标识',
    name            varchar(256)          default null comment '国际化编码',
    app_code        varchar(56)           default null comment 'app标识',
    module_code     varchar(256)          default null comment '模块code',
    widget_type     tinyint(4)            default null comment '控件类型(0:默认输入框,1:单选checkbox,2:单选radio,3:下拉多选,4:下拉单选,5:时间框,6:自定义控件)',
    default_value   varchar(4000)         default null comment '缺省值',
    widget_value    varchar(4000)         default null comment '值',
    max_value       int(11)               default null comment '最大值',
    min_value       int(11)               default null comment '最小值',
    reg_format      varchar(256)          default null comment '格式校验',
    reg_message     varchar(256)          default null comment '提示语',
    has_require     tinyint(1)            default null comment '是否必填',
    custom          text                  default null comment 'json字符串  保存自定义控件渲染内容',
    description     varchar (256)         default null comment '配置项说明',
    creator         varchar(256)          not null default 'system',
    create_time     timestamp             not null default current_timestamp(),
    modifier        varchar(256)          default null,
    modify_time     timestamp             not null default current_timestamp(),
    create_staff_id bigint(20)            default null comment '创建者人员id',
    modify_staff_id bigint(20)            default null comment '修改者人员id',
    constraint unique index udx_sysconf_code(app_code,code),
    primary key (id)
) engine = innodb default charset = utf8 comment ='系统配置表';

insert into systemconfig_config_info(id, catalog_id, sort, code, name, app_code, module_code, widget_type, default_value, widget_value, max_value, min_value, reg_format, reg_message, has_require, custom, description )
select 1, 4, 1, 'spreadjs.licence', 'spreadjs.licence', 'printer', 'printer', 7, '', '', null, null, null, null, 1, null, '' from dual where not exists (select module_code from systemconfig_config_info where code ='spreadjs.licence');

alter table systemconfig_config_info modify default_value varchar(4000) default null;
alter table systemconfig_config_info modify widget_value varchar(4000) default null;

/*===================================================================*/
/* table: 配置版本表                                                   */
/*===================================================================*/
create table if not exists systemconfig_config_version (
    id              bigint(20)            not null comment '主键',
    config_version  varchar(256)          not null comment '配置版本',
    tid_module_key  varchar(256)          not null comment '租户模块key',
    creator         varchar(256)          not null default 'system',
    create_time     timestamp             not null default current_timestamp(),
    modifier        varchar(256)          default null,
    modify_time     timestamp             not null default current_timestamp(),
    create_staff_id bigint(20)            default null comment '创建者人员id',
    modify_staff_id bigint(20)            default null comment '修改者人员id',
    primary key (id)
) engine = innodb default charset = utf8 comment = '配置中心版本表';

/*===================================================================*/
/* table: 配置值表                                                    */
/*===================================================================*/
create table if not exists systemconfig_config_option(
    id              bigint(20)            not null  comment '主键',
    config_id       bigint(20)            default null comment '	系统配置id',
    sort            double                default null comment '顺序',
    label           varchar(256)          default null comment '国际化编码',
    select_value    varchar(256)          default null comment '值',
    creator         varchar(256)          not null default 'system',
    create_time     timestamp             not null default current_timestamp(),
    modifier        varchar(256)          default null,
    modify_time     timestamp             not null default current_timestamp(),
    create_staff_id bigint(20)            default null comment '创建者人员id',
    modify_staff_id bigint(20)            default null comment '修改者人员id',
    primary key (id)
) engine = innodb default charset = utf8 comment ='配置中心下拉选择表';

/*===================================================================*/
/* table: 配置类别表                                                   */
/*===================================================================*/
create table if not exists systemconfig_config_catalog(
    id              bigint(20)            not null comment '主键',
    parent_id       bigint(20)            default null comment '父配置id',
    sort            double                default null comment '顺序',
    code            varchar(256)          default null comment '业务唯一性标识',
    name            varchar(256)          default null comment '国际化编码',
    app_code        varchar(256)          default null comment 'app标识',
    catalog_type    tinyint(4)            default null comment '0:系统配置，1：app配置',
    has_hide        tinyint(1)            default 0,
    creator         varchar(256)          not null default 'system',
    create_time     timestamp             not null default current_timestamp(),
    modifier        varchar(256)          default null,
    modify_time     timestamp             not null default current_timestamp(),
    create_staff_id bigint(20)            default null comment '创建者人员id',
    modify_staff_id bigint(20)            default null comment '修改者人员id',
    constraint unique index udx_sysconf_code(app_code,code),
    primary key (id)
) engine = innodb default charset = utf8 comment ='配置中心分类';

insert into systemconfig_config_catalog(id,sort,code,name,app_code,catalog_type,has_hide) select 1, 1,'system','systemConfig.system','system',1,0 from DUAL where not exists(select code from systemconfig_config_catalog where code='system');
--insert into systemconfig_config_catalog(id,sort,code,parent_id,name,app_code,catalog_type,has_hide) select 3, 2,'loginmethod',1,'systemConfig.loginmethod','system',1,0 from DUAL where not exists(select code from systemconfig_config_catalog where code='systemConfig.loginmethod');
--insert into systemconfig_config_info(id,catalog_id,sort,code,app_code,widget_type,custom) select 1,3,1,'loginmethod','loginmethod',6,'{"systemConfig.loginMethod":"","systemConfig.copyRight":"","systemConfig.logo":"","systemConfig.background":"","systemConfig.slogan":"","systemConfig.title":"","systemConfig.home":"","systemConfig.navigation":"","systemConfig.forgetPassword":"","systemConfig.rememberName":""}' from DUAL where not exists(select code from systemconfig_config_catalog where code='loginmethod' and app_code='loginmethod');
--insert into systemconfig_config_catalog(id,sort,code,parent_id,name,app_code,catalog_type,has_hide) select 4, 3,'loginsecurity',1,'systemConfig.loginsecurity','system',1,0 from DUAL where not exists(select code from systemconfig_config_catalog where code='systemConfig.loginsecurity');
--insert into systemconfig_config_info(id,catalog_id,sort,code,app_code,widget_type,custom) select 2,4,1,'loginsecurity','loginsecurity',6,'{"systemConfig.multifactor":"","systemConfig.passwordComplexity":"","systemConfig.passwordLength":"","systemConfig.passwordPeriod":"","systemConfig.passwordHistory":"","systemConfig.passwordLock":"","systemConfig.passwordLockTime":"","systemConfig.sso":""}' from DUAL where not exists(select code from systemconfig_config_catalog where code='loginsecurity' and app_code='loginsecurity');
insert into systemconfig_config_catalog(id,sort,code,name,app_code,catalog_type,has_hide) select 2,2,'app','systemConfig.app','app',2,0 from DUAL where not exists(select code from systemconfig_config_catalog where code='app');
insert into systemconfig_config_catalog(id,sort,code,parent_id,name,app_code,catalog_type,has_hide) select 3, 2,'userAD',1,'systemConfig.userAD','system',1,0 from DUAL where not exists(select code from systemconfig_config_catalog where code='userAD');
insert into systemconfig_config_catalog(id,sort,code,parent_id,name,app_code,catalog_type,has_hide) select 4, 3,'printer',1,'systemConfig.printer','printer',1,0 from DUAL where not exists(select code from systemconfig_config_catalog where code='spreadjs.licence');
insert into systemconfig_config_catalog(id,sort,code,parent_id,name,app_code,catalog_type,has_hide) select 5, 4,'AKSK',1,'systemConfig.AKSK','AKSK',1,0 from DUAL where not exists(select code from systemconfig_config_catalog where code='AKSK');
insert into systemconfig_config_catalog(id,sort,code,parent_id,name,app_code,catalog_type,has_hide) select 6, 5,'baseImages',1,'systemConfig.baseImages','baseImages',1,0 from DUAL where not exists(select code from systemconfig_config_catalog where code='baseImages');
insert into systemconfig_config_catalog(id,sort,code,parent_id,name,app_code,catalog_type,has_hide) select 7, 6,'passwordConfig',1,'systemConfig.passwordConfig','passwordConfig',1,0 from DUAL where not exists(select code from systemconfig_config_catalog where code='passwordConfig');

## 1.0.1
update systemconfig_config_info set default_value='192.*|10.*,615382821284924#B0vEDu3Wex5Wd6hFWLB5Z5U4KpRTUpFGSXdkMwFGOlJDcXZ4Mv3SSEJXOv96KHhmR7s6V8oGarkmRI5UZ7xUUxdGUPVXV8l4MFVmehlkQSJ6THN6Q7RDSSRkSPtSYWl7TxJWSyAjN0BDe9EXc4cUbDd5czhGSYBDNXZDMhh7N8cDWuNVTol7MOpENGBzT0RkRx56YxEjMVtUY5Nja424RhpFMNNFNtpnRlpnaOFDND56TrYVNZVnU6Z5RzZWT6AjVwlGcPFHRWpHMMtEWxc6blFnV8I4LsV7c0NVQPhTO9gHMDp6a5hjUrUXVWhzaF36ST5kVHVmI0IyUiwiI7QzMFZkRyEjI0ICSiwiM6gDM8MTM9ETM0IicfJye&Qf35VfiU5TzYjI0IyQiwiIzEjL6ByUKBCZhVmcwNlI0IiTis7W0ICZyBlIsIyMyIzMxADIxMjMxAjMwIjI0ICdyNkIsIiKuATMsoiLykTMiojIz5GRiwiI8+Y9sWY9QmZ0Jyp93uL9hKI0vyp9Aqo9n0o9tiL9fGr9ZWr9iojIh94QiwiI4ITO4gjMxIDOygzM5EjNiojIklkIs4XZzxWYmpjIyNHZisnOiwmbBJye0ICRiwiI34TQwh7QV94bwVEbE3URFJlUDB7dXJ5MB9Wd9Q4M854U7YGUBNDdwh5UPVVb4InUshzMI3ya6IGUYVFOzlnWrgWRlZGezolYhZEZQJTZ58mezk7R7JTQjxWWBx4cZN7LLwFjn'
,description='默认授权key只适用于192/10网段，如果其它网段的需求，请独立申请。' where id=1;