package com.supcon.supfusion.auth.api.dto;

import lombok.Data;

/**
 * 用户目录认证
 *
 * @author caokele
 */
@Data
public class UserDirectoryAuthenticateDTO {
    private Long id;
    private String userName;
    private String password;
}
