package com.supcon.supfusion.auth.webapi.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;


/**
 * 用户目录查询参数模型
 *
 * @author caokele
 */
@Data
@ApiModel("用户目录查询参数模型")
public class UserDirectoryQueryVO {

    @ApiModelProperty(value = "用户目录名称，支持模糊搜索", example = "LDAP服务器-生态技术部")
    private String directoryName;

    @ApiModelProperty(value = "用户目录类型", example = "ldap")
    private String directoryType;

    @ApiModelProperty(value = "用户目录名称筛选", example = "[\"生态技术部1\",\"生态技术部2\"]")
    private List<String> screenDirectoryNames;

    @ApiModelProperty(value = "用户目录类型筛选", example = "[\"ldap\",\"msad\"]")
    private List<String> screenDirectoryTypes;
}
