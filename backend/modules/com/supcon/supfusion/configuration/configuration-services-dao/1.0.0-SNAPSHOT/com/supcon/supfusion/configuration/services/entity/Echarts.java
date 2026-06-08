package com.supcon.supfusion.configuration.services.entity;

import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueCodeEntity;
import lombok.Data;

import javax.persistence.Transient;
import java.io.Serializable;
import java.util.List;

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
@javax.persistence.Entity
//@Table(name = Echarts.TABLE_NAME)
public class Echarts extends AbstractAuditUniqueCodeEntity implements Serializable {

    // @Fields serialVersionUID : TODO
    private static final long serialVersionUID = 1L;
    public static final String TABLE_NAME = "ec_echarts";
    @Transient
    protected EcEnv ecEnv = EcEnv.product;
    public static final String LEGEND_TOP = "top";
    public static final String LEGEND_BOTTOM = "bottom";
    public static final String LEGEND_RIGHT = "right";
    public static final String LEGEND_LEFT = "left";
    public static final String LEGEND_RIGHTTOP = "righttop";
    // @Fields title : 标题
//    @BAPInternational(fieldName = "titleInternational", replace = false)
    private String title;
    // @Fields isFirstLoad : 是否加载数据
    private Boolean isFirstLoad = true;
    // @Fields isShowLegend : 是否显示图例
    private Boolean isShowLegend = true;
    // @Fields legendPosition : 图例位置
    private String legendPosition;
    // @Fields isShowMagicType : 是否显示工具栏的切换图表
    private Boolean isShowMagicType;
    // @Fields modelList :
    @Transient
    List<EchartsModel> modelList;
    // @Fields events : 事件
    @Transient
    private List<Event> events;
    // @Fields projFlag : 是否工程期
    private Boolean projFlag;
    // @Fields layoutName : 图表所属布局
    @Transient
    private String layoutName;

    public Echarts(String code) {
        setCode(code);
    }

    public Echarts() {
    }

    @Transient
    public String getLayoutName() {
        if (getCode() == null || "".equals(getCode())) {
            return null;
        }
        return "layout" + getCode().substring(getCode().lastIndexOf("-") + 1);
    }
    /* getter setter end */

    @Override
    protected String _getEntityName() {
        return getClass().getName();
    }

    @Override
    public String toString() {
        return "Echarts [code=" + getCode() + ", title=" + getTitle() + ", isFirstLoad=" + getIsFirstLoad()
                + ", isShowLegend=" + getIsShowLegend() + ", isShowMagicType=" + getIsShowMagicType()
                + ", legendPosition=" + getLegendPosition() + "]";
    }

}
