package com.supcon.supfusion.counter.dao.entities;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import com.supcon.supfusion.framework.scaffold.mybatis.type.handler.BooleanToIntTypeHandler;
import io.swagger.annotations.ApiModel;
import lombok.*;
import org.apache.ibatis.type.BooleanTypeHandler;
import org.apache.ibatis.type.EnumOrdinalTypeHandler;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "counter_rule", autoResultMap = true)
@ApiModel(value = "规则主表对象", description = "编码规则主表")
public class CounterRulePO extends BaseEntity {
    /**
     * 规则id
     */
    private Long id;
    /**
     * 规则名称
     */
    private String ruleName;
    /**
     * 是否有效
     */
    @TableField(typeHandler = BooleanTypeHandler.class)
    private Boolean valid;
}
