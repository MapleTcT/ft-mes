package com.supcon.supfusion.auth.webapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Size;
import java.util.List;

/**
 * @author lifangyuan
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("修改用户模型")
public class UserBatchDeletVO extends VO {

    @Size(min = 1, max = 100, message = "必须大于0 小于等于100")
    @ApiModelProperty(value = "用户id", example = "12", required = true)
    private List<Long> ids;
}
