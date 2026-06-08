package com.supcon.supfusion.notification.apiserver.api.dto;

import com.alibaba.fastjson.JSONObject;
import com.supcon.supfusion.framework.cloud.common.pojo.BO;
import lombok.Data;

import java.util.List;

@Data
public class SendWithTmplateDTO {
    /**
     *  业务方事务编号
     */
    private String bsmodCode;
    /**
     *  业务模块名称
     */
    private String bsmodName;
    /**
     *  模板id
     */
    private List<Long> tmplateId;
    /**
     *  发送协议
     */
    private Long protocolid;
    /**
     *  消息接收范围
     */
    private List<String> receivers;
    /**
     *  消息内容入参
     */
    private JSONObject param;
}
