package com.supcon.supfusion.base.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;
import java.util.List;

/**
 * 数据字典某一类字典项的描述类
 * @author caokele
 *
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = SystemEntityPO.TABLE_NAME)
public class SystemEntityPO {
	public static final String TABLE_NAME = "sys_entity";
	private static final long serialVersionUID = -2463691260984995695L;

	/**
	 * 主键ID
	 */
	@Id
	@Column(name="ID")
	private Long id;

	/**
	 * 数据字典编码
	 */
	@Column(name="CODE")
	private String code;

	/**
	 * 数据字典名称
	 */
	@Column(name="NAME")
	private String name;

	/**
	 * 版本
	 */
	@Column(name="ROW_VERSION")
//	@Version
	private Long rowVersion=0L;

	/**
	 * 类型
	 */
	@Column(name="TYPE")
	private String type;

	/**
	 * 所属公司ID
	 */
	@Column(name="CID")
	private Long cid;

	/**
	 * 所属公司名称
	 */
	@Transient
	private String companyName;

	/**
	 * 所属模块ID
	 */
	@Column(name="MODULE_ID")
	private String moduleId;

	/**
	 * 所属模块名称
	 */
	@Transient
	private String moduleName;

	/**
	 * 是否有效
	 */
	@Column(name="VALID")
	private Integer valid;

	/**
	 * 是否多选
	 */
	@Column(name="MULTI_FLAG")
	private Integer multiFlag;

	/**
	 * 是否系统默认
	 */
	@Column(name="SYS_DEFAULT")
	private Integer sysDefault;

	/**
	 * 备注
	 */
	@Column(name="MEMO")
	private String memo;

	/**
	 * 字典项的直接子编码节点
	 */
	@Transient
	private List<SystemCodePO> children;
}
