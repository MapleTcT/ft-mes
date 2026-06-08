package com.supcon.supfusion.framework.scaffold.auditlog.constant;

import java.util.HashSet;
import java.util.Set;

/**
 * 过滤字段类
 */
public class FilterField {
    /**
     * 模型对象属性展示过滤字段
     */
    public static Set filterFieldSet = new HashSet();

    /**
     * 异常描述过滤标签
     */
    public static Set exceptionFilterStr = new HashSet();

    static {
        filterFieldSet.add("createStaff");
        filterFieldSet.add("createTime");
        filterFieldSet.add("deleteStaff");
        filterFieldSet.add("deleteTime");
        filterFieldSet.add("effectStaff");
        filterFieldSet.add("effectTime");
        filterFieldSet.add("extraCol");
        filterFieldSet.add("fullPathName");
        filterFieldSet.add("id");
        filterFieldSet.add("layNo");
        filterFieldSet.add("layRec");
        filterFieldSet.add("modifyStaff");
        filterFieldSet.add("modifyTime");
        filterFieldSet.add("ownerDepartment");
        filterFieldSet.add("ownerPosition");
        filterFieldSet.add("ownerStaff");
        //filterFieldSet.add("valid");
        filterFieldSet.add("version");

        filterFieldSet.add("tableInfoId");
        filterFieldSet.add("groupId");
        filterFieldSet.add("positionLayRec");
        filterFieldSet.add("tableNo");
        filterFieldSet.add("effectiveState");

        exceptionFilterStr.add("<p>");
        exceptionFilterStr.add("</p>");
        exceptionFilterStr.add("<b>");
        exceptionFilterStr.add("</b>");
    }
}
