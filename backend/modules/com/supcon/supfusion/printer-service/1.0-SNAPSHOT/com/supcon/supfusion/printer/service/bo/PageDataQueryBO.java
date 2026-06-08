package com.supcon.supfusion.printer.service.bo;


import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.printer.common.Constants;
import lombok.*;

import javax.validation.constraints.NotNull;

/**
 * pageData
 * @author yuyimao
 * @date 2020/10/16 5:01 下午
 */

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PageDataQueryBO{

    /**
     * 页面来源
     */
    private Integer source;

    /**
     * 页面名称
     */
    private String name;

    /**
     * 页面父编码
     */
    private String pCode;
}
