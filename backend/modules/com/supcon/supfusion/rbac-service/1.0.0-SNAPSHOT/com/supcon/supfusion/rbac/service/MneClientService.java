package com.supcon.supfusion.rbac.service;

import com.supcon.supfusion.rbac.service.bo.MneQueryBO;

import java.util.List;
import java.util.Map;

public interface MneClientService {

    /**
     * 获取Service对应的Key
     *
     * @return
     */
    String getHandleType();

    /**
     * 返回助记码查询后的数据
     *
     * @param searchContent
     * @param params
     * @return
     */
    List<Map<String, Object>> search(MneQueryBO mneQueryBO, String condition);
}
