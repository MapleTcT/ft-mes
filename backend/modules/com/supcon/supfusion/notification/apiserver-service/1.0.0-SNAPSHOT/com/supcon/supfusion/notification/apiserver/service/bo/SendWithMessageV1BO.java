package com.supcon.supfusion.notification.apiserver.service.bo;

import com.supcon.supfusion.framework.cloud.common.pojo.BO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SendWithMessageV1BO extends BO {

    /**
     * 业务方事务编号
     */
    private String bsmodCode;
    /**
     * 业务模块名称
     */
    private String bsmodName;
    /**
     * 发送协议
     */
    private List<String> protocols;
    /**
     * 接收范围：用户ID
     */
    private List<String> userIds;
    /**
     * 消息标题
     */
    private String title;
    /**
     * 消息内容
     */
    private String text;
    /**
     * 消息内容,与title和text冲突
     */
    private String content;
}
