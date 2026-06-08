package com.supcon.supfusion.organization.service.bo.position;

import lombok.*;

/**
 * 模糊匹配搜索列表
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PositionKeywordBO {
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
