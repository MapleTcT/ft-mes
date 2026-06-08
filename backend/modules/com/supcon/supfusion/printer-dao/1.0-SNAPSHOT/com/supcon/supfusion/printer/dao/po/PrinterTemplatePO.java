package com.supcon.supfusion.printer.dao.po;


import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import lombok.*;

/**
 * 打印模板entity
 * @author yuyimao
 * @date 2020/10/16 5:01 下午
 */

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = PrinterTemplatePO.TABLE_NAME, autoResultMap = true)
public class PrinterTemplatePO extends BaseEntity {
    /**
     * po对应的表名
     */
    public static final String TABLE_NAME = "printer_template";

    /**
     * 主键
     */
    private Long id;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 模板名称国际化编号
     */
    private String i18nKey;

    /**
     * 模板编码
     * */
    private String templateCode;

    /**
     * app
     * */
    private String appId;

    /**
     * 标签名称
     * */
    private String labelNames;

    /**
     * 模板描述
     * */
    private String templateDesc;

    /**
     * 是否启用
     * */
    private Integer enabled;

    /**
     * 是否有效
     */
    private Boolean valid;

}

