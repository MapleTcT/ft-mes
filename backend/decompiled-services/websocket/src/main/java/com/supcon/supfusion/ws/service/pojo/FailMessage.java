/*
 * Decompiled with CFR 0.152.
 */
package com.supcon.supfusion.ws.service.pojo;

public class FailMessage {
    private String userName;
    private String msg;

    public String getUserName() {
        return this.userName;
    }

    public String getMsg() {
        return this.msg;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof FailMessage)) {
            return false;
        }
        FailMessage other = (FailMessage)o;
        if (!other.canEqual(this)) {
            return false;
        }
        String this$userName = this.getUserName();
        String other$userName = other.getUserName();
        if (this$userName == null ? other$userName != null : !this$userName.equals(other$userName)) {
            return false;
        }
        String this$msg = this.getMsg();
        String other$msg = other.getMsg();
        return !(this$msg == null ? other$msg != null : !this$msg.equals(other$msg));
    }

    protected boolean canEqual(Object other) {
        return other instanceof FailMessage;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $userName = this.getUserName();
        result = result * 59 + ($userName == null ? 43 : $userName.hashCode());
        String $msg = this.getMsg();
        result = result * 59 + ($msg == null ? 43 : $msg.hashCode());
        return result;
    }

    public String toString() {
        return "FailMessage(userName=" + this.getUserName() + ", msg=" + this.getMsg() + ")";
    }
}

