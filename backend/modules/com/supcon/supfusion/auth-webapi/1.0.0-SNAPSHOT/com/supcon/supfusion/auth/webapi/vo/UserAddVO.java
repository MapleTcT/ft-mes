package com.supcon.supfusion.auth.webapi.vo;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.supcon.supfusion.framework.cloud.common.json.converters.IDJsonDeserializer;
import com.supcon.supfusion.framework.cloud.common.json.converters.IDJsonSerializer;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Positive;
import java.util.List;

/**
 * @author lifangyuan
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("添加用户模型")
public class UserAddVO extends VO {

    @NotBlank(message = "用户名称必填")
    @Pattern(regexp = "^\\w+$", message = "用户名称支持数字 字母 _")
    @ApiModelProperty(value = "用户名称", example = "adasdas", required = true)
    private String userName;

    @NotEmpty(message = "密码不能为空")
    @ApiModelProperty(value = "用户密码", example = "123abcd.", required = true)
    private String password;

    @Positive(message = "人员id必须正数")
    @ApiModelProperty(value = "组织人员id", example = "123", required = true)
    @JsonSerialize(using = IDJsonSerializer.class)
    @JsonDeserialize(using = IDJsonDeserializer.class)
    private Long personId;

    @ApiModelProperty(value = "用户角色", required = false)
    private List<RoleVO> role;

    @ApiModelProperty(value = "时区", example = "CST+08:00", required = false)
    private String timeZone;

    @ApiModelProperty(value = "描述", example = "asdasd", required = false)
    private String description;

    @Range(min = 0, max = 1, message = "用户类型0 或者1")
    @ApiModelProperty(value = "用户类型", example = "0", required = true)
    private Integer userType;

}
