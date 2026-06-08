package com.supcon.supfusion.organization.dao.po.excel;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import lombok.*;

/**
 * Excel导入记录类
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = ExcelPO.TABLE_NAME, autoResultMap = true)
public class ExcelPO extends BaseEntity {

    public static final String TABLE_NAME = "org_excel";

    /**
     * Excel导入记录id
     */
    private Long id;

    /**
     * 导入状态,1进行中, 2成功, 3失败
     */
    private Integer status;

    /**
     * 导入文件名
     */
    private String fileName;

    /**
     * 生成的错误文件名
     */
    private String errorFile;

    /**
     * 错误消息
     */
    private String errorMessage;

    /**
     * 类型,import 或 export
     */
    @TableField(value = "excel_type")
    private String type;


}
