package com.supcon.supfusion.ws.service.pojo;


import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import java.io.Serializable;

/**
 * @author lifangyuan
 */
@Data
public class NoticeMessage implements Serializable {
    private String topic;
    private String userName;
    private JSONObject data;
}
