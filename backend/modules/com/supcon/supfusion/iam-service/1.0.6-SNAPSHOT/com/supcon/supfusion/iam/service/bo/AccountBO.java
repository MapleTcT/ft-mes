package com.supcon.supfusion.iam.service.bo;

import com.supcon.supfusion.framework.cloud.common.pojo.BO;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * @author tomcat - <huangjianbo@supos.com>
 * @date 20-5-11 下午3:56
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AccountBO extends BO {
    private static final long serialVersionUID = -6889328266504087728L;

    private Long id;

    private String username;

    private String description;

    private String accessKey;

    private String secretKey;

    private Integer system = 1;
}
