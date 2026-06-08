package com.supcon.supfusion.notification.admin.webapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.Data;

@Data
public class NoticeTaskProtocolVO extends VO {
    private String content;
    private String url;
}
