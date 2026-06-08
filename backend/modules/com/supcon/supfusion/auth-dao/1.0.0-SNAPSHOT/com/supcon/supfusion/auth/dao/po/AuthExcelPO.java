package com.supcon.supfusion.auth.dao.po;

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
@TableName(value = AuthExcelPO.TABLE_NAME, autoResultMap = true)
public class AuthExcelPO extends BaseEntity {

    public static final String TABLE_NAME = "auth_excel";

    /**
     * Excel导入记录id
     */
    private Long id;

    /**
     * 导入状态,1进行中, 2成功, 3失败
     */
    private Integer status;

    /**
     * 文件名
     */
    private String fileName;


    /**
     * 错误消息
     */
    private String errorMessage;

    /**
     * 成功数目
     */
    private Integer addNum;

    /**
     * 失败数目
     */
    private Integer updateNum;

    /**
     * 类型,import 或 export
     */
    @TableField(value = "excel_type")
    private String type;


}
