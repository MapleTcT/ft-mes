package com.supcon.supfusion.configuration.services.entity;

import com.supcon.supfusion.counter.common.constants.AutoDateRuleType;
import com.supcon.supfusion.counter.common.constants.AutoType;
import com.supcon.supfusion.counter.common.constants.FieldType;
import com.supcon.supfusion.counter.common.constants.TheCase;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueCodeEntity;
import io.swagger.annotations.ApiModel;
import lombok.*;

import javax.persistence.*;
import javax.persistence.Entity;
import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * @Author kk.C
 * @Description: 编码生成器规则子表
 * @Date 2020/10/27 9:25
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = CounterRuleField.TABLE_NAME)
@ApiModel(value = "规则子表对象", description = "编码规则子表")
public class CounterRuleField implements Serializable {

    public static final String TABLE_NAME = "counter_rule_field";
    private static final long serialVersionUID = 7738573896171744121L;

    /**
     * 主键id
     */
    @Id
    private Long id;
    /**
     * 规则id
     */
    private Long ruleId;
    /**
     * 大小写样式：0=ORIGINAL=保持原样 1=UPPER=大写 2=LOWER=小写
     */
    @Enumerated(EnumType.ORDINAL)
    private TheCase thecase = TheCase.ORIGINAL;
    /**
     * 字段类型
     */
    @Enumerated(EnumType.ORDINAL)
    private FieldType fieldType = FieldType.DATE;

    /**
     * 值
     */
    private String fieldValue = "unknown";
    /**
     * 日期格式化
     */
    private String dateFormatter = "unknown";
    /**
     * 自增长度
     */
    private Integer autoLength = 0;
    /**
     * 自增类型
     */
    @Enumerated(EnumType.ORDINAL)
    private AutoType autoType = AutoType.CODE;
    /**
     * 按日期自增规则
     */
    @Enumerated(EnumType.ORDINAL)
    private AutoDateRuleType autoDateRule = AutoDateRuleType.DAILY;
    /**
     * 排序码
     */
    private Integer fieldOrder;
}
