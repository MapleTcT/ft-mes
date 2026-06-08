package com.supcon.supfusion.organization.webapi.vo.person;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.*;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PersonFlowSimpleVO extends VO {

    private Long id;

    private String code;
}
