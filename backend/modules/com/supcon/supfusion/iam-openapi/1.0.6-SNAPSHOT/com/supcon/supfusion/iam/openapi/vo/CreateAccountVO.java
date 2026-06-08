package com.supcon.supfusion.iam.openapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.validator.constraints.NotBlank;

/**
 * @author tomcat - <huangjianbo@supos.com>
 * @date 20-5-11 下午3:53
 */
@Setter
@Getter
@ToString
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAccountVO extends VO {
    private static final long serialVersionUID = 5308899560288436861L;

    @NotBlank(message = "user name must not be empty")
    private String username;

    private String description;
}
