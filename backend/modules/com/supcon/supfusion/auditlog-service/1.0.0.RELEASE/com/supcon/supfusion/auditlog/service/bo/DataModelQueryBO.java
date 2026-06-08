package com.supcon.supfusion.auditlog.service.bo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * 数据模型请求模型
 *
 * @author caokele
 */
@Getter
@Setter
@ToString
public class DataModelQueryBO {

    /**
     * 被操作对象编码
     */
    private String modelObjCode;

    /**
     * 被操作对象名称
     */
    private String modelObjName;

    /**
     * 操作类型
     */
    private List<String> operateTypes;
}
