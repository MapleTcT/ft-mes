package com.supcon.supfusion.rbac.webapi.vo.menuInfoCompany;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.util.List;

@Data
@ApiModel(description= "APP公司关联请求类")
public class AppCompanyRefVO extends VO {

    private static final long serialVersionUID = 4830865249640420872L;

    /**
     * appId
     */
    private String appId;

    /**
     * 公司ID集合
     */
    private List<String> cidList;
}
