package com.supcon.supfusion.i18n.common.until;

/**
 * @Description: 常量
 * @Author: ShenZhiqiang
 * @Date: Create in  18:00 2020/6/16
 * @Modified:
 */
public class Constants {
    //默认分页 常量字符串
    public static final String PAGE_NO = "current";
    public static final String PAGENO = "pageNO";
    public static final String PAGE_SIZE = "pageSize";
    public static final String OFFSET = "offset";
    public static final String LIMIT = "limit";
    public static final String CUSTOM = "custom";
    // 默认租户ID
    public static final String DEFAULT_TENANT = "dt";
    
    //同sdk 交互常量字符串
    public static final String USED = "used";
    public static final String TRUE = "true";
    public static final String FALSE = "false";
    public static final String TOKEN = "token";
    public static final String HAS_TOKEN = "hasToken";
    public static final String HAS_TOKEN_MESSAGE = "message";
    public static final String STOP = "stop";
    public static final String BREAK = "break";
    public static final String BREAK_NO = "continue";
    public static final String BREAK_ERROR = "error";
    public static final String I18N_STR = "i18n";
    public static final String I18N_KEY = "i18n_key";
    public static final String I18NKEY = "i18nKey";
    public static final String I18N_KEYS = "i18nKeys";
    public static final String I18N_VALUE = "i18n_value";
    public static final String I18N_VALUES = "i18n_values";
    public static final String I18NVALUES = "i18nValues";
    public static final String I18NINDEXMAP = "i18nIndexMap";
    public static final String LANGU_CODE = "langu_code";
    public static final String LANGUCODE = "languCode";
    public static final String LANGU_TYPE = "langu_type";
    public static final String LANGU_NAME = "langu_name";
    public static final String MODULE_CODE = "moduleCode";
    public static final String MODULE_CODES = "moduleCodes";
    public static final String MODULE_INDEX_CODE = "moduleIndex";
    public static final String MODULE_VERSION_CODE = "moduleVersion";
    public static final String MODULE_VERSION_CODES = "moduleVersions";
    public static final String I18N_KEYS_LIST_STR = "i18nKeyListStr";
    public static final String TENANT_ID = "tenantId";

    //数据库类型常量字符串
    public static final String DB_TYPE_MARIADB = "mariadb";
    public static final String DB_TYPE_MYSQL = "mysql";
    public static final String DB_TYPE_ORACLE = "oracle";
    public static final String DB_TYPE_SQLSERVER = "sqlserver";


    //相对路径
    public static final String USER_DIR = "user.dir";
    public static final String EXCEL_FILE_PATH = "i18n/other/excel/";//导入导出文件目录
    public static final String RESOURCE_FILE_PATH = "i18n/messages/";//初始化国际化键值对文件目录
    public static final String RESOURCE_FILE_CUSTOM_PATH = "i18n/custom/messages/";//非初始化国际化键值对文件目录
    public static final String RESOURCE_FILE_INDEX_PATH = "i18n/index/";//索引文件目录
    public static final String ZIP_FILE_IMPORT_PATH = "i18n/other/zip/importZip/";//接收压缩包的临时文件目录
    public static final String ZIP_FILE_EXPORT_PATH = "i18n/other/zip/exportZip/";//发送压缩包的临时文件目录
    public static final String EXCEL_FILE_IMPORT_UPDATE_PATH = "i18n/other/updateFile/";//数据库 发生变化的国际化key 的文件目录
    public static final String PROPERTIES_FILE_IMPORT_UPDATE_PATH = "i18n/other/updateOneFile/";//数据库 发生变化的国际化key 的文件目录
    public static final String EXCEL_FILE_EXPORT_PATH = "i18n/other/excel/export/";//导出 临时文件目录
    public static final String EXCEL_FILE_IMPORT_PATH = "i18n/other/excel/import/";//导入 临时文件目录
    public static final String EXCEL_ERROR_FILE_PATH = "i18n/other/excel/error/";//错误文件目录
    public static final String EXCEL_FILE_TEMPLATE_PATH = "i18n/other/excel/template/";//模版文件目录
    public static final String MAVEN_RESOURCE_PATH = "BOOT-INF/classes/";//windows 打包资源文件路径
    public static final String PATH = "/";
    public static final String SQL_CHECK = "\\";
    public static final String SQL_SERVER_PREFIX = "[";
    public static final String SQL_SERVER_SUFFIX = "]";


    //空格字符串和空字符串
    public static final String STR_SPACE = " ";
    public static final String STR_NO_SPACE = "";
    public static final String STR_POINT = ".";
    public static final String STR_POINT1 = "\\.";
    public static final String STR_LINE = "_";
    public static final String STR_POINT_DOU = ",";
    public static final String STR_POINT_SHU = "|";
    public static final String STR_POINT_SHU1 = "\\|";
    public static final String STR_POINT_M = ":";
    public static final String JAR_FILE_SUFFIX = "!/";
    public static final String JAR_FILE_PREFIX = "jar:";
    public static final String FILE_PREFIX = "file:";
    //文件后缀格式常量
    public static final String PROPERTIES = "PROPERTIES";
    public static final String PROPERTIES_LOW= "properties";
    public static final String ZIP = "ZIP";
    public static final String XLSX = "XLSX";
    public static final String ZIP_LOW = "zip";
    public static final String XLSX_LOW = "xlsx";
    public static final String TXT_LOW = "txt";
    //数字 boolean 常量
    public static final Integer JAR_FILE_SUFFIX_LENGTH = 2;
    public static final Integer PAGE_SIZE_NUM_DEFA = 20;
    public static final String ZERO_STR = "0";
    public static final Integer ZERO_INT = 0;
    public static final String ONE_STR = "1";
    public static final String TWO_STR = "2";
    public static final Integer ONE_INT = 1;
    //excel 导入导出 字符串常量
    public static final String COUNT = "count";
    public static final String ADD_NUM = "addNum";
    public static final String SHEET_AT =  "sheetAt";
    public static final String I18N_POS =  "i18n_POs";
    public static final String I18N_MODULE_SET =  "i18nModuleSet";
    public static final String I18N_KEY_SET =  "i18nKeySet";
    public static final String STR_RANDOM = "random";
    public static final String STR_IMPORT = "import";
    public static final String STR_EXPORT = "export";
    public static final String STR_ALL_NUM = "allNum";
    public static final String UPDATE_NUM = "updateNum";
    public static final String STR_ERROR_NUM = "errorNum";
    public static final String FILE_NAME_STR = "filename";
    public static final String INDEX_FILE_NAME = "index";
    public static final String DOWN_ALL_STR =  "downAll";
    public static final String COUNT_ERROR_MAP =  "count_errorMap";
    public static final String I18N_KEY_ERROR_MAP =  "i18n_key_errorMap";
    public static final String I18N_VALUE_ERROR_MAP =  "i18n_value_errorMap";
    public static final String RESOURCE_LIST = "i18n_resource_list";

    //正则
    public static final String I18N_KEY_REGEX= "^[a-zA-Z\\d_.]+$"; //英文字母 数字 点 下划线 正则表达式
    //mysql  mariadb   *.?+$^[](){}|\/%&=-_;.,~`@!'" 都需要转义  在进入数据库查询之前处理 在前面添加 \
    public static final String I18N_VALUE_REGEX= "*.?+$^[](){}|\\/%&=-_;,~`@!'\"";
    //oracle 特殊字符 *.?+$^[](){}|&=-;,~`@!'"/& 不用转义  \%_ 需要转义  在进入数据库查询之前处理
    public static final String I18N_VALUE_REGEX_ORACLE= "\\%_";
    //sqlserver 特殊字符    ^ ] 不需要转义  *.?+$[(){}|\/%&=-_;,~`@!" 需要转义 在进入数据库查询之前处理 加上[]  单引号需要单独处理 再加一个单引号
    public static final String I18N_VALUE_REGEX_SQL_SERVER= "*.?+$[(){}|\\/%&=-_;,~`@!\"";

    //运行环境 常量字符串
    public static final String DEV_ENVIRO = "productDev";
    public static final String PRO_ENVIRO = "product";
    public static final String I18N_LANGUAGE_ZH_CN = "zh_CN";
    public static final String I18N_LANGUAGE_ZH_HK = "zh_HK";
    public static final String I18N_LANGUAGE_EN_US = "en_US";
    //excel模版 常量字符串
    public static final String FILENAME = "国际化模板";
    public static final String I18N_KEY_ZHCN = "国际化主键";
    public static final String FILE_TABLE_NAME = "国际化数据";
    //提示信息 需要整理成自己模块的 后端异常国际化资源
    public static final String CREATE_ERROR = "创建失败";
    public static final String DELETE_ERROR = "删除失败";
    public static final String PARAM_ERROR = "参数错误";
    public static final String PARAM_LOST = "参数不完整";
    public static final String PARAM_SUCCESS = "SUCCESS!";
    public static final String SERVER_ERROR = "服务器处理异常";
    public static final String MODULE_CODE_ERROR = "模块名错误";
    public static final String MODULE_CODE_EXIST_ERROR = "模块在模块注册服务仍然存在";
    public static final String FIND_NO_VALUE = "未查询到相应的国际化值";
    public static final String FIND_NO_LANGUAGE = "未查询到语言";
    public static final String FILE_UPLOADING_ERROR = "当前国际化正在进行资源上传操作，请稍后再试";
    public static final String PAGE_PARAM_ERROR = "分页参数错误";
    public static final String ZIP_CREATE_FAIL = "create zip fail! ";
    public static final String EXCEL_IMPORTING = "后台正在解析excel!";
    public static final String FILE_TRANSPORT_ERROR = "文件传输失败";
    public static final String TOKEN_ERROR = "token过期，请重新获取！";
    public static final String FILE_RESOLVER_ERROR = "文件解析失败";
    public static final String RESOURCE_UNZIP_ERROR = "文件解压失败";
    public static final String RESOURCE_FILE_COPY_ERROR = "文件复制失败";
    public static final String EXCEL_IMPORTING_ERROR = "解析过程发生异常，使用模版文件重试";
    public static final String FILE_RESOLVER_SHEET_ERROR = "解析更新失败";
    public static final String EXCEL_FILE_ERROR = "上传文件格式不符合要求";
    public static final String VERSION_TOKEN_ERROR = "版本号和token不对应";
    public static final String NO_TOKEN_CODE = "没有token或token为空字符串";
    public static final String RESOURCE_EXIST = "已经存在该版本的国际化资源！";
    public static final String RESOURCE_VERSION_ERROR = "版本号错误";
    public static final String UPLOAD_FILE_NO_ZIP_ERROR = "上传文件不是zip格式";
    public static final String VERSION_LOW_DB_VERSION = "当前版本低于服务端版本！";
    public static final String NO_MODULE_CODE = "没有模块编号或模块编号为空字符串";
    public static final String NO_VERSION_CODE = "没有模块国际化资源版本号或版本号为空字符串";
    public static final String UPLOAD_FILE_NO_PROPERTIES_ERROR = "上传文件不是properties格式";
    public static final String EXCEL_IMPORTING_OOM_ERROR = "文件数据量过大，内存溢出";
    public static final String XLSX_UPLOAD_MAX_SIZE_ERROR = "上传文件大小超过限制";
    public static final String XLSX_UPLOAD_MAX_NUM_ERROR = "上传文件超过最大人数限制，稍后再试";
    public static final String FILE_DOWNLOAD_ERROR = "文件下载失败，请重试！";
    public static final String FILE_EXPORT_ERROR = "导出数据写入文件失败";
    public static final String MODULE_VERSION_RESOLVE_ERROR = "版本号解析异常";
    public static final String FILE_ZIP_CREATE_ERROR = "压缩包资源不存在";
    public static final String DATA_INPUT_FILE_ERROR = "数据写入文件失败";
    public static final String MODE_EXPORT_ERROR = "国际化模板导出异常";
    public static final String MODULE_INDEX_ERROR = "索引不存在";
    public static final String FILE_COPY_ERROR = "文件复制过程出现错误";
    public static final String I18N_VALUE_LENGTH_ERROR = "国际化value超过长度限制";
    public static final String I18N_VALUE_BLANK_ERROR = "国际化value不能全部为空";
    public static final String I18N_KEY_LENGTH_ERROR = "国际化key超过长度限制";
    public static final String I18N_KEY_START_ERROR = "国际化key没有以模块名开头";
    public static final String FILE_EXPORT_DB_ERROR = "数据导出过程中查询数据库异常";
    public static final String LANGUAGE_HAS_USED_ERROR = "至少启用一种语言";
    public static final String LANGUAGE_HAS_NO_ERROR = "数据库不存在当前语言类型";
    public static final String FILE_UPLOAD_NO_FILE_ERROR = "未接受到资源文件";
    public static final String FILE_ERROR_MESSAGE = "记录文件已经删除，请重新导入后重试";
    public static final String XLSX_UPLOAD_KEY_BLANK_ERROR = "国际化key不允许存在空值";
    public static final String XLSX_UPLOAD_KEY_REPEAT_ERROR = "国际化key不允许出现重复";
    public static final String RESOURCE_IS_UPLOADING = "正在处理当前模块资源，请稍后再试";
    public static final String RESOURCE_NOT_FIND_ERROR = "未查询到解析状态，请检查文件名重试";
    public static final String FILE_SHEET_NONE_ERROR = "导入失败,文件数据为空";
    public static final String I18N_KEY_EXIST = "该国际化主键已存在，请重新输入";
    public static final String I18N_KEY_ERROR = "国际化主键只能由英文字母、数字、下划线和点组成";
    public static final String PARAM_LOST_I18N_KEY = "参数不完整:未接受到国际化key";
    public static final String PARAM_LOST_I18N_VALUE = "参数不完整:未接受到国际化value";
    public static final String PARAM_LOST_I18N_LANGUAGE = "参数不完整:未接受到语言和国际化值";
    public static final String FILE_IS_TOKEN_ERROR = "其他模块已获得令牌，正在更新国际化资源";
    public static final String FILE_HEAD_RESOLVER_ERROR = "文件表头解析失败，请更换模版文件重试";
    public static final String FILE_NO_TOKEN_ERROR = "没有token权限上传国际化资源,请获取token重试";
    public static final String FILE_RESOLVER_SAVE_FILE_ERROR = "解析文件，入库成功，同步文件失败";
    public static final String MODULE_NUM_NOT_VERSION_NUM = "模块名和版本号不对应";
    public static final String NO_MORE_THAN_ERROR = "num不能超过10000";
    public static final String NO_LESS_THAN_ERROR = "num不能小于0";
    public static final String I18N_CLUSTER_LOCK = "i18n_init_lock";

    private Constants() {
        throw new IllegalStateException("Constants class");
    }
}
