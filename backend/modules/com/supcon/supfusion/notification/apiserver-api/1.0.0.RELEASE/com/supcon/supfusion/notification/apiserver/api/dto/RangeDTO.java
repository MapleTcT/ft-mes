package com.supcon.supfusion.notification.apiserver.api.dto;

import com.supcon.supfusion.framework.cloud.common.pojo.DTO;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.notification.common.bean.RangeType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Collection;
import java.util.List;

@Setter
@Getter
@ToString
public class RangeDTO extends DTO {
    private RangeType rangeType;
    private Collection<String> codes;
    private String url;
}
