package com.supcon.supfusion.organization.dao.po.person;

import lombok.*;

/**
 * 人员PO类
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PersonLoginPO {
    /**
     * 性别的编码值name
     */
    private String gender;

    /**
     * 人员头像url
     */
    private String avatarUrl;
}
