/*
 * Decompiled with CFR 0.152.
 */
package com.supcon.supos.suposgateway.feign.dto;

import java.io.Serializable;

public class AccountDTO
implements Serializable {
    private static final long serialVersionUID = 5084001264712683877L;
    private String ak;
    private String sk;

    public void setAk(String ak) {
        this.ak = ak;
    }

    public void setSk(String sk) {
        this.sk = sk;
    }

    public String getAk() {
        return this.ak;
    }

    public String getSk() {
        return this.sk;
    }
}

