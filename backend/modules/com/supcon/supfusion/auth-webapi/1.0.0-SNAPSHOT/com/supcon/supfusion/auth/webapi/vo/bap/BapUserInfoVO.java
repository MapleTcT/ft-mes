package com.supcon.supfusion.auth.webapi.vo.bap;


import lombok.Data;
import lombok.experimental.Accessors;

/**
 * bap用户信息
 *
 * @author caokele
 */
@Data
@Accessors(chain = true)
public class BapUserInfoVO {
    private Info staff;
    private Info company;
    private Info department;
    private Info user;
    private Info mainPosition;

    @Data
    public static class Info {
        private Long id;
        private String code;
        private String name;
    }
}
