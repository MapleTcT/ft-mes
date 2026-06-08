package com.bluetron.supos.upgrade.auth.po;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;

@Data
@Entity
public class OldUser {

    @Column(name="userId")
    private Long userId;

    @Column(name = "username")
    private String username;

    @Column(name="password")
    private String password;

    @Column(name = "userDesc")
    private String userDesc;

    @Column(name = "uploadUrl")
    private String uploadUrl;

    @Column(name = "needChangePassword")
    private Boolean needChangePassword;

    @Column(name = "createTime")
    private String createTime;

    @Column(name = "updateTime")
    private String updateTime;

    @Column(name = "timeZone")
    private String timeZone;

    @Column(name = "lockStatus")
    private Boolean lockStatus;

    @Column(name = "accountType")
    private Integer accountType;

    @Column(name = "cide")
    private Long cid;

    @Column(name = "staffCode")
    private String staffCode;

    @Column(name = "staffName")
    private String staffName;

}
