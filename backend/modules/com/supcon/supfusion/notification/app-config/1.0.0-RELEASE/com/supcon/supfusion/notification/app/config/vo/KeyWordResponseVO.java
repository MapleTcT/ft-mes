package com.supcon.supfusion.notification.app.config.vo;

import lombok.Data;

import java.util.List;

@Data
public class KeyWordResponseVO {
    /**
     * 主题关联的模板关键字
     */
    private List<KeyWordVO> keyWordVOS;
}
