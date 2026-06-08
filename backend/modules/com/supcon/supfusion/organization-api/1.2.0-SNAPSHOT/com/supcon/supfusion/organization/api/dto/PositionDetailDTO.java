package com.supcon.supfusion.organization.api.dto;

import com.supcon.supfusion.framework.cloud.common.pojo.DTO;
import lombok.*;

/**
 * 岗位类
 * @author root
 *
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PositionDetailDTO extends DTO {

    /**
     * 岗位id
     */
    private Long id;

    /**
     * 岗位编码
     */
    private String code;

    /**
     * 岗位名称
     */
    private String name;

    /**
     * 所属公司id
     */
    private Long companyId;

    /**
     * 上级岗位id（如果上级是公司则为空）
     */
    private Long parentId;

    /**
     * 描述
     */
    private String description;

    /**
     * 是否为主岗
     */
    private Boolean mainPosition;

    /**
     * 部门id
     */
    private Long depId;

    private Long userId;

}
