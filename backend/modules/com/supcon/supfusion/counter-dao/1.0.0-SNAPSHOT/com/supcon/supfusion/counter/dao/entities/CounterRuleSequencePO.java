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
@TableName(value = "counter_rule_sequence", autoResultMap = true)
@ApiModel(value = "规则序号表", description = "规则序号表")
public class CounterRuleSequencePO extends BaseEntity {
    /**
     * 主键id
     */
    private Long id;
    /**
     * 规则id
     */
    private Long ruleId;
    /**
     *规则序号字段ID
     */
    private Long ruleFieldId;
    /**
     *'序号参照值：即序号是依照其滚动生成的
     */
    private String seqReference;
    /**
     *当前序号值
     */
    private Long seqNo;
    /**
     * 最后一次申请的批次ID
     */
    private Long lastBatchId;
}
