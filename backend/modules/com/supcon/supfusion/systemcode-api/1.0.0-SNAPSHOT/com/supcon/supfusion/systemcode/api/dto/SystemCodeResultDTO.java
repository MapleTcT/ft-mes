package com.supcon.supfusion.systemcode.api.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import com.supcon.supfusion.framework.cloud.common.pojo.DTO;
import com.supcon.supfusion.systemcode.common.constants.Constants;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.validator.constraints.Length;


/**
 * 数据字典某一类下具体的编码和值的描述类
 * @author root
 *
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class SystemCodeResultDTO extends DTO {
	
	private static final long serialVersionUID = 6755105232352834077L;

	/**
	 * 主键ID
	 */
	private Long id;

	/**
	 * 系统字典项编码
	 */
	private String entityCode;

	/**
	 * 值的编码
	 */
	private String code;

	/**
	 * 显示名称
	 */

	private String displayName;

	/**
	 * 值的名称,国际化键
	 */
	private String name;

	/**
	 * 所属公司ID
	 */
	private Long cid;

	/**
	 * 所属公司名称
	 */
	private String companyName;

	/**
	 * 父节点ID
	 */
	private Long parentId;

	/**
	 * 父节点名称
	 */
	private String parentName;

	/**
	 * 层级
	 */
	private Integer layNo;

	/**
	 * 序列号id
	 */
	private Long seqId;

	/**
	 * 备注
	 */
	private String memo;

	/**
	 * 描述C
	 */
	private String desC;

	/**
	 * 描述B
	 */
	private String desB;

	/**
	 * 描述A
	 */
	private String desA;

	/**
	 * 顺序
	 */
	private Double sort;

	/**
	 * 是否默认
	 */
	private Integer defaultFlag;

	/**
	 * 类型
	 */
	private String type;
}
