package com.supcon.supfusion.rbac.api.dto;

import com.supcon.supfusion.framework.cloud.common.pojo.DTO;
import lombok.Data;

/**
 * <p>
 * 自定义权限表
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-16
 */
@Data
public class CustomPermissionDTO extends DTO {


    /**
     * 编码
     */
    private String code;

    /**
     * 模式DEV或PRODUCT 默认PRODUCT
     */
    private String ecEnv;

    /**
     * 版本
     */
    private Integer version;

    /**
     * 实体编码
     */
    private String entityCode;

    /**
     * 模块编码
     */
    private String moduleCode;

    /**
     * 备注
     */
    private String memo;

    /**
     * 标题
     */
    private String title;

    /**
     * 条件SQL
     */
    private String conditionSql;

    /**
     * JSON条件
     */
    private String jsonCondition;

    /**
     * 视图编码
     */
    private String viewCode;


}
