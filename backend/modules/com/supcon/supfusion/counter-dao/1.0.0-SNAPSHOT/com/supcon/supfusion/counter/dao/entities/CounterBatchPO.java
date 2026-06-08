package com.supcon.supfusion.counter.dao.entities;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import io.swagger.annotations.ApiModel;
import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "counter_batch", autoResultMap = true)
@ApiModel(value = "申请批次表", description = "申请批次表")
public class CounterBatchPO extends BaseEntity {
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
