package com.supcon.supfusion.rbac.webapi.vo.datapermission;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class RoleDataResourceResponseVO extends VO {
    @ApiModelProperty(value = "数据权限")
    private List<DataResouceVO> dataResouceVOS;
    @ApiModelProperty(value = "是否受控")
    private boolean controlled;
}
