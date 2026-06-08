package com.supcon.supfusion.rbac.api.dto;

import com.supcon.supfusion.framework.cloud.common.pojo.DTO;
import lombok.Data;

@Data
public class UpdateMenuInfoIdDTO extends DTO {

    private Long oldId;

    private Long newId;
}
