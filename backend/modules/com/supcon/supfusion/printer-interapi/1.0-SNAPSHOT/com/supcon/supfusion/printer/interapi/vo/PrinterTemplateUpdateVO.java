package com.supcon.supfusion.printer.interapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.printer.common.Constants;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.List;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PrinterTemplateUpdateVO extends VO {
    /**
     * 模板编号
     */
    @ApiModelProperty(value = "模板id")
    private Long id;

    /**
     * 模板名称
     */
    @NotBlank(message = Constants.PARAM_TEMPLATENAME_NECESSARY)
    @ApiModelProperty(value = "模板名称")
    private String templateName;

    /**
     * 国际化编号
     */
    @ApiModelProperty(value = "国际化编号")
    private String i18nKey;

    /**
     * 模板编码
     */
    @NotBlank(message = Constants.PARAM_TEMPLATECODE_NECESSARY)
    @Pattern(regexp = "[a-zA-Z0-9_]+", message = Constants.PARAM_TEMPLATECODE_FORMAT_ERROR)
    @ApiModelProperty(value = "模板编码")
    private String templateCode;

    /**
     * app编号
     */
    @NotBlank(message = Constants.PARAM_APPID_NECESSARY)
    @ApiModelProperty(value = "app编号")
    private String appId;

    /**
     * 模板标签
     */
    @ApiModelProperty(value = "模板标签")
    private String labelNames;

    /**
     * 描述
     */
    @ApiModelProperty(value = "描述")
    private String templateDesc;

    /**
     * 启停状态
     */
    @ApiModelProperty(value = "发布状态，1：已发布，2：未发布，3：修改中, 4:已停用")
    private Integer enabled;

    /**
     * 关联页面
     */
    @ApiModelProperty(value = "关联页面")
    private List<PrinterTemplateRelationPageVO> pageDatas;
}
