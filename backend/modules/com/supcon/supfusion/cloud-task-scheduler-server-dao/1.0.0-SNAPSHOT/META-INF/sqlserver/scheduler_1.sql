## 1.0.0
create table scheduler_job_info
(
   id                   bigint not null,
   model_name           varchar(64)  not null,
   job_name             varchar(256)  not null ,
   code                 varchar(256) not null,
   job_key              varchar(64)  not null ,
   job_desc             varchar(510) ,
   job_cron             varchar(64) not null ,
   job_status           int default 0 ,
   job_service_api      varchar(510) not null ,
   job_service_params   varchar(510)  ,
   job_call_no          bigint ,
   last_time            datetime,
   next_time            datetime,
   user_name            varchar(64) not null,
   creator              varchar(32) default null,
   modifier             varchar(32) default null,
   terminator           varchar(32) default null,
   create_time          datetime,
   modify_time          datetime,
   delete_time          datetime,
   create_staff_id bigint ,
   modify_staff_id bigint ,
   primary key(id)
);

create index index_jobinfo_code on scheduler_job_info(code);

create table scheduler_job_log_info
(
   id                   bigint not null ,
   model_name           varchar(64) not null,
   job_name             varchar(256) not null,
   code                 varchar(256) not null,
   job_service_api      varchar(510) not null,
   job_service_params   varchar(510) ,
   job_message          varchar(510) ,
   job_status           int,
   exception_info       varchar(510) ,
   user_name            varchar(64) ,
   creator              varchar(32) default null,
   modifier             varchar(32) default null,
   terminator           varchar(32) default null,
   create_time          datetime,
   modify_time          datetime,
   delete_time          datetime,
   create_staff_id bigint ,
   modify_staff_id bigint ,
   primary key(id)
);

create index index_jobloginfo_code on scheduler_job_log_info(code);
create index index_jobloginfo_model_name on scheduler_job_log_info(model_name);
create index index_jobloginfo_job_status on scheduler_job_log_info(job_status);
## 1.0.2
alter table scheduler_job_info alter column job_cron varchar(510) not null;
## 1.0.3
alter table scheduler_job_info add module_code varchar(256);
## 1.0.4
alter table scheduler_job_info alter column job_cron varchar(550) not null;
## 1.1.0
update sji set sji.module_code = (select artifact from ec_module em where em.code = sji.module_code)from scheduler_job_info sji where exists(select artifact from ec_module em where em.code = sji.module_code);