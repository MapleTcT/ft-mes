package com.supcon.supfusion.organization.webapi.vo.position;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.*;

import java.util.List;

/**
 * 岗位的角色关联
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PositionRoleAddVO extends VO {

    private Long positionId;

    private List<Long> roleIds;
}
