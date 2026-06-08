package com.supcon.supfusion.custon.property.dao.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.custon.property.common.enums.EcEnv;
import com.supcon.supfusion.custon.property.dao.entity.base.LogicBasePO;
import lombok.Data;

/**
 * Copyright: Copyright (c) 2018 SUPCON
 *
 * @ClassName: Echarts.java
 * @Description: Echarts图表实体类
 * @version: v1.0.0
 * @author: huning
 * @date: 2018年12月25日 上午10:55:31
 */
@Data
@TableName(value = "ec_echarts",autoResultMap = true)
public class Echarts extends LogicBasePO {

    // @Fields serialVersionUID : TODO
    private static final long serialVersionUID = 1L;
    @TableId
    private String code;
    private EcEnv ecEnv = EcEnv.product;
    public static final String LEGEND_TOP = "top";
    public static final String LEGEND_BOTTOM = "bottom";
    public static final String LEGEND_RIGHT = "right";
    public static final String LEGEND_LEFT = "left";
    public static final String LEGEND_RIGHTTOP = "righttop";

    private String title;

    private Boolean isFirstLoad = true;

    private Boolean isShowLegend = true;

    private String legendPosition;

    private Boolean isShowMagicType;

    private Boolean projFlag;

}
