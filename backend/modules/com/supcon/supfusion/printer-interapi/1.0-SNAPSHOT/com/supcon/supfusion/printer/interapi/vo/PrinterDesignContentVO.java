package com.supcon.supfusion.printer.interapi.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.printer.common.Constants;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;
import org.apache.ibatis.type.JdbcType;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PrinterDesignContentVO extends VO {

    /**
     * 模板id
     */
    @NotNull(message = Constants.PARAM_TEMPLATEID_NECESSARY)
    @ApiModelProperty(value = "打印模板id", required = true)
    private Long templateId;

    /**
     * 设计模板json内容
     */
    @NotBlank(message = Constants.PARAM_TEMPLATE_CONTENT_NECESSARY)
    @ApiModelProperty(value = "设计模板json内容", required = true)
    private String content;

    /**
     * 启停状态
     */
    @ApiModelProperty(value = "发布状态，1：已发布、2：未发布、3：修改中")
    private Integer enabled;
}
