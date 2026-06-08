## 1.0.0
create table scheduler_job_info
(
   id                   number(20,0) not null,
   model_name           varchar(64)  not null,
   job_name             varchar(256)  not null ,
   job_key              varchar(64)  not null ,
   job_desc             varchar(510) ,
   job_cron             varchar(64) not null ,
   job_status           number default 0 ,
   job_service_api      varchar(510) not null ,
   job_service_params   varchar(510)  ,
   job_call_no          number(20,0) ,
   last_time            timestamp,
   next_time            timestamp,
   user_name            varchar(64) not null,
   creator              varchar(32) default null,
   modifier             varchar(32) default null,
   terminator           varchar(32) default null,
   create_time          timestamp,
   modify_time          timestamp,
   delete_time          timestamp,
   create_staff_id number(20, 0) ,
   modify_staff_id number(20, 0) ,
   primary key(id)
);
create index index_jobinfo_code on scheduler_job_info(code);

create table scheduler_job_log_info
(
   id                   number(20,0) not null ,
   model_name           varchar(64) not null,
   job_name             varchar(256) not null,
   job_service_api      varchar(510) not null,
   job_service_params   varchar(510) ,
   job_message          varchar(510) ,
   job_status           number,
   exception_info       varchar(510) ,
   user_name            varchar(64) ,
   creator              varchar(32) default null,
   modifier             varchar(32) default null,
   terminator           varchar(32) default null,
   create_time          timestamp,
   modify_time          timestamp,
   delete_time          timestamp,
   create_staff_id number(20, 0) ,
   modify_staff_id number(20, 0) ,
   primary key(id)
);
create index index_jobloginfo_code on scheduler_job_log_info(code);
create index index_jobloginfo_model_name on scheduler_job_log_info(model_name);
create index index_jobloginfo_job_status on scheduler_job_log_info(job_status);

alter table scheduler_job_log_info add code varchar(64);
alter table scheduler_job_info add code varchar(64);
alter table scheduler_job_info modify job_name varchar2(256);
alter table scheduler_job_info modify code varchar2(256);
alter table scheduler_job_info modify job_desc varchar2(510);
alter table scheduler_job_info modify job_service_api varchar2(510);
alter table scheduler_job_info modify job_service_params varchar2(510);
alter table scheduler_job_log_info modify job_name varchar2(256);
alter table scheduler_job_log_info modify code varchar2(256);
alter table scheduler_job_log_info modify job_service_api varchar2(510);
alter table scheduler_job_log_info modify job_service_params varchar2(510);
alter table scheduler_job_log_info modify job_message varchar2(510);
alter table scheduler_job_log_info modify exception_info varchar2(510);
## 1.0.1
alter table scheduler_job_info modify job_cron varchar(510);
## 1.0.2
alter table scheduler_job_info add module_code varchar(256);
## 1.0.3
alter table scheduler_job_info modify job_cron varchar(550);
## 1.1.0
update scheduler_job_info sji set sji.module_code = (select artifact from ec_module em where em.code = sji.module_code) where exists(select artifact from ec_module em where em.code = sji.module_code);