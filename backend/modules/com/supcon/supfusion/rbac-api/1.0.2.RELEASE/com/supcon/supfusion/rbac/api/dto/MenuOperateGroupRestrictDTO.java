package com.supcon.supfusion.rbac.api.dto;

import com.supcon.supfusion.framework.cloud.common.pojo.DTO;
import lombok.Data;

@Data
public class MenuOperateGroupRestrictDTO extends DTO {

    private String entityCode;

    private Boolean enableGrouprestrict;
}
