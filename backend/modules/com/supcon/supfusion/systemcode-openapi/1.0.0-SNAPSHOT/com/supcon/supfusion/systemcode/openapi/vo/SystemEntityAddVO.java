package com.supcon.supfusion.systemcode.openapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * @author 
 * @date 20-5-11 下午14:48
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class SystemEntityAddVO extends VO {
	private static final long serialVersionUID = 7370272234687519755L;

	/**
	 * 数据字典编码
	 */
	@NotBlank(message = "systemCode.CODE_PARAM_NECESSARY")
	@Length(max = 100, message = "systemCode.CODE_LENGTH_MAX_ERROR")
	private String code;

	/**
	 * 数据字典名称
	 */
	@NotBlank(message = "systemCode.NAME_PARAM_NECESSARY")
	@Length(max = 500, message = "systemCode.NAME_LENGTH_MAX_ERROR")
	private String name;

	/**
	 * 类型
	 */
	@NotBlank(message = "systemCode.TYPE_PARAM_NECESSARY")
	private String type;

	/**
	 * 所属公司ID
	 */
	@NotNull(message = "systemCode.COMPANY_ID_PARAM_NECESSARY")
	private Long cid;

	/**
	 * 模块ID
	 */
	@NotBlank(message = "systemCode.MODULE_ID_PARAM_NECESSARY")
	private String moduleId;

	/**
	 * 版本
	 */
	private Long rowVersion;

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
	@Length(max = 255, message = "systemCode.MEMO_LENGTH_MAX_ERROR")
	private String memo;

}
