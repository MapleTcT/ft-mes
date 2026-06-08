package com.supcon.supfusion.notification.apiserver.service.bo;

import com.supcon.supfusion.framework.cloud.common.pojo.BO;
import com.supcon.supfusion.notification.apiserver.common.bean.RangeBO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SendWithMessageBO extends BO {
    /**
     * 业务方事务编号
     */
    private String bsmodCode;
    /**
     * 业务模块名称
     */
    private String bsmodName;
    /**
     * 消息接收范围
     */
    private List<RangeBO> receivers;
    /**
     * 消息内容
     */
    private List<MessageBO> contents;
}
