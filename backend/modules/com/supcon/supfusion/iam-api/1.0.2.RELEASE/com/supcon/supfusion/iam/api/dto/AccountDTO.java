package com.supcon.supfusion.iam.api.dto;

import com.supcon.supfusion.framework.cloud.common.pojo.DTO;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * @author tomcat - <huangjianbo@supos.com>
 * @date 20-5-11 下午4:51
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AccountDTO extends DTO {
    private static final long serialVersionUID = 5084001264712683877L;

    /**
     * AK
     */
    private String ak;
    /**
     * SK
     */
    private String sk;
}
