package com.supcon.supfusion.auth.webapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Positive;
import java.util.List;


/**
 * @author lifangyuan
 */
@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel("修改用户模型")
public class UserUpdateVO extends VO {
    @Positive(message = "id 为正数")
    @ApiModelProperty(value = "用户id", example = "123", required = true)
    private Long id;

    @ApiModelProperty(value = "用户密码", example = "123abcd.", required = false)
    private String password;

    @ApiModelProperty(value = "用户名称", example = "sfsdfs", required = false)
    private String userName;

    @ApiModelProperty(value = "组织人员id", example = "123", required = false)
    private Long personId;

    @ApiModelProperty(value = "用户角色", required = false)
    private List<RoleVO> role;

    @ApiModelProperty(value = "时区", example = "CST+08:00", required = false)
    private String timeZone;

    @ApiModelProperty(value = "描述", example = "asdasd", required = false)
    private String description;

    @ApiModelProperty(value = "用户锁定状态", example = "true", required = false)
    private Boolean lock;

    @ApiModelProperty(value = "用户类型", example = "0", required = false)
    private Integer userType;
}
