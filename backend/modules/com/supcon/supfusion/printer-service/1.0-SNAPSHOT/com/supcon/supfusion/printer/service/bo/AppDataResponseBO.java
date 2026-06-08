package com.supcon.supfusion.printer.service.bo;

import com.supcon.supfusion.framework.cloud.common.result.Result;
import lombok.*;

import java.util.List;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class AppDataResponseBO {
    private Long code;

    private String msg;

    private Result<List<PageDataBO>> data;
}
