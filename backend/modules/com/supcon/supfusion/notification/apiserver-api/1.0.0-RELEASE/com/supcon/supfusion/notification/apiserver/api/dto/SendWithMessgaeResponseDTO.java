package com.supcon.supfusion.notification.apiserver.api.dto;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendWithMessgaeResponseDTO extends VO {
    private String code;
}
