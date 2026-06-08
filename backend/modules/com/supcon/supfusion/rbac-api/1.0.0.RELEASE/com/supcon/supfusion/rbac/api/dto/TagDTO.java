package com.supcon.supfusion.rbac.api.dto;

import com.supcon.supfusion.framework.cloud.common.pojo.DTO;
import lombok.Data;

@Data
public class TagDTO extends DTO {

    private static final long serialVersionUID = -2251920177887709041L;
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 标签类型
     */
    private String type;

    /**
     * 标签名
     */
    private String name;

    /**
     * 公司ID
     */
    private Long cid;

    /**
     * 关联ID
     */
    private Long objectid;
}
