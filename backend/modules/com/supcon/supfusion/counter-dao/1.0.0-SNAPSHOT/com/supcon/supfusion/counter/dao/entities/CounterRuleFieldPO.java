package com.supcon.supfusion.counter.dao.entities;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.counter.common.constants.AutoDateRuleType;
import com.supcon.supfusion.counter.common.constants.AutoType;
import com.supcon.supfusion.counter.common.constants.FieldType;
import com.supcon.supfusion.counter.common.constants.TheCase;
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
@TableName(value = "counter_rule_field", autoResultMap = true)
@ApiModel(value = "规则子表对象", description = "编码规则子表")
public class CounterRuleFieldPO extends BaseEntity {
    /**
     * 主键id
     */
    private Long id;
    /**
     * 规则id
     */
    private Long ruleId;
    /**
     *大小写样式：0=ORIGINAL=保持原样 1=UPPER=大写 2=LOWER=小写
     */
    @TableField(typeHandler = EnumOrdinalTypeHandler.class)
    private TheCase thecase;
    /**
     * 字段类型
     */
    @TableField(typeHandler = EnumOrdinalTypeHandler.class)
    private FieldType fieldType;

    /**
     * 值
     */
    private String fieldValue;
    /**
     * 日期格式化
     */
    private String dateFormatter;
    /**
     * 自增长度
     */
    private Integer autoLength;
    /**
     * 自增类型
     */
    @TableField(typeHandler = EnumOrdinalTypeHandler.class)
    private AutoType autoType;
    /**
     * 按日期自增规则
     */
    @TableField(typeHandler = EnumOrdinalTypeHandler.class)
    private AutoDateRuleType autoDateRule;
    /**
     * 排序码
     */
    private Integer fieldOrder;
    /**
     * 是否有效
     */
    @TableField(typeHandler = BooleanTypeHandler.class)
    private Boolean valid;
}
