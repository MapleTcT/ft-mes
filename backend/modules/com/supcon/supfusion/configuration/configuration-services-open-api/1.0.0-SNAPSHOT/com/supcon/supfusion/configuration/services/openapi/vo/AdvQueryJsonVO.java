package com.supcon.supfusion.configuration.services.openapi.vo;

import com.supcon.supfusion.configuration.services.entity.EcEnv;
import com.supcon.supfusion.configuration.services.entity.Event;
import com.supcon.supfusion.configuration.services.entity.Field;
import com.supcon.supfusion.configuration.services.entity.Model;
import com.supcon.supfusion.configuration.services.entity.View;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
@ApiModel(value = "AdvQueryJsonVO", description = "AdvQueryJsonVO")
public class AdvQueryJsonVO extends VO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String code;
    private int version;
    protected EcEnv ecEnv = EcEnv.product;
    private View view;
    private String queryConfig;
    private String name;
    private Model targetModel;// 当前advQueryJson的关联模型
    private String layoutName;
    private List<Field> fields;
    private List<Event> events;
    private Boolean projFlag;
    private Map queryConfigMap;

}
