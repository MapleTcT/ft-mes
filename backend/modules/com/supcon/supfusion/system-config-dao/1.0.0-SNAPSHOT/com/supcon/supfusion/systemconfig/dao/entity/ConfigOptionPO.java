package com.supcon.supfusion.systemconfig.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import lombok.*;

/**
 * @author lifangyuan
 */
@EqualsAndHashCode(callSuper = true)
@TableName("systemconfig_config_option")
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ConfigOptionPO extends BaseEntity {
    private Long id;
    private Long configId;
    private Double sort;
    private String label;
    private String selectValue;
}
