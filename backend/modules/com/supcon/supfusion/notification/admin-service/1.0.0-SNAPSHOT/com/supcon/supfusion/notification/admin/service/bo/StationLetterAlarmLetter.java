package com.supcon.supfusion.notification.admin.service.bo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

/**
 * ${description}
 *
 * @author huangxin2
 * @create 2020/5/22 11:12
 */
@Getter
@Setter
public class StationLetterAlarmLetter {
    private Boolean enable;
    private String linkPage;
    private Boolean showGuide;
    @JsonIgnore
    private String name="警铃消息";

}
