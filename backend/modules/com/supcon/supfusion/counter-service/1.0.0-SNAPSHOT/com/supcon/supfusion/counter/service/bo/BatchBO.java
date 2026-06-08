package com.supcon.supfusion.counter.service.bo;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class BatchBO {
    /**
     * 主键id
     */
    private Long id;
    /**
     * 规则id
     */
    private Long ruleId;
    /**
     * 申请数量：>=1
     */
    private Integer applyCount;
}
