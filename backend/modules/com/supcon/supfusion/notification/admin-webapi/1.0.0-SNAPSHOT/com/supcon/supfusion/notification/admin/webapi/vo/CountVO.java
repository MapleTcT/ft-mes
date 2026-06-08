package com.supcon.supfusion.notification.admin.webapi.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.supcon.supfusion.framework.cloud.common.json.converters.IDJsonSerializer;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.Data;

/**
 * @author zhang yafei
 */
@Data
public class CountVO extends VO {
    @JsonSerialize(using = IDJsonSerializer.class)
    private long count;
}
