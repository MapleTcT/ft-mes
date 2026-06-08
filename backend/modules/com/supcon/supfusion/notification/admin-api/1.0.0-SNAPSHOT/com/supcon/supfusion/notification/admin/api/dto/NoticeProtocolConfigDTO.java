package com.supcon.supfusion.notification.admin.api.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.supcon.supfusion.framework.cloud.common.json.converters.IDJsonSerializer;
import com.supcon.supfusion.framework.cloud.common.pojo.DTO;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class NoticeProtocolConfigDTO extends DTO {

    /**
     * id
     */
    private Long id;

    /**
     * 协议ID
     */
    private String protocol;

    /**
     * 配置项内容
     */
    private String configValue;
}
