package com.supcon.supfusion.configuration.services.openapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(value = "ImportVO", description = "ImportVO")
public class ImportVO extends VO implements Serializable {
    public static final long serialVersionUID = 1L;

    private String code;
    private String name;
    private int version;
}
