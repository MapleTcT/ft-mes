package com.supcon.supfusion.notification.admin.service.bo;

import com.supcon.supfusion.framework.cloud.common.pojo.BO;
import io.swagger.annotations.ApiParam;
import lombok.Data;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Date;

/**
 * 查询条件实体类
 */

@Data
public class NoticeMsgSelectBO  extends BO {

    /**
     * 主题id
     */
    private Long topicId;

    /**
     * 发送协议
     */
    private String protocol;

    /**
     * 分表时间戳
     */
    private Date shardingTime;

    /**
     * 接收人名称
     */
    private String staffName;
    /**
     * 读取状态
     */
    private String readStatus;

}
