package com.supcon.supfusion.systemcode.webapi.vo;


import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.Length;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.systemcode.common.constants.Constants;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @author 
 * @date 20-5-11 下午14:48
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class SystemCodeAddVO extends VO {

	private static final long serialVersionUID = 3350339092086142306L;

	/**
	 * 系统字典项编码
	 */
	@NotBlank(message = "systemCode.CODE_PARAM_NECESSARY")
	private String entityCode;

	/**
	 * 值的编码
	 */
	@NotBlank(message = "systemCode.CODE_PARAM_NECESSARY")
	@Length(max = 100, message = "systemCode.CODE_LENGTH_MAX_ERROR")
	private String code;

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
	 * 所属公司ID
	 */
	@NotNull(message = "systemCode.COMPANY_ID_PARAM_NECESSARY")
	private Long cid;

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
