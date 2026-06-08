package com.supcon.supfusion.auth.openapi.suposvo;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.*;

import java.io.Serializable;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonPropertyOrder({"total", "pageSize", "current"})
public class SuposPagination implements Serializable {
    private static final long serialVersionUID = -6986191490122868740L;

    /**
     * 总记录数
     */
    private long total = 0;
    /**
     * 页长
     */
    private long pageSize = 0;
    /**
     * 页码
     */
    private long pageIndex = 1;
}
