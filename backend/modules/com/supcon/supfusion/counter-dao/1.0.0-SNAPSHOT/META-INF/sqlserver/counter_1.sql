## 1.0.0
/*====================================================*/
/* Table: 规则表                                       */
/*====================================================*/
create table counter_rule  (
  id             BIGINT not null ,--comment '规则ID',
  rule_name      varchar(128) not null ,--comment '规则名称',
  valid          TINYINT not null default 0 ,--comment '是否失效',
  creator        varchar(32) not null default 'system' ,--comment '创建者',
  create_time    datetime not null default current_timestamp ,--comment '创建时间',
  create_staff_id BIGINT not null default 0 ,--comment '创建者id',
  modifier       varchar(32) ,--comment '修改人',
  modify_time    datetime ,--comment '修改时间',
  modify_staff_id BIGINT ,--comment '修改者id',
  constraint pk_counter_rule primary key (id)
);

/*====================================================*/
/* Table: 规则子表                                     */
/*====================================================*/
create table counter_rule_field  (
  id             BIGINT not null ,--comment '字段ID',
  rule_id        BIGINT not null ,--comment '规则ID',
  field_type     TINYINT not null ,--comment '字段类型：0=DATE=日期 1=AUTO=自增 2=CUSTOM=自定义 3=PROPERTY=实体字段 4=INHERENT=实体属性 5=SEPARATOR=分隔符',
  thecase        TINYINT not null default 0 ,--comment '大小写样式：0=ORIGINAL=保持原样 1=UPPER=大写 2=LOWER=小写',
  field_value    varchar(64) not null ,--comment '字段值：当fieldType=0时为systemdate',
  date_formatter varchar(32) not null default 'unknown' ,--comment '日期格式化：yy、yyyy、yyyyMM、yyyyMMdd',
  auto_length    TINYINT not null default 0 ,--comment '自增长度: > 1',
  auto_type      TINYINT not null default 0 ,--comment '自增类型：0=DATE=按日期 1=CODE=按编码',
  auto_date_rule TINYINT not null default 0 ,--comment '按日期自增规则：0=Daily=按天 1=Monthly=按月 2=Yearly=按年',
  field_order    int not null default 1 ,--comment '字段排序码：>=1',
  valid          TINYINT not null default 0 ,--comment '是否失效',
  creator        varchar(32) not null default 'system' ,--comment '创建者',
  create_time    datetime not null default current_timestamp ,--comment '创建时间',
  create_staff_id BIGINT not null default 0 ,--comment '创建者id',
  modifier       varchar(32) ,--comment '修改人',
  modify_time    datetime ,--comment '修改时间',
  modify_staff_id BIGINT ,--comment '修改者id',
  constraint pk_counter_rule_field primary key (id),
  index idx_counter_rule_field (rule_id,valid)
 );

/*====================================================*/
/* Table: 规则序号表                                   */
/*====================================================*/
create table counter_rule_sequence (
id             BIGINT not null ,--comment 'ID',
rule_id        BIGINT not null ,--comment '规则ID',
rule_field_id  BIGINT not null ,--comment '规则序号字段ID',
seq_reference  varchar(128) not null ,--comment '序号参照值：即序号是依照其滚动生成的',
seq_no         BIGINT not null ,--comment '当前序号值',
last_batch_id  BIGINT not null ,--comment '最后一次申请的批次ID',
creator        varchar(32) not null default 'system' ,--comment '创建者',
create_time    datetime not null default current_timestamp ,--comment '创建时间',
create_staff_id BIGINT not null default 0 ,--comment '创建者id',
modifier       varchar(32) ,--comment '修改人',
modify_time    datetime ,--comment '修改时间',
modify_staff_id BIGINT ,--comment '修改者id',
constraint pk_counter_rule_seq primary key (id),
constraint udx_counter_rule_seq unique(rule_id,seq_reference)
);

/*====================================================*/
/* Table: 申请批次表                                   */
/*====================================================*/
create table counter_batch (
id             BIGINT not null ,--comment '批次ID',
rule_id        BIGINT not null ,--comment '规则ID',
apply_count    int not null ,--comment '申请数量：>=1',
creator        varchar(32) not null default 'system' ,--comment '创建者',
create_time    datetime not null default current_timestamp ,--comment '创建时间',
create_staff_id BIGINT not null default 0 ,--comment '创建者id',
modifier       varchar(32) ,--comment '修改人',
modify_time    datetime ,--comment '修改时间',
modify_staff_id BIGINT ,--comment '修改者id',
constraint pk_counter_batch primary key (id),
index idx_counter_batch (rule_id)
);


/*====================================================*/
/* Table: 申请批次字段参数表                            */
/*====================================================*/
create table counter_batch_param (
id                 BIGINT not null ,--comment 'ID',
batch_id           BIGINT not null ,--comment '批次ID',
rule_field_id      BIGINT not null ,--comment '规则字段ID',
rule_field_value   varchar(128) not null ,--comment '规则字段值',
creator            varchar(32) not null default 'system' ,--comment '创建者',
create_time        datetime not null default current_timestamp ,--comment '创建时间',
create_staff_id    BIGINT not null default 0 ,--comment '创建者id',
modifier           varchar(32) ,--comment '修改人',
modify_time        datetime ,--comment '修改时间',
modify_staff_id    BIGINT ,--comment '修改者id',
constraint pk_counter_batch_param primary key (id),
index idx_counter_batch_param (batch_id)
);