package com.supcon.supfusion.printer.service.bo;

import lombok.*;

import java.util.List;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PrinterTemplateBatchUpdateBO {

    private Integer enabled;

    private List<Long> templateIds;
}
