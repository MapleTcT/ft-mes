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
@TableName(value = "counter_batch_param", autoResultMap = true)
@ApiModel(value = "申请批次字段参数表", description = "申请批次字段参数表")
public class CounterBatchParamPO extends BaseEntity {
    /**
     * 主键id
     */
    private Long id;
    /**
     * 批次ID
     */
    private Long batchId;
    /**
     *规则字段ID
     */
    private Long ruleFieldId;
    /**
     *规则字段值
     */
    private String ruleFieldValue;
}
