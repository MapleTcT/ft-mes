package com.supcon.supfusion.configuration.services.openapi.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.supcon.supfusion.configuration.services.entity.EcEnv;
import com.supcon.supfusion.configuration.services.entity.DataClassific;
import com.supcon.supfusion.configuration.services.entity.Model;
import com.supcon.supfusion.configuration.services.entity.View;
import com.supcon.supfusion.configuration.services.i18n.DispalyNameInternationalSerialzer;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;

@Data
@ApiModel(value = "DataGroupVO", description = "DataGroupVO")
public class DataGroupVO extends VO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String code;
    private int version;
    protected EcEnv ecEnv = EcEnv.product;
//    private Model targetModel;// 当前dataGroup的关联模型
    private View view;
    private String name;
    @JsonSerialize(using = DispalyNameInternationalSerialzer.class)
    private String displayName;
    private Boolean isMultiple = false;
//    private Set<DataClassific> dataClassifics = new LinkedHashSet<DataClassific>();
    private Long sort;
    private Boolean projFlag;
    private String layoutName;
    private String moduleCode;
    private String entityCode;

}
