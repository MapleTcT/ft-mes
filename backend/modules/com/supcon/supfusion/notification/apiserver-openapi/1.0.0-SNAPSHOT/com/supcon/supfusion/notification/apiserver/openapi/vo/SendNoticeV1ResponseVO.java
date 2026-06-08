package com.supcon.supfusion.notification.apiserver.openapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendNoticeV1ResponseVO extends VO {
    private String taskCode;
}
