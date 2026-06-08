package com.supcon.supfusion.auth.openapi.suposvo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.supcon.supfusion.framework.cloud.common.exception.ErrorEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Collection;

@Setter
@Getter
@ToString(callSuper = true)
@JsonPropertyOrder({"code", "message", "pagination", "list"})
public class SuposPageResult<T> extends ErrorEntity {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Collection<T> list;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private SuposPagination pagination;


}
