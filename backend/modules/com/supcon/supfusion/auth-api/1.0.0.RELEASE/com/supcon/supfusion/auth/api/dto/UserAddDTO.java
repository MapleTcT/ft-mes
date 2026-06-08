package com.supcon.supfusion.auth.api.dto;

import com.supcon.supfusion.framework.cloud.common.pojo.DTO;
import lombok.*;
import org.hibernate.validator.constraints.Range;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
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
@Valid
public class UserAddDTO extends DTO {
    @NotBlank(message = "用户名称必填")
    @Pattern(regexp = "^\\w+$", message = "用户名称支持数字 字母 _")
    private String userName;
    @NotBlank(message = "密码必填")
    @Pattern(regexp = "^(?![A-Za-z]+$)(?![A-Z0-9]+$)(?![a-z0-9]+$)(?![a-z\\W]+$)(?![A-Z\\W]+$)(?![0-9\\W]+$)[a-zA-Z0-9\\W]{8,16}$", message = "8-16位大写字母、小写字母、特殊符号、数字中的任意三项")
    private String password;
    @Positive(message = "公司id为正数")
    private Long companyId;
    private String companyCode;
    private List<Long> roleIds;
    private Long personId;
    private String personCode;
    private String personName;
    private String description;
    @Range(min = 0, max = 1, message = "用户类型1 或者2")
    private Integer userType;
}
