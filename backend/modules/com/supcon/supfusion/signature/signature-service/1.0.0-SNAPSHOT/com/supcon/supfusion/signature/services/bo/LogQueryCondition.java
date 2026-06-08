package com.supcon.supfusion.signature.services.bo;

import com.supcon.supfusion.signature.base.enums.SignatureColumn;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @author zhang yafei
 */
@Data
public class LogQueryCondition {

    private Map<SignatureColumn, List<String>> likeCondition;
    private Map<SignatureColumn, List<String>> inCondition;
    private Map<SignatureColumn, List<String>> timeCondition;

}
