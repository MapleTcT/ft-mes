## 1.0.0
/*==============================================================*/
/* table: tenant_server                                         */
/*==============================================================*/
create table if not exists scheduler_job_info
(
   id                   bigint(20) not null comment 'id',
   model_name           varchar(64) not null comment '模块名称',
   job_name             varchar(64) not null comment '任务名称',
   code                 varchar(64) not null comment '任务编码',
   job_key              varchar(64) not null comment '任务名称国际化key值',
   job_desc             varchar(128) comment '任务说明',
   job_cron             varchar(64) not null comment 'cron表达式',
   job_status           int default 0 comment '任务状态',
   job_service_api      varchar(64) not null comment '远程http调用接口',
   job_service_params   varchar(64)  comment '参数',
   job_call_no          bigint(20) default 0 comment '调用次数',
   last_time            timestamp(0) not null default '0000-00-00 00:00:00' comment '修改时间',
   next_time            timestamp(0) not null default '0000-00-00 00:00:00' comment '下次调用时间',
   user_name            varchar(64) not null comment '租户名称',
   delete_time          timestamp comment '删除时间',
   modify_time          timestamp comment '修改时间',
   create_time          timestamp comment '创建时间',
   terminator           varchar(32) comment '删除者',
   modifier             varchar(32) comment '修改者',
   creator              varchar(32) comment '创建者',
   create_staff_id      bigint comment '创建者人员id',
   modify_staff_id      bigint comment '修改者人员id',
   primary key(id)
)
engine=innodb
default charset=utf8
comment='调度任务表';
create index index_jobinfo_code on scheduler_job_info(code);

create table if not exists scheduler_job_log_info
(
   id                   bigint(20) not null comment 'id',
   model_name           varchar(64) not null comment '模块名称',
   job_name             varchar(64) not null comment '任务名称',
   code                 varchar(64) not null comment '任务编码',
   job_service_api      varchar(64) not null comment '远程http调用接口',
   job_service_params   varchar(64)  comment '参数',
   job_message          varchar(64)  comment 'job信息',
   job_status           int  comment '任务状态',
   exception_info       varchar(200)  comment '异常信息',
   user_name            varchar(64) not null comment '租户名称',
   delete_time          timestamp comment '删除时间',
   modify_time          timestamp comment '修改时间',
   create_time          timestamp comment '创建时间',
   terminator           varchar(32) comment '删除者',
   modifier             varchar(32) comment '修改者',
   creator              varchar(32) comment '创建者',
   create_staff_id      bigint comment '创建者人员id',
   modify_staff_id      bigint comment '修改者人员id',
   primary key(id)
)
engine=innodb
default charset=utf8
comment='调度任务日志表';

create index index_jobloginfo_code on scheduler_job_log_info(code);
create index index_jobloginfo_model_name on scheduler_job_log_info(model_name);
create index index_jobloginfo_job_status on scheduler_job_log_info(job_status);



alter table scheduler_job_info modify job_name varchar(256);
alter table scheduler_job_info modify code varchar(256);
alter table scheduler_job_info modify job_desc varchar(510);
alter table scheduler_job_info modify job_service_api varchar(510);
alter table scheduler_job_info modify job_service_params varchar(510);
alter table scheduler_job_log_info modify job_name varchar(256);
alter table scheduler_job_log_info modify code varchar(256);
alter table scheduler_job_log_info modify job_service_api varchar(510);
alter table scheduler_job_log_info modify job_service_params varchar(510);
alter table scheduler_job_log_info modify job_message varchar(510);
alter table scheduler_job_log_info modify exception_info varchar(510);
## 1.0.1
alter table scheduler_job_info modify job_cron varchar(510);
## 1.0.2
alter table scheduler_job_info add module_code varchar(256);
## 1.0.3
alter table scheduler_job_info modify job_cron varchar(550);
## 1.0.7
alter table scheduler_job_info modify create_time timestamp not null default current_timestamp;
alter table scheduler_job_info modify modify_time timestamp not null default current_timestamp;
alter table scheduler_job_info modify delete_time timestamp null default null;
alter table scheduler_job_log_info modify create_time timestamp not null default current_timestamp;
alter table scheduler_job_log_info modify modify_time timestamp not null default current_timestamp;
alter table scheduler_job_log_info modify delete_time timestamp null default null;

## 1.1.0
update scheduler_job_info sji set sji.module_code = (select artifact from ec_module em where em.code = sji.module_code) where exists(select artifact from ec_module em where em.code = sji.module_code);
## 1.1.1
alter table scheduler_job_info modify last_time timestamp not null default current_timestamp;
alter table scheduler_job_info modify next_time timestamp not null default current_timestamp;