package com.supcon.supfusion.signature.services.utils.callback;

import com.supcon.supfusion.signature.dao.entity.SignatureLog;

import java.util.List;

/**
 * @author zhang yafei
 */
public interface ExcelCallback {
    List<SignatureLog> getData(int pageIndex);
}
