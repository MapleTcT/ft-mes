package com.supcon.supfusion.file.server.api.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class DocumentQueryDTO implements Serializable {
    private static final long serialVersionUID = 7097916839851820036L;
    List<Long> linkIds;
    String type;
    String fileType;
    String propertyCode;
}
