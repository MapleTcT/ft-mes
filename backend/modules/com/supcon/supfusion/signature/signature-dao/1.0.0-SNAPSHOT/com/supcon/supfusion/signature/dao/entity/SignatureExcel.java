package com.supcon.supfusion.signature.dao.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.supcon.supfusion.framework.scaffold.mybatis.type.handler.UTCToStringTypeHandler;
import lombok.Data;
import org.apache.ibatis.type.JdbcType;

import java.io.Serializable;
import java.util.Date;

/**
 * @author zhang yafei
 */
@Data
@TableName(value = "signature_excel", autoResultMap=true)
public class SignatureExcel implements Serializable {
    private static final long serialVersionUID = 8718334521164277822L;

    //Excel导入记录id
    @TableId
    private Long id;
    //导入 导出状态,1进行中, 2成功, 3失败
    private Integer status;
    //导入 导出 文件名
    private String fileName;
    //错误消息
    private String errorMessage;
    //类型,import 或 export
    private String operateType;
    //是否删除  0不使用 1 使用
    @TableLogic(
            value = "1",
            delval = "0"
    )
    private Integer valid;
//    //创建人
    private String creator;
//    //创建时间
    private Date createTime;
    //修改人
    private String modifier;
    //更新时间
    private Date modifyTime;

}
