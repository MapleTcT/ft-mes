package com.supcon.supfusion.portal.dao.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName(value = RuntimeModulePO.TABLE_NAME)
public class RuntimeModulePO implements Serializable {

    public static final String TABLE_NAME = "runtime_module";
    private static final long serialVersionUID = -8731470022332493339L;

    @TableId
    private String code;
    private String name;
    private String category;//分类名
    private int valid;


}
