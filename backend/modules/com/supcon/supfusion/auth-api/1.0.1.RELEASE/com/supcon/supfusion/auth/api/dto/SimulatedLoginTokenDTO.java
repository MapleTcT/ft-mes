package com.supcon.supfusion.auth.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模拟登录令牌
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimulatedLoginTokenDTO {
    /**
     * token类型
     */
    private String tokenType;
    /**
     * token
     */
    private String accessToken;
    /**
     * 过期时间(秒)
     */
    private Integer expiresIn;
}
