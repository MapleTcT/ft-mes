package com.supcon.supfusion.rbac.openapi.vo.roleUser;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 导出文件生成状态 不存数据库 只存redis
 * </p>
 *
 * @author 袁阳
 * @since 2020-07-01
 */
@Data
public class ExportFileStatusVO implements Serializable {

    private static final long serialVersionUID=1L;

    /**
     * 主键ID
     */
    private String id;

    /**
     * 状态 1成功 0失败
     */
    private Integer status;

}
