## 1.0.0
/*====================================================*/
/* table: 门户信息表                                       */
/*====================================================*/
create table if not exists ec_portlet (
    code varchar(510) not null comment '编码主键',
    memo varchar(4000) comment '备注',
    height int comment '高度,iframeflag为true时有效',
    resize_func text comment 'resize事件',
    onload_func text comment 'onload事件',
    iframe_flag tinyint comment '是否适用iframe',
    menu_info_id bigint comment '关联菜单id',
    menu_operate_id bigint comment '关联操作id',
    menu_code varchar(510) comment '关联菜单code',
    operate_code varchar(510) comment '操作编码',
    cid bigint comment '公司id',
    module_code varchar(510) comment '模板id',
    is_hidden tinyint comment '是否隐藏',
    power_flag tinyint comment '是否启用权限',
    scope_num int comment '所属范围   0所有公司   1本公司',
    version int default 0,
    is_default int,
    title_color varchar(510),
    title_key varchar(510),
    title varchar(510),
    size_num int,
    more_target varchar(510),
    more_url varchar(510),
    url varchar(510),
    delete_time timestamp comment '删除时间',
    modify_time timestamp comment '修改时间',
    create_time timestamp comment '创建时间',
    terminator varchar(32) comment '删除者',
    modifier varchar(32) comment '修改者',
    creator varchar(32) comment '创建者',
    create_staff_id bigint comment '创建者人员id',
    modify_staff_id bigint comment '修改者人员id',
    primary key (code)
)
engine=innodb
default charset=utf8
comment='门户信息表';

CREATE TABLE IF NOT EXISTS `ec_my_portlet` (
  `id` bigint(20) NOT NULL,
  `version` int DEFAULT '0',
  `config` text,
  `user_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

## 1.0.1
insert into ec_portlet
(code, height, iframe_flag,power_flag, cid, module_code, title_key, title, url)
values('myProcess', 350, 1,0, 0, 'portal','portal.homepage.myProcess','portal.homepage.myProcess','/license/#/myProcess');

insert into ec_portlet
(code, height, iframe_flag,power_flag, cid, module_code, title_key, title, url)
values('pendingNotice', 110, 1,0, 0, 'portal','portal.homepage.pendingNotice','portal.homepage.pendingNotice','/license/#/pendingNotice');

## 1.0.2
update ec_portlet set url='/supplant/#/myProcess'  where code = 'myProcess';
update ec_portlet set url='/supplant/#/pendingNotice' where code = 'pendingNotice';

## 1.0.3
insert into ec_my_portlet
(id ,config ,user_id)
values(1,'[{"portlets":[{"cid":0,"code":"myProcess","default":false,"hidden":false,"iframeFlag":false,"menuInfoId":0,"powerFlag":false,"scopeNum":0,"title":"我的流程","titleKey":"portal.homepage.myProcess","url":"/supplant/#/myProcess?__t__=1616033155820"}],"width":"33%"},{"portlets":[{"cid":0,"code":"pendingNotice","default":false,"hidden":false,"iframeFlag":false,"menuInfoId":0,"powerFlag":false,"scopeNum":0,"title":"待办提醒","titleKey":"portal.homepage.pendingNotice","url":"/supplant/#/pendingNotice?__t__=1616033156792"}],"width":"33%"},{"portlets":[],"width":"33%"}]',-1);
