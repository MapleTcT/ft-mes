package com.supcon.supfusion.auditlog.dao.po.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 运行时属性表
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = EcPropertyPO.TABLE_NAME, autoResultMap = true)
public class EcPropertyPO {
    public static final String TABLE_NAME = "ec_property";

    private String code;
    private String displayName;
}
