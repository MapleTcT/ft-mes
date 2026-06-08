package com.supcon.supfusion.notification.app.config.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.Data;

/**
 * ${description}
 *
 * @author chenweinan
 * @create 2020/7/8 14:24
 */
@Data
public class NoticeTemplateVO extends VO {


    private Long protocol;


    private String template;
    
    private String code;
    
    private String name;
    
    private String memo;

}
