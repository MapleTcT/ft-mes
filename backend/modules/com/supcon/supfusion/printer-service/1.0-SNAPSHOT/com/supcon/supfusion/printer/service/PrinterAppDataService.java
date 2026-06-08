package com.supcon.supfusion.printer.service;

import com.supcon.supfusion.printer.service.bo.PageDataBO;
import com.supcon.supfusion.printer.service.bo.PageDataQueryBO;

import java.util.List;

/**
 * appdata service
 */
public interface PrinterAppDataService {

    /**
     * 获取app列表
     * @param pageDataQueryBO
     */
    List<PageDataBO> getAppList(PageDataQueryBO pageDataQueryBO);

    /**
     * 获取page列表
     * @param pageDataQueryBO
     */
    List<PageDataBO> getPageList(PageDataQueryBO pageDataQueryBO);
}
