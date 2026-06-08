package com.supcon.supfusion.rbac.common.Contants;


public class Constants {

    public static final String EXPORT_EXCEL_NAME = "role.xlsx";

    //组态期菜单
    public static final String ENVIRONMENT_CONFIGURE = "configure";

    //运行期菜单
    public static final String ENVIRONMENT_NORMAL = "normal";

    //兼有
    public static final String ENVIRONMENT_ALL = "all";

    //数据库类型常量字符串
    public static final String DB_TYPE_MARIADB = "mariadb";
    public static final String DB_TYPE_MYSQL = "mysql";
    public static final String DB_TYPE_ORACLE = "oracle";
    public static final String DB_TYPE_SQLSERVER = "sqlserver";

    public static final String MENU_CODE_IS_NOT_EMPTY = "菜单编码不能为空";

    //public static final String CURRENT_PAGE_MIN_1 = "分页页码不可以小于1";
    //public static final String CURRENT_PAGE_SIZE_MIN_1 = "每页记录数不可以小于1";

    public static final String PAGE_CURRENT_ERROR = "分页页码不可以小于１!";
    public static final String PAGE_PAGESIZE_ERROR = "分页条数不可以小于１!";
    public static final String PAGE_PAGESIZE_MAX_ERROR = "每页条数不可以大于500!";

    public static final String MENU_OPERATE_CODE_IS_NOT_EMPTY = "操作编码不能为空";

    //mysql  mariadb   *.?+$^[](){}|\/%&=-_;.,~`@!'" 都需要转义  在进入数据库查询之前处理 在前面添加 \
    public static final String I18N_VALUE_REGEX= "*.?+$^[](){}|\\/%&=-_;,~`@!'\"";
    //oracle 特殊字符 *.?+$^[](){}|%&=-;,~`@!'"/& 不用转义  \%_ 需要转义  在进入数据库查询之前处理
    public static final String I18N_VALUE_REGEX_ORACLE= "\\%_";
    //sqlserver 特殊字符    ^ ] 不需要转义  *.?+$[(){}|\/%&=-_;,~`@!" 需要转义 在进入数据库查询之前处理 加上[]  单引号需要单独处理 再加一个单引号
    public static final String I18N_VALUE_REGEX_SQL_SERVER= "*.?+$[(){}|\\/%&=-_;,~`@!\"";

    public static final String SQL_CHECK = "\\";
    public static final String SQL_SERVER_PREFIX = "[";
    public static final String SQL_SERVER_SUFFIX = "]";


    public static final String LAY_REC_SPLIT = "-";

    //创建公司默认管理员名称
    public static final String DEFAULT_ROLE_NAME = "公司管理员角色";
    public static final String DEFAULT_NORMAL_ROLE_NAME = "普通用户角色";

    //顶级菜单code
    public static final String MENU_LIST = "menu_list";

    /**
     * 助记码通配符
     */
    public static final String MNE_WILD_CARD = "\\*";
    /**
     * 数据库通配符
     */
    public static final String SQL_WILD_CARD = "\\%";
    public static final String SQL_ESCAPE_CHAR = "\\&";

    public static final String SQL_LIKE_CHAR = "%";

    public static final Long MENU_LIST_ID = -1L;

    public static final String START_PREFIX = "start";

}
