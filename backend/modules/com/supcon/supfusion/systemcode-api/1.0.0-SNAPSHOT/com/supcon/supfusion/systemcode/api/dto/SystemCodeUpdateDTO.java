package com.supcon.supfusion.systemcode.api.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import com.supcon.supfusion.framework.cloud.common.pojo.DTO;

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
public class SystemCodeUpdateDTO extends DTO {
	
	private static final long serialVersionUID = 6755105232352834077L;

	/**
	 * 系统字典项编码
	 */
	@NotBlank(message = "systemCode.CODE_PARAM_NECESSARY")
	private String entityCode;

	/**
	 * 值的编码
	 */
	@NotBlank(message = "systemCode.CODE_PARAM_NECESSARY")
	@Length(max = 50, message = "systemCode.CODE_LENGTH_MAX_ERROR")
	private String code;

	/**
	 * 所属公司ID
	 */
	@NotNull(message = "systemCode.COMPANY_ID_PARAM_NECESSARY")
	private Long cid;

	/**
	 * 显示名称
	 */
	@NotBlank(message = "systemCode.NAME_PARAM_NECESSARY")
	@Length(max = 500, message = "systemCode.NAME_LENGTH_MAX_ERROR")
	private String displayName;

	/**
	 * 值的名称,国际化键
	 */
	@NotBlank(message = "systemCode.NAME_PARAM_NECESSARY")
	@Length(max = 500, message = "systemCode.NAME_LENGTH_MAX_ERROR")
	private String name;

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
	@Length(max = 255, message = "systemCode.MEMO_LENGTH_MAX_ERROR")
	private String memo;

	/**
	 * 描述C
	 */
	@Length(max = 255, message = "systemCode.DES_LENGTH_MAX_ERROR")
	private String desC;

	/**
	 * 描述B
	 */
	@Length(max = 255, message = "systemCode.DES_LENGTH_MAX_ERROR")
	private String desB;

	/**
	 * 描述A
	 */
	@Length(max = 255, message = "systemCode.DES_LENGTH_MAX_ERROR")
	private String desA;

	/**
	 * 顺序
	 */
	private Double sort;

	/**
	 * 类型
	 */
	private String type;

	/**
	 * 是否默认
	 */
	private Integer defaultFlag;

	/**
	 * 层级全路径
	 */
	private String fullPath;
}
