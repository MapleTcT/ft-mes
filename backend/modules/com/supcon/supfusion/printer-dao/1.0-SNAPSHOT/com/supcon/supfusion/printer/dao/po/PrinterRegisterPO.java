package com.supcon.supfusion.printer.dao.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import lombok.*;

/**
 * 数据注册entity
 * @author yuyimao
 * @date 2020/10/16 5:01 下午
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = PrinterRegisterPO.TABLE_NAME, autoResultMap = true)
public class PrinterRegisterPO extends BaseEntity {
    /**
     * po对应的表名
     */
    public static final String TABLE_NAME = "printer_register";

    /**
     * 主键
     */
    private Long id;

    /**
     * 数据来源
     */
    private Integer source;

    /**
     * 服务地址
     */
    private String serviceUrl;

    /**
     * 服务类型
     */
    private Integer serviceType;

    /**
     * http请求方式
     */
    private Integer callType;
}
