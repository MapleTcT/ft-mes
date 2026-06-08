package com.supcon.supfusion.auditlog.dao.po.excel;


import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Excel导入记录类
 */
@Document
@Data
public class ExcelPO {
    /**
     * Excel导入记录id
     */
    @Id
    private String id;

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
    private String type;
}
