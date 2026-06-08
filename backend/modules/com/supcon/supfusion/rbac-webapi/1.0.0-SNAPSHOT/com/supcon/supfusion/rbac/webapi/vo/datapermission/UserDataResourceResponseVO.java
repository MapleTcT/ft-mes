package com.supcon.supfusion.rbac.webapi.vo.datapermission;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class UserDataResourceResponseVO extends VO {
    @ApiModelProperty(value = "数据权限")
    private List<DataResouceVO> dataResouceVOS;
    @ApiModelProperty(value = "是否受控")
    private boolean controlled;
}
