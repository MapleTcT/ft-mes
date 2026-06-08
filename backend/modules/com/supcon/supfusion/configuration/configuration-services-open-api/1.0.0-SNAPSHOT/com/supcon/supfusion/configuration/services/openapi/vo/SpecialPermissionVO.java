package com.supcon.supfusion.configuration.services.openapi.vo;

import com.supcon.supfusion.configuration.services.entity.Property;
import com.supcon.supfusion.configuration.services.entity.View;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@ApiModel(value = "SpecialPermissionVO", description = "SpecialPermissionVO")
public class SpecialPermissionVO extends VO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String code;
    private int version;
    // 关联的模型编码
    private String modelCode;
    private String moduleCode;
    private String entityCode;
    // 等级
    private Integer rank;
    // 关系
    private String relation;
    // 类型
    private String type;
    private String targetModelCode;
    private Boolean isTree = false;
    @ManyToOne
    @JoinColumn(name = "REF_VIEW_CODE")
    @Fetch(FetchMode.SELECT)
    private View refView;
    private Property property;
    // 顺序
    private Integer orderNo;
    // 关联值,用于显示
    private String propertyName;
    private String associateName;
    private String associateType;
    private String associateCode;
    private String refViewUrl;
    private String targetModelName;
    private List<View> relateRefViews = new ArrayList<View>();

}
