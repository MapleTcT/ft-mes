package com.supcon.supfusion.notification.admin.api.dto;

import com.supcon.supfusion.framework.cloud.common.pojo.BO;
import com.supcon.supfusion.framework.cloud.common.pojo.DTO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ProtocolDTO extends DTO {
    /**
     * 消息协议类型
     */
    private String protocol;
    /**
     * 消息协议展示名称
     */
    private String name;

}
