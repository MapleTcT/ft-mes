package com.supcon.supfusion.ws.client.dto;

import com.alibaba.fastjson.JSONObject;
import com.supcon.supfusion.framework.cloud.common.pojo.DTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author lifangyuan
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoticeMessageDTO extends DTO {
    private String userName;
    private JSONObject data;
}
