package com.supcon.supfusion.custon.property.dao.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.custon.property.common.enums.EcEnv;
import com.supcon.supfusion.custon.property.dao.entity.base.LogicBasePO;
import lombok.Data;

@Data
@TableName(value = "runtime_data_grid",autoResultMap = true)
public class DataGrid extends LogicBasePO {

    private EcEnv ecEnv = EcEnv.product;
    private String moduleCode;
    private String entityCode;
    @TableId
    private String code;
    private static final long serialVersionUID = 5784761384089509802L;

    private String name;

    private String config;

    private String fullConfig;

    private Boolean ex;// 是否增强版

    private String dataGridName;

    private Integer dataGridType; // PT类型，默认是编辑pt 0， 列表PT 1

    private String permissionCode;

    private Boolean isPermission = false; // 启用

    private String operateName;// 列表pt权限名称

    private Boolean projFlag;

    private String dataGridJson;

    private String viewCode;

//    private View view;
//
//    private List<Field> fields;
//
//    private List<Button> buttons;
//
//    private Model targetModel;// 当前datagrid的关联模型
//
//    private Property orgProperty;// １对多关联中，多的一方关联１的关联字段

    public String getConfig() {
        if (config != null && config.length() > 0) {
            if (config.indexOf("<?xml version=\"1.0\" encoding=\"UTF-8\"?>") == -1) {
                config = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + config;
            }
        }
        return config;
    }

}