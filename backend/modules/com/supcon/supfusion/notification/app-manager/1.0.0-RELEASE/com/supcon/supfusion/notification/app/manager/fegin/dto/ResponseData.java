package com.supcon.supfusion.notification.app.manager.fegin.dto;

import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@ToString
public class ResponseData {
    private List<Fail> fail;
}
