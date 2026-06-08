package com.supcon.supfusion.counter.sdk.bo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class AllocCodeBO {
    private Long batchId;
    private List<String> codes;
}
