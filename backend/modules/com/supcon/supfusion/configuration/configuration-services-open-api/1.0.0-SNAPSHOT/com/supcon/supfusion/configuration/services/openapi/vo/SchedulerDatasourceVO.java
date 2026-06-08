package com.supcon.supfusion.configuration.services.openapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(value = "SchedulerDatasourceVO", description = "SchedulerDatasourceVO")
public class SchedulerDatasourceVO extends VO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String code;
    private int version;
    private String moduleCode; // 模块code
    private String datasourceAddress; // 数据库地址
    private String datasourceName; // 数据库名称
    private String datasourceType; // 数据库类型
    private String name; // 数据源名称
    private String password; // 数据库密码
    private String port; // 数据库端口
    private String username; //数据库用户名

}
