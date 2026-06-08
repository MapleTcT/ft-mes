/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.configuration.services.service;


import com.supcon.supfusion.configuration.services.entity.ButtonInfo;

/**
 * @author rockey
 * 
 */
public interface ButtonInfoService {

    ButtonInfo load(String code);

    void save(ButtonInfo entity);
}
