package com.supcon.supfusion.systemconfig.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@TableName("systemconfig_config_version")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ConfigVersionPO extends BaseEntity {
    private Long id;
    private String configVersion;
    private String tidModuleKey;
}
