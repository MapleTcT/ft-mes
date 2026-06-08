package com.supcon.supfusion.configuration.services.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import javax.persistence.Entity;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
@Table(name = CounterRule.TABLE_NAME)
@ApiModel(value = "规则主表对象", description = "编码规则主表")
public class CounterRule implements Serializable {

    public static final String TABLE_NAME = "counter_rule";
    private static final long serialVersionUID = -3692937573307635815L;

    /**
     * 规则id
     */
    @Id
    private Long id;
    /**
     * 规则名称
     */
    private String ruleName;

//    @JsonIgnore
    @OneToMany(mappedBy = "ruleId", cascade = {CascadeType.REMOVE})
    @Fetch(FetchMode.SELECT)
    //    @org.hibernate.annotations.OrderBy(clause = "code asc")
    private List<CounterRuleField> ruleFields = new ArrayList<>();


}
