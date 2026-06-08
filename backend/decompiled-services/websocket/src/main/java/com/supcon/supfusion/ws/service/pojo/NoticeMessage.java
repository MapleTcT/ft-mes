/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.alibaba.fastjson.JSONObject
 */
package com.supcon.supfusion.ws.service.pojo;

import com.alibaba.fastjson.JSONObject;
import java.io.Serializable;

public class NoticeMessage
implements Serializable {
    private String topic;
    private String userName;
    private JSONObject data;

    public String getTopic() {
        return this.topic;
    }

    public String getUserName() {
        return this.userName;
    }

    public JSONObject getData() {
        return this.data;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setData(JSONObject data) {
        this.data = data;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof NoticeMessage)) {
            return false;
        }
        NoticeMessage other = (NoticeMessage)o;
        if (!other.canEqual(this)) {
            return false;
        }
        String this$topic = this.getTopic();
        String other$topic = other.getTopic();
        if (this$topic == null ? other$topic != null : !this$topic.equals(other$topic)) {
            return false;
        }
        String this$userName = this.getUserName();
        String other$userName = other.getUserName();
        if (this$userName == null ? other$userName != null : !this$userName.equals(other$userName)) {
            return false;
        }
        JSONObject this$data = this.getData();
        JSONObject other$data = other.getData();
        return !(this$data == null ? other$data != null : !this$data.equals(other$data));
    }

    protected boolean canEqual(Object other) {
        return other instanceof NoticeMessage;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $topic = this.getTopic();
        result = result * 59 + ($topic == null ? 43 : $topic.hashCode());
        String $userName = this.getUserName();
        result = result * 59 + ($userName == null ? 43 : $userName.hashCode());
        JSONObject $data = this.getData();
        result = result * 59 + ($data == null ? 43 : $data.hashCode());
        return result;
    }

    public String toString() {
        return "NoticeMessage(topic=" + this.getTopic() + ", userName=" + this.getUserName() + ", data=" + this.getData() + ")";
    }
}

