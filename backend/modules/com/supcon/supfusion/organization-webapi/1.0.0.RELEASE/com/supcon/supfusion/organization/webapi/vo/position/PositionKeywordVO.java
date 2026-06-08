package com.supcon.supfusion.organization.webapi.vo.position;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.*;

/**
 * 关键词查询
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PositionKeywordVO extends VO {
    /**
     * 岗位id
     */
    private Long id;

    /**
     * 岗位名称
     */
    private String name;

    /**
     * 编码
     */
    private String code;

    /**
     * 关联的人员数量
     */
    private Long personNum = 0L;
}
