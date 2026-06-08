package com.supcon.supfusion.auditlog.webapi.vo;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.supcon.supfusion.framework.cloud.common.json.converters.IDJsonDeserializer;
import com.supcon.supfusion.framework.cloud.common.json.converters.IDJsonSerializer;
import com.supcon.supfusion.systemcode.api.dto.SystemCodeResultDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;


/**
 * @author caokele
 */
@Getter
@Setter
@ToString
@ApiModel("数据审计模型")
public class DataAuditLogModelResponseVO {
    @JsonSerialize(using = IDJsonSerializer.class)
    @JsonDeserialize(using = IDJsonDeserializer.class)
    @ApiModelProperty(value = "链路跟踪ID", example = "89552823343120385")
    private String traceId;

    @ApiModelProperty(value = "实体编码", example = "auditTest_1.0.0_car")
    private String entityCode;

    @ApiModelProperty(value = "实体名称", example = "auditTest.entityname.randon1605142148922")
    private String entityName;

    @ApiModelProperty(value = "模型编码", example = "auditTest_1.0.0_car_Car")
    private String modelCode;

    @ApiModelProperty(value = "模型对象编码", example = "Order001")
    private String modelObjCode;

    @ApiModelProperty(value = "模型对象名称", example = "订单001")
    private String modelObjName;

    @ApiModelProperty(value = "操作类型")
    private SystemCodeResultDTO operateType;

    @ApiModelProperty(value = "操作时间", example = "2020-08-03T21:02:02.000+0000")
    private String operateTime;

    @ApiModelProperty(value = "模型属性列表")
    private List<DataModelPropertyResponseVO> dataModelProperties;
}
