package com.supcon.supfusion.counter.sdk.bo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Set;

@Getter
@Setter
@ToString
public class CodeResult {
    private boolean withSequence;
    private Long batchId;
    private Set<String> codes;
}
