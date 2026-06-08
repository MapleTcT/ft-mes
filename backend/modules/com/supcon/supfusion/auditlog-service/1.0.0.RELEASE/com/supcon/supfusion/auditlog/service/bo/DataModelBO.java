package com.supcon.supfusion.auditlog.service.bo;


import com.supcon.supfusion.systemcode.api.dto.SystemCodeResultDTO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;


/**
 * 数据模型
 * @author caokele
 */
@Getter
@Setter
@ToString
public class DataModelBO {

    /**
     * 链路跟踪ID
     */
    private String traceId;

    /**
     * 实体编码
     */
    private String entityCode;

    /**
     * 实体名称
     */
    private String entityName;

    /**
     * 模型编码
     */
    private String modelCode;

    /**
     * 模型对象编码
     */
    private String modelObjCode;

    /**
     * 模型对象名称
     */
    private String modelObjName;

    /**
     * 操作类型
     */
    private SystemCodeResultDTO operateType;

    /**
     * 操作时间
     */
    private String operateTime;

    /**
     * 模型属性列表
     */
    private List<DataModelPropertyBO> dataModelProperties;
}
