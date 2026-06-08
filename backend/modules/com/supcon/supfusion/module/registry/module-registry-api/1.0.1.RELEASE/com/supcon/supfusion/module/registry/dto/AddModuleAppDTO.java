/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.module.registry.dto;

import java.io.Serializable;
import java.util.List;

import javax.validation.constraints.NotNull;

import lombok.Data;


@Data
public class AddModuleAppDTO implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
     * appId
     */
    private String appId;
    
    @NotNull(message = "模块列表不能为空")
    private List<AddModuleDTO> modules;
}