package com.supcon.supfusion.organization.service.bo.kafka;

import lombok.*;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentDeleteMessageBO {

    private Long rowVersion;

    private Long id;

    private String code;
}
