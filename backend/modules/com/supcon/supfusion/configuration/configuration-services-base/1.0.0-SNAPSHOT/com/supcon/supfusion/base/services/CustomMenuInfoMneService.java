/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD.
 * All rights reserved.
 */
package com.supcon.supfusion.base.services;


import com.supcon.supfusion.base.entities.MenuInfoMneCodePO;
import com.supcon.supfusion.base.entities.MenuInfoPO;

import java.util.List;

public interface CustomMenuInfoMneService {

    void save(List<MenuInfoPO> menuInfoPOS);
}
