package com.supcon.supfusion.systemcode.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class SystemCodeDetailDTO extends SystemCodeResultDTO {

	private static final long serialVersionUID = -784671506270949065L;

	/**
	 * 编码值子节点
	 */
	private List<SystemCodeDetailDTO> children;

	/**
	 * 提供给baseService前端服务使用
	 */
	private List<SystemCodeDetailDTO> children2 = new ArrayList<>();
}
