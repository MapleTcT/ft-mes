package com.supcon.supfusion.organization.service.bo.kafka;


import com.supcon.supfusion.organization.service.bo.person.MainPositionBaseBO;
import com.supcon.supfusion.organization.service.bo.person.SystemCodeBO;
import com.supcon.supfusion.organization.service.bo.person.UserBO;
import lombok.*;

import java.util.List;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PersonDeleteMessageBO {

    private Long rowVersion;

    private Long id;

    private String code;

}
