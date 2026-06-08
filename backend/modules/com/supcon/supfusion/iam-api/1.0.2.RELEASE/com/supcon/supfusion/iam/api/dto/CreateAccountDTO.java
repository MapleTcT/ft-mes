package com.supcon.supfusion.iam.api.dto;

import com.supcon.supfusion.framework.cloud.common.pojo.DTO;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.validator.constraints.NotBlank;

/**
 * @author tomcat - <huangjianbo@supos.com>
 * @date 20-5-11 下午4:48
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CreateAccountDTO extends DTO {
    private static final long serialVersionUID = -2302381918390993992L;

    /**
     * 用户名
     */
    @NotBlank(message = "user name must not be empty")
    private String username;
    /**
     * 说明
     */
    private String description;
}
