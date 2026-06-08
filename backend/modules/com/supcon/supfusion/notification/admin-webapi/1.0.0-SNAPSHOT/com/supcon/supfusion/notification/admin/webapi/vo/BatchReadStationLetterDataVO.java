package com.supcon.supfusion.notification.admin.webapi.vo;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@ApiModel
public class BatchReadStationLetterDataVO {
    @NotNull
    private String id;
    private String protocol;
    @NotNull
    private Long shardingTime;
}
