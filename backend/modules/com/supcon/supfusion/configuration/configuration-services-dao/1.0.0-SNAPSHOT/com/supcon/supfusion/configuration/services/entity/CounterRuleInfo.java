package com.supcon.supfusion.configuration.services.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.Entity;
import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@ApiModel(value = "规则对象")
public class CounterRuleInfo implements Serializable {

    private static final long serialVersionUID = 8600688336214275510L;

    /**
     * 规则id
     */
    private Long id;
    /**
     * 规则名称
     */
    private String ruleName;


    private List<CounterRuleField> ruleFields;


}
