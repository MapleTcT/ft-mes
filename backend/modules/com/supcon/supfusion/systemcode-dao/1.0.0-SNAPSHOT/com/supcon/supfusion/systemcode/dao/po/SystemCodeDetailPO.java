package com.supcon.supfusion.systemcode.dao.po;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class SystemCodeDetailPO extends SystemCodePO {

	private static final long serialVersionUID = -784671506270949065L;

	/**
	 * 编码值子节点
	 */
	private List<SystemCodeDetailPO> children;

	/**
	 * 提供给baseService前端服务使用
	 */
	private List<SystemCodeDetailPO> children2 = new ArrayList<>();
}
