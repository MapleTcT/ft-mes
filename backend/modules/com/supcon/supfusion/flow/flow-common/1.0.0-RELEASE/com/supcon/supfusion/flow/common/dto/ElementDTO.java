package com.supcon.supfusion.flow.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
@AllArgsConstructor
public class ElementDTO {

    private boolean reject;

    private String targetId;
}
