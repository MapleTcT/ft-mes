package com.supcon.supfusion.rbac.api.dto;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.Data;

import java.util.List;

@Data
public class UserDataResourceDTO extends VO {
    private List<DataResouceDTO> dataResouces;
    private boolean controlled;
}
