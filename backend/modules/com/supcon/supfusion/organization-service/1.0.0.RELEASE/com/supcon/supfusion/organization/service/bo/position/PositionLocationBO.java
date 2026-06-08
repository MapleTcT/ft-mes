package com.supcon.supfusion.organization.service.bo.position;

import lombok.*;

/**
 * 岗位修改位置PO
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PositionLocationBO {
    /**
     * 岗位id
     */
    private Long id;

    /**
     * 前续岗位id
     */
    private Long upId;

    /**
     * 父级岗位id
     */
    private Long parentId;
}
