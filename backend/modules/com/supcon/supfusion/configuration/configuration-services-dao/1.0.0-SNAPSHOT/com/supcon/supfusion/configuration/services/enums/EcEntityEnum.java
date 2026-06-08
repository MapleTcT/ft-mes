package com.supcon.supfusion.configuration.services.enums;

import com.supcon.supfusion.configuration.services.entity.*;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueCodeEntity;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractCodeEntity;

/**
 * 
 * 
 * @author tanzhengyang
 * @version $Id$
 */
public enum EcEntityEnum {
	Module(com.supcon.supfusion.configuration.services.entity.Module.class), // 模块
	Entity(com.supcon.supfusion.configuration.services.entity.Entity.class), // 实体
	Model(com.supcon.supfusion.configuration.services.entity.Model.class), // 模型
	View(com.supcon.supfusion.configuration.services.entity.View.class), // 视图
	ExtraView(ExtraView.class), // ExtraView
	DataGrid(DataGrid.class), // DataGrid
	Property(com.supcon.supfusion.configuration.services.entity.Property.class), // 字段
	ExtraQueryJson(ExtraQueryJson.class), // 1对多快速查询
	DefaultAdvCond(DefaultAdvCond.class), //
	FastQueryJson(FastQueryJson.class), // 快速查询
	AdvQueryJson(AdvQueryJson.class), // 高级查询
	Button(com.supcon.supfusion.configuration.services.entity.Button.class), // 按钮
	CustomerCondition(com.supcon.supfusion.configuration.services.entity.CustomerCondition.class), // 自定义条件
	SpecialPermission(com.supcon.supfusion.configuration.services.entity.SpecialPermission.class), // 特殊权限
	DataGroup(DataGroup.class), // 数据分组
	DataClassific(com.supcon.supfusion.configuration.services.entity.DataClassific.class), // 数据分类
	Field(com.supcon.supfusion.configuration.services.entity.Field.class), // 页面字段
	Layout(com.supcon.supfusion.configuration.services.entity.Layout.class), // 布局
	Validate(com.supcon.supfusion.configuration.services.entity.Validate.class), // 验证
	Sql(com.supcon.supfusion.configuration.services.entity.Sql.class), // SQL语句
	Event(Event.class),// 事件
	Echarts(Echarts.class),
	EchartsModel(EchartsModel.class),
	SqlModel(SqlModel.class);

	public Class<? extends AbstractCodeEntity> clazz;

    private EcEntityEnum(Class<? extends AbstractCodeEntity> clazz) {
		this.clazz = clazz;
    }

    @SuppressWarnings("unused")
    public Class<? extends AbstractCodeEntity> getClazz() {
		return clazz;
    }
}
