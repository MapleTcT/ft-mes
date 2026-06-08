package com.supcon.supfusion.organization.common.constants;

import java.util.regex.Pattern;

/**
 * 常量类
 */
public class Constants {

    public static final String DEPARTMENT = "Department";
    public static final String POSITION = "Position";
    public static final String GROUP = "Group";
    public static final String COMPANY = "Company";
    public static final String PERSON = "Person";
    //------------------------部门错误消息------------------------
    public static final String DEPARTMENT_PARAM_CODE_NECESSARY = "部门编码为必填项，不可以为空！";
    public static final String DEPARTMENT_PARAM_NAME_NECESSARY = "部门名称为必填项，不可以为空！";
    public static final String DEPARTMENT_PARAM_TYPE_NECESSARY = "部门类型为必填项，不可以为空！";
    public static final String DEPARTMENT_PARAM_COMPANYID_NECESSARY = "部门的所属公司id为必填项，不可以为空！";
    public static final String DEPARTMENT_PARAM_ID_NECESSARY = "部门id为必填项，不可以为空";
    public static final String DEPARTMENT_PARAM_CODE_LENGTH_ERROR = "部门编码长度不可以超过50！";
    public static final String DEPARTMENT_PARAM_NAME_LENGTH_ERROR = "部门名称长度不可以超过200！";
    public static final String DEPARTMENT_PARAM_DESC_LENGTH_ERROR = "部门描述长度不可以超过500！";
    public static final String DEPARTMENT_PARENT_NAME_ONLY = "同上级部门下的子部门名称不可以重复";


    //------------------------岗位错误消息------------------------
    public static final String POSITION_PARAM_CODE_NECESSARY = "岗位编码为必填项，不可以为空！";
    public static final String POSITION_PARAM_CODE_LENGTH_ERROR = "岗位编码长度不可以超过50！";
    public static final String POSITION_PARAM_NAME_LENGTH_ERROR = "岗位名称长度不可以超过200！";
    public static final String POSITION_PARAM_DESC_LENGTH_ERROR = "岗位描述长度不可以超过500！";

    public static final String POSITION_PARAM_NAME_NECESSARY = "岗位名称为必填项，不可以为空！";

    public static final String POSITION_PARAM_DEPID_NECESSARY = "岗位关联部门id为必填项，不可以为空！";
    public static final String POSITION_PARAM_COMPANYID_NECESSARY = "岗位的所属公司id为必填项，不可以为空！";
    public static final String POSITION_PARAM_ID_NECESSARY = "岗位id为必填项，不可以为空";
    public static final String POSITION_PARAM_DEPARTMENTID_NECESSARY = "岗位所属部门id为必填项，不可以为空!";
    public static final String POSITION_ROLE_ID_PARAM_NECESSARY = "岗位关联角色,角色id为必填项, 不可以为空!";
    public static final String POSITION_PARENT_NAME_ONLY = "同上级岗位下的子岗位名称不可以重复";

    //------------------------Excel导入错误消息------------------------
    public static final String EXCEL_CODE_CELL_EMPTY = "编码为必填项,不可以为空!";
    public static final String EXCEL_TYPE_CELL_ERROR = "部门类型不正确!";
    public static final String EXCEL_PERSON_CODE_MAIN_POSITION_DIFF = "相同人员的主岗必须相同!";
    public static final String EXCEL_PERSON_CODE_REL_POSITION_DUP = "人员重复!";
    public static final String PERSON_REL_POSITION_NAME_NECESSARY_ERROR = "所属岗位名称必填!";

    public static final String PERSON_MAIN_REL_SAME_MUST_ERROR = "必须存在一条主岗和所属岗位相同!";


    //------------------------公司错误消息------------------------
    public static final String COM_PARAM_CODE_NOTNULL = "公司编码为必填项，不可以为空！";
    public static final String COM_PARAM_FULLNAME_NOTNULL = "公司全称为必填项，不可以为空！";
    public static final String COM_PARAM_SHORTNAME_NOTNULL = "公司简称为必填项，不可以为空！";
    public static final String COM_PARAM_ID_NOTNULL = "公司id为必填项，不可以为空！";
    public static final String COM_ADMIN_USERNAME_NECESSARY = "公司管理员名称为必填项,不可以为空!";
    public static final String COM_ADMIN_PASSWORD_NECESSARY = "公司管理员密码为必填项,不可以为空!";
    public static final String COMPANY_PARAM_CODE_LENGTH_ERROR = "公司编码长度不可以超过50！";
    public static final String COMPANY_PARAM_SHORTNAME_LENGTH_ERROR = "公司简称长度不可以超过200！";
    public static final String COMPANY_PARAM_FULLNAME_LENGTH_ERROR = "公司全称长度不可以超过200！";
    public static final String COMPANY_PARAM_DESC_LENGTH_ERROR = "公司描述长度不可以超过500！";

    //------------------------组错误消息------------------------
    public static final String GROUP_PARAM_CODE_NECESSARY = "组编码为必填项，不可以为空！";
    public static final String GROUP_PARAM_NAME_NECESSARY = "组名称为必填项，不可以为空！";
    public static final String GROUP_PARAM_TYPE_NECESSARY = "组类型为必填项，不可以为空！";
    public static final String GROUP_PARAM_COMPANYID_NECESSARY = "组的所属公司id为必填项，不可以为空！";
    public static final String GROUP_PARAM_CODE_LENGTH_ERROR = "组编码长度不可以超过50！";
    public static final String GROUP_PARAM_NAME_LENGTH_ERROR = "组名称长度不可以超过50！";
    public static final String GROUP_PARAM_DESC_LENGTH_ERROR = "组描述长度不可以超过500！";
    public static final String GROUP_PARAM_ID_NECESSARY = "组id为必填项，不可以为空";
    public static final String PERSON_NAME_LENGTH_ERROR = "人员姓名不可以为空,并且长度不得大于200!";
    public static final String PERSON_DESCRIPTION_LENGTH_ERROR = "描述长度不得大于500!";
    public static final String PERSON_REL_POSITION_CODE_NECESSARY_ERROR = "所属岗位名称重复,请填写岗位编码!";

    //------------------------人员错误消息------------------------
    public static final String PERSON_PARAM_CODE_NECESSARY = "人员编号为必填项,不可以为空";
    public static final String PERSON_PARAM_NAME_NECESSARY = "人员名称为必填项,不可以为空";
    public static final String PERSON_PARAM_GENDER_NECESSARY = "人员性别为必填项,不可以为空";
    public static final String PERSON_PARAM_USERNAME_NECESSARY = "用户名为必填项,不可以为空";
    public static final String PERSON_PARAM_PASSWORD_NECESSARY = "用户密码为必填项,不可以为空";
    public static final String PERSON_PARAM_MAIN_POSITION_NECESSARY = "人员的主岗为必填项,不可以为空";
    public static final String PERSON_PARAM_STATUS_NECESSARY = "人员的状态为必填项,不可以为空";
    public static final String PERSON_PARAM_ID_NECESSARY = "人员的id为必填项，不可以为空";

    //------------------------其他错误消息------------------------
    public static final String PAGE_CURRENT_ERROR = "分页页码不可以小于１!";
    public static final String PAGE_PAGESIZE_ERROR = "每页条数不可以小于１!";
    public static final String PAGE_PAGESIZE_MAX_ERROR = "每页条数不可以大于500!";
    public static final String COMPANY_TAG_NAME_NECESSARY = "标签名称为必填项,不可以为空!";
    public static final String PERSON_CODE_LENGTH_ERROR = "人员编号不可以为空,并且长度不得大于50!";

    public static final String PERSON_GENDER_ERROR = "人员性别不正确!";
    public static final String PERSON_STATUS_ERROR = "人员状态不正确!";
    public static final String PERSON_PHONE_ERROR = "手机号格式不正确,只能为数字!";
    public static final String PERSON_EMAIL_ERROR = "邮箱地址格式不正确!";
    public static final String PERSON_MAIN_POSITION_CODE_NECESSARY_ERROR = "主岗名称重复,请填写主岗编码!";
    public static final String PERSON_MAIN_POSITION_NAME_NECESSARY_ERROR = "主岗名称必填!";
    public static final String POSITION_THIS_NAME_NOT_EXISTS = "指定名称的岗位不存在!";
    public static final String POSITION_THIS_CODE_NOT_EXISTS = "指定编码的岗位不存在!";

    public static final String POSITION_NAME_DUP_CODE_NECESSARY_ERROR = "岗位名称重复,请填写岗位编码!";

    public static final String PERSON_MAIN_POSITION_NO_RELATION_POSITION = "人员主岗没有对应的所属岗位!";
    public static final String DEPARTMENT_NAME_DUP_CODE_NEED = "部门名称重复,请填写部门编码!";

    public static final String PERSON_TITLE_ERROR = "职称不正确!";
    public static final String PERSON_EDUCATION_ERROR = "学历不正确!";
    public static final String PERSON_CLASSIFIEDLEVEL_ERROR = "涉密等级不正确!";
    //-------------------------Excel-----------------
    public static final String PERSON_DATA_SHEETNAME = "人员信息";
    public static final String DEPARTMENT_DATA_SHEETNAME = "部门信息";
    public static final String POSITION_DATA_SHEETNAME = "岗位信息";
    public static final String EXCEL_IMPORT_SUCESS = "导入成功";

    public static final String EXCEL_DEPARTMENT_CODE_DUPLICATION = "表格中部门编码重复!";
    public static final String EXCEL_POSITION_CODE_DUPLICATION = "表格中岗位编码重复!";
    public static final String EXCEL_PERSON_CODE_DUPLICATION = "表格中人员编码重复!";
    public static final String EXCEL_DEPARTMENT_CODE_DUPLICATION_DB = "部门编码已存在!";
    public static final String EXCEL_POSITION_CODE_DUPLICATION_DB = "岗位编码已存在!";
    //----------------------old version-------------------
    public static final String CODE = "code";
    public static final String NAME = "name";
    public static final String SHOWNAME = "showName";
    public static final String GENDER = "gender";
    public static final String STATUS = "status";
    public static final String PHONE = "phone";
    public static final String EMAIL = "email";
    public static final String DESCRIPTION = "description";
    public static final String PARAM_NECESSARY = "必填参数不可以为空!";
    public static final String PARAM_GENDER_ERROR = "性别只能为1或者0!";
    public static final String PARAM_STATUS_ERROR = "状态只能为1或者0!";
    public static final String PARAM_SHOWNAME_LENGTH = "名称长度不可以超过200!";
    public static final String PARAM_CODE_LENGTH = "编码长度不可以超过50";
    public static final String COMPANY_NOT_EXISTS = "指定公司不存在";
    public static final String DEPT_NOT_EXISTS = "指定部门不存在";
    public static final String POSITION_NOT_EXISTS = "指定岗位不存在";
    public static final String PERSON_NOT_EXISTS = "指定人员不存在";

    //---------------------------枚举类型数据-------------------------
    public static final int FEMALE = 0; // 女性
    public static final int ON_WORK = 0; // 在职

    public static final long DEFAULT_COMPANY_ID = 1000;


    //----------------------old version-------------------

    public static final String ORG_TYPE = "org_type";
    public static final String ORG_NAME = "org_name";
    public static final String DEFAULT_COMPANY_NAME = "Company_default_org_company"; // 默认公司名
    public static final String DEFAULT_COMPANY_DEPARTMENT_CODE = "default_department"; // 默认公司部门编号
    public static final String DEFAULT_COMPANY_POSITION_CODE = "default_position"; // 默认公司岗位编号
    public static final String DEFAULT_COMPANY_DEPARTMENT_NAME = "default_department_1"; // 默认公司部门名
    public static final String DEFAULT_COMPANY_POSITION_NAME = "default_position_1"; // 默认公司岗位名
    public static final String DEFAULT_POSITION_SHOWNAME = "默认岗位"; // 默认岗位名
    public static final String DEFAULT_DEPARTMENT_SHOWNAME = "默认部门"; // 默认部门名
    public static final String PERSON_ID = "person_id";

    //---------------------------枚举类型数据-------------------------
    public static final String ORG_OLD_NAME = "Company_default_org_company";
    public static final String GENDER_FEMALE = "sys_gender/female"; // 女性
    public static final String GENDER_MALE = "sys_gender/male"; // 男性
    public static final String ON_WORK_CODE = "sys_person_status/onWork"; // 在职
    public static final String OFF_WORK_CODE = "sys_person_status/offWork"; // 离职
    public static final String DEPT_GENDERAL_CODE = "sys_department_type/general";
    public static final String DEPT_EMERGENCY_CODE = "sys_department_type/emergency";
    public static final String GENDER_ENTITYCODE = "sys_gender";
    public static final String PERSON_STATUS_ENTITYCODE = "sys_person_status";
    public static final String DEPARTMENT_TYPE_ENTITYCODE = "sys_department_type";
    public static final String CODE_REGEX = "^[0-9a-zA-Z_]{1,}$";
    public static final Pattern codePattern = Pattern.compile(CODE_REGEX);
    public static final String ORG_CODE_PATTERN = "编码错误，编码为字母、数字、下划线组合";

    public static final String PERSON_ID_NUMBER_ERROR = "身份证号由数字、大小写字母、特殊字符组成，长度不超过200字符!";
    public static final String PERSON_QUALIFICATION_LENHTH_ERROR = "资质长度不超过200字符!";
    public static final String PERSON_MAJOR_LENHTH_ERROR = "专业长度不超过200字符!";
    public static final String LINUX = "linux";

    public static final String WINDOWS = "windows";

    public static final String PERSON_TITLE_ENTITYCODE = "sys_person_title";
    public static final String PERSON_EDUCATION_ENTITYCODE = "sys_education";
    public static final String PERSON_CLASSIFIED_LEVEL_ENTITYCODE = "sys_person_classified_level";

    public static final String FILE_NO_TENANTID = "sys_default";

    public static final String ID_NUMBER_REGEX = "[^\\u4e00-\\u9fa5]+";

}
