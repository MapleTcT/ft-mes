package com.supcon.supfusion.notification.apiserver.common.bean;

import com.supcon.supfusion.framework.cloud.common.pojo.BO;
import com.supcon.supfusion.notification.apiserver.common.execption.NotificationApiServerError;
import com.supcon.supfusion.notification.apiserver.common.execption.NotificationApiServerExecption;
import com.supcon.supfusion.notification.common.bean.RangeType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class RangeBO extends BO {
    private RangeType rangeType;
    private List<String> codes;
    private String url;
}
