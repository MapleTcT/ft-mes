create table ${notice_task}
(
   id                                 number(20,0) not null,
   code                               varchar2(128) not null,
   bsmod_code                         varchar2(200) not null,
   bsmod_name                         varchar2(200) not null,
   task_type                          number(1,0) not null,
   status                             number(1,0) default 0,
   sharding_time                      number(20,0) not null,
   notice_topic_id                    number(20,0),
   creator                            varchar2(32) not null,
   create_time                        timestamp not null,
   create_staff_id                    number(20,0) NOT NULL,
   modify_staff_id                    number(20,0),
   modifier                           varchar2(32),
   modify_time                        timestamp,
   primary key (id)
);
create index ${notice_task} on ${notice_task}(sharding_time);
