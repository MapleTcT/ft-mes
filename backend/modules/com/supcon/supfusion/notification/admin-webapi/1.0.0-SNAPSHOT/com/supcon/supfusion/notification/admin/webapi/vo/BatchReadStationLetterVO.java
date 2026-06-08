package com.supcon.supfusion.notification.admin.webapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@ApiModel
public class BatchReadStationLetterVO extends VO {
    @NotNull
    private List<BatchReadStationLetterDataVO> datas;
}
