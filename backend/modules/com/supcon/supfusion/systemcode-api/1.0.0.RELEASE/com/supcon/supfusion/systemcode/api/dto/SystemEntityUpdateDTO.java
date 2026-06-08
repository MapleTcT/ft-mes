package com.supcon.supfusion.systemcode.api.dto;

import com.supcon.supfusion.framework.cloud.common.pojo.DTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class SystemEntityUpdateDTO extends DTO {

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
    private String type;

    /**
     * 备注
     */
    @Length(max = 255, message = "systemCode.MEMO_LENGTH_MAX_ERROR")
    private String memo;
}
