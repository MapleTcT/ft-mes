package com.supcon.supfusion.auditlog.dao.po.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 审计模块PO类
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = AuditlogModelPO.TABLE_NAME, autoResultMap = true)
public class AuditlogModelPO {
    public static final String TABLE_NAME = "auditlog_model";

    private Long id;
    private String modelCode;
    private String moduleCode;
}
