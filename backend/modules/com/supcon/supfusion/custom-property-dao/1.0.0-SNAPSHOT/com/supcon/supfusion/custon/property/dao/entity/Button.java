/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD.
 * All rights reserved.
 */
package com.supcon.supfusion.custon.property.dao.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.supcon.supfusion.custon.property.common.enums.EcEnv;
import com.supcon.supfusion.custon.property.common.enums.OperateType;
import com.supcon.supfusion.custon.property.common.enums.RegionType;
import com.supcon.supfusion.custon.property.dao.entity.base.AbstractEntity;
import com.supcon.supfusion.custon.property.dao.entity.base.LogicBasePO;
import com.supcon.supfusion.custon.property.dao.utils.SerializeUitls;
import com.supcon.supfusion.framework.scaffold.mybatis.type.handler.UTCToStringTypeHandler;
import lombok.Data;
import org.apache.ibatis.type.JdbcType;

import java.beans.Transient;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 *
 * @author zhuyuyin
 * @version $Id$
 */

@Data
@TableName(value = "runtime_button", autoResultMap = true)
public class Button extends AbstractEntity {
    private static final long serialVersionUID = -415440617645100336L;

    @TableId
    private String code;

    private EcEnv ecEnv = EcEnv.product;

    private String name;

    private Boolean isConfirm = false;

    private String confirmContent;

    private String buttonStyle;

    private String buttonOperationCode;

    private Boolean isUseMore = false;

    private Boolean isPermission = false;

    private Boolean isCallback = false;

    private Boolean isCustomFunc = false;

    private Boolean isHide = false;

    private String operateUrl;

    private String displayName;

    private String cellCode;

    private String signatureDescrible;

    private String scriptCode;// 按钮对应的脚本

    private String config;

    private String permissionCode;

    private String buttonAlign;

    private Boolean isPublished;

    private String signerId;

    private String positionId;

    private String roleId;

    private String powerType;

    private Boolean signatureEnabled = false;

    private String signatureType;

    private String releaseFelid;

    private Boolean isSignatureConfig;
    private String moduleCode;

    private String entityCode;

    private Boolean projFlag;

    private RegionType regionType;

    private OperateType operateType;
    private String viewCode;
    @TableField("viewselect_code")
    private String viewSelectCode;
    //
//    @TableField(exist = false)
//    private Set<Event> events = new HashSet<Event>();
    @TableField("datagrid_code")
    private String dataGridCode;
    @TableLogic(
            value = "1",
            delval = "0"
    )
    @TableField("valid")
    private Boolean valid;

    @TableField(
            value = "create_staff_id",
            fill = FieldFill.INSERT,
            jdbcType = JdbcType.BIGINT
    )
    private Long createStaffId;
    @TableField(
            value = "create_time",
            fill = FieldFill.INSERT,
            jdbcType = JdbcType.TIMESTAMP,
            typeHandler = UTCToStringTypeHandler.class
    )
    private String createTime;

    public String getConfig() {
        if (config != null && !config.isEmpty()) {
            if (config.indexOf("<?xml version=\"1.0\" encoding=\"UTF-8\"?>") == -1) {
                config = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + config;
            }
        }
        return config;
    }

    @SuppressWarnings("rawtypes")
    @TableField(exist = false)
    private Map configMap;

    @Transient
    public Map getConfigMap() {
        if (this.configMap == null) {
            this.configMap = (Map) SerializeUitls.deserialize(getConfig());
        }
        return configMap;
    }

}
