package com.supcon.supfusion.counter.api.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Setter
@Getter
@ToString
public class AllocCodeDTO {
    private Long batchId;

    private List<String> codes;
}
