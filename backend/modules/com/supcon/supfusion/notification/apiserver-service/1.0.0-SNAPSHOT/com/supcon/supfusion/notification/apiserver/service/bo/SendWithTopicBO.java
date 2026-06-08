package com.supcon.supfusion.notification.apiserver.service.bo;

import com.alibaba.fastjson.JSONObject;
import com.supcon.supfusion.framework.cloud.common.pojo.BO;
import com.supcon.supfusion.notification.apiserver.common.bean.RangeBO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SendWithTopicBO extends BO {
    /**
     * 业务方事务编号
     */
    private String bsmodCode;
    /**
     * 业务模块名称
     */
    private String bsmodName;
    /**
     * 主题编号
     */
    private String topicCode;
    /**
     * 发送协议
     */
    private List<String> protocols;
    /**
     * 消息接收范围
     */
    private List<RangeBO> receivers;
    /**
     * 消息内容入参
     */
    private JSONObject param;
    /**
     * 主题ID
     */
    private Long topicId;
}
