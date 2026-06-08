package com.supcon.supfusion.rbac.api.dto;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.Data;

@Data
public class DataResouceDTO extends VO {
    private String resourceCode;
    private String resourceName;
    private String resourceType;
}
