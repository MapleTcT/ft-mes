package com.supcon.supfusion.organization.service.bo.kafka;

import com.supcon.supfusion.organization.service.bo.person.SystemCodeBO;
import lombok.*;

import java.util.List;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentSimpleMessageBO {

    private Long id;

    private String code;

    private String name;

}
