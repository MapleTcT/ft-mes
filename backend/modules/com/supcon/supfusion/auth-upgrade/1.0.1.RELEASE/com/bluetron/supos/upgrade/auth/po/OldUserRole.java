package com.bluetron.supos.upgrade.auth.po;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;

@Data
@Entity
public class OldUserRole {

    @Column(name = "urid")
    private Long urid;

    @Column(name = "userId")
    private Long userId;

    @Column(name = "roleId")
    private Long roleId;
}
