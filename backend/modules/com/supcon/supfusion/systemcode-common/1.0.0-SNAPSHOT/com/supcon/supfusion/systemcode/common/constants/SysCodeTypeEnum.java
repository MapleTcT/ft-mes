package com.supcon.supfusion.systemcode.common.constants;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author lizheng
 * @date 2021/3/23 09:24
 * @description 系统编码类型枚举组件
 */
public enum SysCodeTypeEnum {

  APP(0, "app级"),
  SYSTEM(1, "系统级");


  private Integer type;

  private String desc;

  SysCodeTypeEnum(Integer type, String desc) {
    this.type = type;
    this.desc = desc;
  }

  public Integer getType() {
    return type;
  }

}
