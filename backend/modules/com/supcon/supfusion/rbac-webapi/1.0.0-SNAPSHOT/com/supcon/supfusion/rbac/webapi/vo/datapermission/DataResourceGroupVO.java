package com.supcon.supfusion.rbac.webapi.vo.datapermission;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.Data;

@Data
public class DataResourceGroupVO extends VO {
    private String groupCode;
    private String groupName;
    private String resourceUrl;
}
