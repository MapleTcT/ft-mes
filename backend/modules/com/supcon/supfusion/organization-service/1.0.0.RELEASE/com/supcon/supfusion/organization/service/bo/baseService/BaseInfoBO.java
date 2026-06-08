package com.supcon.supfusion.organization.service.bo.baseService;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

/**
 * 人员相关信息
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class BaseInfoBO {

    /**
     * id主键
     */
    private Long id;

    /**
     * 编码或编号
     */
    private String code;

    /**
     * 名称
     */
    private String name;


}
