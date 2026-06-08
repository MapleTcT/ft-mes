package com.supcon.supfusion.counter.sdk;

import com.supcon.supfusion.counter.sdk.bo.RuleBO;
import com.supcon.supfusion.counter.sdk.bo.RuleParamBO;

import java.util.List;

public interface ParamSupplier {
    List<RuleParamBO> fillAndGet(RuleBO ruleBO);
}
