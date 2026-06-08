package com.supcon.supfusion.counter.sdk;

import com.supcon.supfusion.counter.sdk.bo.CodeResult;
import com.supcon.supfusion.counter.sdk.bo.RuleBO;
import com.supcon.supfusion.counter.sdk.bo.RuleParamBO;

import java.util.ArrayList;
import java.util.List;

public interface CodeGenerator extends ParamSupplier {
    CodeResult allocate(long ruleId, int applyCount, ParamSupplier supplier);

    void rollback(long batchId);
}
