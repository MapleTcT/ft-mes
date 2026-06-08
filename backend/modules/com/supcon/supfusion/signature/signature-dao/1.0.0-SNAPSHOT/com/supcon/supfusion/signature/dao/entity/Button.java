/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD.
 * All rights reserved.
 */
package com.supcon.supfusion.signature.dao.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.supcon.supfusion.framework.scaffold.mybatis.type.handler.UTCToStringTypeHandler;
import com.supcon.supfusion.signature.dao.entity.base.AbstractEntity;
import com.supcon.supfusion.signature.dao.entity.base.LogicBasePO;
import com.supcon.supfusion.signature.dao.enums.EcEnv;
import com.supcon.supfusion.signature.dao.enums.OperateType;
import com.supcon.supfusion.signature.dao.enums.RegionType;
import lombok.Data;
import org.apache.ibatis.type.JdbcType;


/**
 * @author zhuyuyin
 * @version $Id$
 */
@Data
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

}