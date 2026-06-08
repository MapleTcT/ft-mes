package com.supcon.supfusion.notification.admin.webapi.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.supcon.supfusion.framework.cloud.common.json.converters.IDJsonSerializer;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.Data;

@Data
public class StationLetterUnReadNumVO extends VO {
    @JsonSerialize(using = IDJsonSerializer.class)
    private long count;
}
