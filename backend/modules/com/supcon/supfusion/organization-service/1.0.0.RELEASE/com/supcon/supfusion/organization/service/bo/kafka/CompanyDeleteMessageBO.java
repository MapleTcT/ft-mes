package com.supcon.supfusion.organization.service.bo.kafka;

import lombok.*;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CompanyDeleteMessageBO {

    private Long rowVersion;

    private Long id;

    private String code;
}
