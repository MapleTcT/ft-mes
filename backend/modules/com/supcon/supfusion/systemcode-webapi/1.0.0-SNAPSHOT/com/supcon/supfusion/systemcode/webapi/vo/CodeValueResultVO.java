package com.supcon.supfusion.systemcode.webapi.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CodeValueResultVO {

    private static final long serialVersionUID = -1006794692304328661L;

    private List<CodeValueBaseVO> children2;
}
