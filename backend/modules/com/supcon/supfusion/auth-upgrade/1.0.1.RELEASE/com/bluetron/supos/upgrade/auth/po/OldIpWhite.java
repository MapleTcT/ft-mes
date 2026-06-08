package com.bluetron.supos.upgrade.auth.po;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;

@Data
@Entity
public class OldIpWhite {

    @Column(name = "id")
    private Long id;

    @Column(name = "IP")
    private String IP;

    @Column(name = "creationTime")
    private Long creationTime;

    @Column(name = "createdBy")
    private String createdBy;

    @Column(name = "type")
    private String type;

    @Column(name = "cid")
    private Long cid;

}
