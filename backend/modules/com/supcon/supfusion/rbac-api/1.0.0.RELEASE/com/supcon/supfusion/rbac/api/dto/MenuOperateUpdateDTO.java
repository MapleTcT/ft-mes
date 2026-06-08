package com.supcon.supfusion.rbac.api.dto;

import com.supcon.supfusion.framework.cloud.common.pojo.DTO;
import lombok.Data;

import java.util.List;

@Data
public class MenuOperateUpdateDTO extends DTO{

    private static final long serialVersionUID = 1162559466149393478L;

    private List<String> codes;

    private Boolean enableGrouprestrict;
}
