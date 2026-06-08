package com.supcon.supfusion.organization.webapi.vo.department;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.*;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentFlowSimpleVO extends VO {

    private Long id;

    private String code;
}
