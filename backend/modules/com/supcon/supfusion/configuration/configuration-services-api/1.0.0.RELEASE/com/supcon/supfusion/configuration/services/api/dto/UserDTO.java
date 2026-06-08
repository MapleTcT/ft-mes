package com.supcon.supfusion.configuration.services.api.dto;


import com.supcon.supfusion.framework.cloud.common.pojo.DTO;
import lombok.*;

/**
 * @author tomcat - <huangjianbo@supos.com>
 * @date 20-4-15 上午10:09
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO extends DTO {
    private static final long serialVersionUID = 8603287202884805135L;

    private String name;
}
