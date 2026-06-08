package com.supcon.supfusion.iam.api.dto;

import com.supcon.supfusion.framework.cloud.common.pojo.DTO;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotEmpty;

/**
 * @author tomcat
 * @date 20-6-16 下午8:34
 */
@Setter
@Getter
@ToString
@SuperBuilder
@NoArgsConstructor
public class AccountVerifyDTO extends DTO {
    private static final long serialVersionUID = -8139591502982593703L;

    /**
     * AK
     */
    @NotEmpty(message = "AK must not be empty")
    private String accessKey;
    /**
     * SK
     */
    @NotEmpty(message = "SK must not be empty")
    private String secretKey;
}
