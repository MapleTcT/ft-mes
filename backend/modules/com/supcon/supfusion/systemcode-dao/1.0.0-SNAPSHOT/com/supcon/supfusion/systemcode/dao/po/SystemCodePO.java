package com.supcon.supfusion.systemcode.dao.po;


import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = SystemCodePO.TABLE_NAME, autoResultMap = true)
public class SystemCodePO extends BaseEntity {

	
	private static final long serialVersionUID = -1311087735589441011L;

    public static final String TABLE_NAME = "sys_code";

	/**
	 * 主键ID
	 */
	@TableId(value = "id")
	private Long id;

	/**
	 * 版本
	 */
	@TableField(value = "row_version")
	private Long rowVersion;

	/**
	 * 实体编码
	 */
	@TableField(value = "entity_code")
	private String entityCode;

	/**
	 * 值的编码
	 */
	@TableField(value = "code")
	private String code;

	/**
	 * 值的名称
	 */
	@TableField(value = "name")
	private String name;

	/**
	 * 显示名称
	 */
	@TableField(value = "display_name")
	private String displayName;

	/**
	 * 类型
	 */
	@TableField(value = "type")
	private String type;

	/**
	 * 所属公司ID
	 */
	@TableField(value = "cid")
	private Long cid;

	/**
	 * 上级节点ID
	 */
	@TableField(value = "parent_id")
	private Long parentId;

	/**
	 * 父节点名称
	 */
	@TableField(value = "parent_name")
	private String parentName;

	/**
	 * 父节点显示名称
	 */
	@TableField(exist = false)
	private String parentDisplayName;

	/**
	 * 父节点编码
	 */
	@TableField(exist = false)
	private String parentCode;

	/**
	 * 父节点编码拼接
	 */
	@TableField(exist = false)
	private String parentCodeStr;

	/**
	 * 层级
	 */
	@TableField(value = "lay_no")
	private Integer layNo;

	/**
	 * 层级结构
	 */
	@TableField(value = "lay_rec")
	private String layRec;

	/**
	 * 序列号ID
	 */
	@TableField(value = "seq_id")
	private Long seqId;

	/**
	 * 备注
	 */
	@TableField(value = "memo")
	private String memo;

	/**
	 * 描述A
	 */
	@TableField(value = "des_a")
	private String desA;

	/**
	 * 描述B
	 */
	@TableField(value = "des_b")
	private String desB;

	/**
	 * 描述C
	 */
	@TableField(value = "des_c")
	private String desC;

	/**
	 * 顺序
	 */
	@TableField(value = "sort")
	private Double sort;

	/**
	 * 所属公司名称
	 */
	@TableField(exist = false)
	private String companyName;

	/**
	 * 是否有效
	 */
	@TableField(value = "valid")
	private Integer valid;

	/**
	 * 是否默认
	 */
	@TableField(value = "default_flag")
	private Integer defaultFlag;

	/**
	 * 是否叶子节点
	 */
	@TableField(value = "leaf")
	private boolean leaf = false;

	/**
	 * 层级编码全路径
	 */
	@TableField(value = "full_path")
	private String fullPath;

	/**
	 * 层级名称全路径
	 */
	@TableField(value = "full_path_name")
	private String fullPathName;

	/**
	 * 值
	 */
	@TableField(exist = false)
	private String value;

	public static String getIdFieldName() {
		return "id";
	}

	public static String getCodeFieldName() {
		return "code";
	}

	public static String getNameFieldName() {
		return "name";
	}

	public static String getDisplayNameFieldName() {
		return "display_name";
	}

	public static String getRowVersionFieldName() {
		return "row_version";
	}

	public static String getEntityCodeFieldName() {
		return "entity_code";
	}

	public static String getTypeFieldName() {
		return "type";
	}

	public static String getCidFieldName() {
		return "cid";
	}

	public static String getParentIdFieldName() {
		return "parent_id";
	}

	public static String getParentNameFieldName() {
		return "parent_name";
	}

	public static String getValidFieldName() {
		return "valid";
	}

	public static String getFullPathFieldName() {
		return "full_path";
	}

	public static String getLayNoFieldName() {
		return "lay_no";
	}

	public static String getSortFieldName() {
		return "sort";
	}
}
