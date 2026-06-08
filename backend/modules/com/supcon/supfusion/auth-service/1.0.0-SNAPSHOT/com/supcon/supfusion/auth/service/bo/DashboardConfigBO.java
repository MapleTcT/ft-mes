package com.supcon.supfusion.auth.service.bo;

import lombok.Data;

@Data
public class DashboardConfigBO {
    private Long userId;
    private String mkey;
    private String fields;
    private String configInfo;
}
