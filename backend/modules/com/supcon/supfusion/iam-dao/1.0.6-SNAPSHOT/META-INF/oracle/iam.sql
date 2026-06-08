## 1.0.0
create table iam_account (
   id                   number(20,0) not null,
   access_key           varchar2(32) not null,
   secret_key           varchar2(32) not null,
   username             varchar2(64) not null,
   description          varchar2(256),
   system               number(1,0) default 1,
   download_mark        number(1,0) default 1,
   creator              varchar2(32) default 'system',
   create_time          timestamp default systimestamp,
   create_staff_id      number(20,0),
   modifier             varchar2(32),
   modify_time          timestamp default systimestamp,
   modify_staff_id      number(20,0),
   primary key (id)
);

create unique index udx_account_username on iam_account(username);
create unique index udx_account_ak on iam_account(access_key);