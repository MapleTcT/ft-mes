package com.supcon.supfusion.framework.scaffold.auditlog.pojo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 模型对象信息
 * @author caokele
 */
@Getter
@Setter
@ToString(callSuper = true)
public class ModelObjectInfo {
    /**
     * 模型编码
     */
    private String modelCode;
    /**
     * 模型名称（国际化Key）
     */
    private String modelName;
    /**
     * 实体编码
     */
    private String entityCode;
    /**
     * 实体名称（国际化Key）
     */
    private String entityName;
    /**
     * 模型对象主键
     */
    private String modelObjPk;
    /**
     * 模型对象编码
     */
    private String modelObjCode;
    /**
     * 模型对象名称
     */
    private String modelObjName;
}
