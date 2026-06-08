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
public class StationLetterShowWindow {
    private Boolean enable;
    private String width;
    private String heigth;
    @JsonIgnore
    private String name="模态窗口";
    private String title;
    private String linkPage;

}
