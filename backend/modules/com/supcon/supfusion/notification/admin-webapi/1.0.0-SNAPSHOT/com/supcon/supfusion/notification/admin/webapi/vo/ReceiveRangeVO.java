package com.supcon.supfusion.notification.admin.webapi.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ReceiveRangeVO {
    private String id;
    private String code;
    private String name;

    public ReceiveRangeVO(Long id, String code, String name) {
        this.code = code;
        this.name = name;
        this.id = id != null ? id.toString() : null;
    }
}
