package com.supcon.supfusion.printer.interapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.printer.common.Constants;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import javax.validation.constraints.NotBlank;
import java.util.List;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PrinterTemplateBatchUpdateVO extends VO {

    @ApiModelProperty(value = "发布状态，1：已发布，2：未发布，3：修改中, 4:已停用")
    private Integer enabled;

    @ApiModelProperty(value = "模板id")
    private List<Long> templateIds;
}
