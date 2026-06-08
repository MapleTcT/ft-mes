package com.supcon.supfusion.organization.service.bo.person;

import lombok.*;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class SystemCodeBaseBO {

    /**
     * 系统编码
     */
    private String code;

    /**
     * 系统编码值
     */
    private String value;

    /**
     * 是否当前使用的
     */
    //private Boolean used =false;
}
