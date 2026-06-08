/*
 * Decompiled with CFR 0.152.
 */
package com.supcon.supfusion.ws.service.pojo;

import com.supcon.supfusion.ws.service.pojo.FailMessage;
import java.util.List;

public class FailMessages {
    private List<FailMessage> fail;

    public List<FailMessage> getFail() {
        return this.fail;
    }

    public void setFail(List<FailMessage> fail) {
        this.fail = fail;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof FailMessages)) {
            return false;
        }
        FailMessages other = (FailMessages)o;
        if (!other.canEqual(this)) {
            return false;
        }
        List<FailMessage> this$fail = this.getFail();
        List<FailMessage> other$fail = other.getFail();
        return !(this$fail == null ? other$fail != null : !((Object)this$fail).equals(other$fail));
    }

    protected boolean canEqual(Object other) {
        return other instanceof FailMessages;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        List<FailMessage> $fail = this.getFail();
        result = result * 59 + ($fail == null ? 43 : ((Object)$fail).hashCode());
        return result;
    }

    public String toString() {
        return "FailMessages(fail=" + this.getFail() + ")";
    }
}

