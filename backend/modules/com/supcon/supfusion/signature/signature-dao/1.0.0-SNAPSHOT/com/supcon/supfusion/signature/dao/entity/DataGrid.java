package com.supcon.supfusion.signature.dao.entity;


import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.signature.dao.annotation.BAPInternational;
import com.supcon.supfusion.signature.dao.entity.base.LogicBasePO;
import com.supcon.supfusion.signature.dao.enums.EcEnv;
import lombok.Data;


@Data
@TableName("ec_data_grid")
public class DataGrid extends LogicBasePO {
    private static final long serialVersionUID = 5784761384089509802L;

    protected EcEnv ecEnv = EcEnv.product;

    @TableId
    private String code;
    private String moduleCode;

    private String entityCode;

    private String name;

    private String config;

    private String fullConfig;

    private Boolean ex;// 是否增强版

    @BAPInternational(replace = true)
    private String dataGridName;

    private Integer dataGridType; // PT类型，默认是编辑pt 0， 列表PT 1

    private String dataGridJson;

    private Boolean isPermission = false; // 启用

    private String permissionCode;

    @BAPInternational(fieldName = "operateNameInternational", replace = false)
    private String operateName;// 列表pt权限名称

    private Boolean projFlag;

    //@JsonIgnore
//    @ManyToOne
//    @JoinColumn(name = "TARGETMODEL_CODE", referencedColumnName = "code")
//    @Fetch(FetchMode.SELECT)
//    @NotFound(action=NotFoundAction.IGNORE)
    @TableField("targetmodel_code")
    private String targetModelCode;// 当前datagrid的关联模型


//    @OneToMany(mappedBy = "dataGrid")
//    @Where(clause = "valid = 1")
//    @OrderBy(clause = "code asc")
//    @Fetch(FetchMode.SELECT)
//    private List<Field> fields;


    //@JsonIgnore
//    @ManyToOne
//    @JoinColumn(name = "VIEW_CODE")
//    @Fetch(FetchMode.SELECT)
//    @Index(name = "idx_datagrid_view")
//    @NotFound(action=NotFoundAction.IGNORE)
    private String viewCode;

    //@JsonIgnore
//    @OneToMany(mappedBy = "dataGrid")
//    @Where(clause = "valid = 1")
//    @OrderBy(clause = "code asc")
//    @Fetch(FetchMode.SELECT)
//    private List<Button> buttons;

    //@JsonIgnore
//    @ManyToOne
//    @JoinColumn(name = "ORGPROPERTY_CODE", referencedColumnName = "code")
//    @Fetch(FetchMode.SELECT)
//    @NotFound(action=NotFoundAction.IGNORE)
    @TableField("orgproperty_code")
    private String orgPropertyCode;// １对多关联中，多的一方关联１的关联字段


    public String getConfig() {
        if (config != null && config.length() > 0) {
            if (config.indexOf("<?xml version=\"1.0\" encoding=\"UTF-8\"?>") == -1) {
                config = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + config;
            }
        }
        return config;
    }


}