package com.supcon.supfusion.configuration.services.openapi.vo;

import com.supcon.supfusion.configuration.services.entity.EcEnv;
import com.supcon.supfusion.configuration.services.entity.Echarts;
import com.supcon.supfusion.configuration.services.entity.View;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
@ApiModel(value = "ExtraViewVO", description = "ExtraViewVO")
public class ExtraViewVO extends VO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String code;
    private int version;
    protected EcEnv ecEnv = EcEnv.product;
    private View view;
    private String config;
    private String fullConfig;
    private Boolean projFlag;
    private String viewJson;
    private Map configMap;
    private List<Echarts> echartsList;

}
