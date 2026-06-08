package com.bluetron.supos.upgrade.auth.po;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;

@Data
@Entity
public class OldConfigDashboard {

    @Column(name = "userId")
    private Long userId;

    @Column(name = "mkey")
    private String mkey;

    @Column(name = "fields")
    private String fields;

    @Column(name = "configInfo")
    private String configInfo;
}
