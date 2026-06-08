package com.supcon.supfusion.ws.client.dto;

import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@ToString
public class ResponseData {
    private List<Fail> fail;
}
