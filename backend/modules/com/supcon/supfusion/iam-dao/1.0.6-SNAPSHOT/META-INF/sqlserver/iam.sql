## 1.0.1
create table iam_account (
   id                   bigint not null,
   access_key           varchar(32) not null,
   secret_key           varchar(32) not null,
   username             varchar(64) not null,
   description          varchar(256),
   system               tinyint default 1,
   download_mark        tinyint default 1,
   creator              varchar(32) default 'system',
   create_time          datetime default getutcdate(),
   create_staff_id      bigint,
   modifier             varchar(32),
   modify_time          datetime default getutcdate(),
   modify_staff_id      bigint,
   primary key (id)
);

create unique index udx_account_username on iam_account(username);
create unique index udx_account_ak on iam_account(access_key);