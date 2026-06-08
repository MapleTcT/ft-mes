package com.supcon.supfusion.organization.service.bo.company;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.framework.scaffold.mybatis.type.handler.UTCToStringTypeHandler;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;
import org.apache.ibatis.type.JdbcType;

import java.util.List;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CompanyDetailInfoBO {

    private String code;

    private String parentCode;

    private String fullName;

    private String shortName;

    private String description;

    private List<String> tags;

    private String fullPath;

    private String layNo;

    private String sort;

    private Integer valid;

    private String modifyTime;
}
