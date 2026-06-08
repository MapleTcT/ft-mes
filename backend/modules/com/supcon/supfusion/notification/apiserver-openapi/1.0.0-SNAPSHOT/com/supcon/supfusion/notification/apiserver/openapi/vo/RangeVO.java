package com.supcon.supfusion.notification.apiserver.openapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.notification.common.bean.RangeType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Setter
@Getter
@ToString
public class RangeVO extends VO {
    private RangeType rangeType;
    private List<String> codes;
    private String url;
}
