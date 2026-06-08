package com.supcon.supfusion.systemcode.api.dto;

import com.supcon.supfusion.framework.cloud.common.pojo.DTO;

import lombok.*;

/**
 * 数据字典某一类字典项的描述类
 * @author root
 *
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class SystemEntityResultDTO extends DTO {
	
	private static final long serialVersionUID = -2463691260984995695L;

	/**
	 * 主键ID
	 */
	private Long id;

	/**
	 * 数据字典编码
	 */
	private String code;

	/**
	 * 数据字典名称
	 */
	private String name;

	/**
	 * 显示名称
	 */
	private String displayName;

	/**
	 * 版本
	 */
	private Long rowVersion;

	/**
	 * 类型
	 */
	private String type;

	/**
	 * 所属公司ID
	 */
	private Long cid;

	/**
	 * 所属公司名称
	 */
	private String companyName;

	/**
	 * 所属模块ID
	 */
	private String moduleId;

	/**
	 * 所属模块名称
	 */
	private String moduleName;

	/**
	 * 是否有效
	 */
	private Integer valid;

	/**
	 * 是否多选
	 */
	private Integer multiFlag;

	/**
	 * 是否系统默认
	 */
	private Integer sysDefault;

	/**
	 * 备注
	 */
	private String memo;

}
