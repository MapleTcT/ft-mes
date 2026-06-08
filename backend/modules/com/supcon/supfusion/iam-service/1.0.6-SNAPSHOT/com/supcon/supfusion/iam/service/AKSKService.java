package com.supcon.supfusion.iam.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supcon.supfusion.iam.common.bean.Order;
import com.supcon.supfusion.iam.dao.entity.AccountPO;
import com.supcon.supfusion.iam.service.bo.AccountBO;

import java.util.List;

public interface AKSKService {
    Page<AccountPO> queryListByKeyword(String appId, Order order, Integer pageNum, Integer pageSize);

    long add(String appId, String description);

    void update(Long id, String description);

    void batchDelete(List<Long> id);

    AccountBO download(Long id);
}
