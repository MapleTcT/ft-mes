package com.supcon.supfusion.notification.admin.service.bo;

import com.supcon.supfusion.framework.cloud.common.pojo.BO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class NoticeTopicListBO extends BO {
    private Long id;
    private String code;
    private String name;
    private String protocolName;
    private String templateName;
    private String receiver;
    private List<String> staffCodes;
    private List<String> deptCodes;
    private List<String> roleCodes;
    private List<String> positionCodes;
}
