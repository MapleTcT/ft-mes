package com.supcon.supfusion.i18n.dao.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

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

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final String TABLE_NAME = "supfusion_i18n_excel";

    //Excel导入记录id
    private Long id;
    //导入状态,1进行中, 2成功, 3失败
    private Integer status;
    //导入文件名
    private String fileName;
    //生成的错误文件名
    private String errorFile;
    //错误消息
    private String errorMessage;
    //类型,import 或 export
    private String operateType;
    //不符合要求的行数
    private Integer errorNum;
    //不符合要求的行数
    private Integer addNum;
    //不符合要求的行数
    private Integer updateNum;
    //总行数
    private Integer allNum;
    //是否删除  0不使用 1 使用
    private String valid;
    // 租户ID
    @TableField(fill = FieldFill.INSERT)
    private String tenantId;
    

}
