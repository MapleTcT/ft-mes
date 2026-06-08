package com.supcon.supfusion.organization.service.bo.kafka;

import lombok.*;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CompanySimpleMessageBO {

    private Long id;

    private String code;

    private String fullName;

    private String shortName;
}
