package com.supcon.supfusion.organization.service.bo.company;

import lombok.*;

/**
 * 公司标签
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CompanyTagBO {

    private Long id;

    private String type;

    private String name;
}
