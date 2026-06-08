package com.supcon.supfusion.signature.services.utils.excel;

import lombok.Data;

@Data
public class ExcelHeaderDto {

    private String columnName;
    private String columnKey;
    private Integer rowNumber;
    private Integer rowSpan;
    private Integer columnNumber;
    private Integer columnSpan;
}
