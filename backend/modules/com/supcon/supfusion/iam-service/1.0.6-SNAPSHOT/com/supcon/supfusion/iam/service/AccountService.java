package com.supcon.supfusion.iam.service;

import com.supcon.supfusion.iam.dao.entity.AccountPO;
import com.supcon.supfusion.iam.service.bo.AccountBO;

import java.util.List;

/**
 * @author tomcat - <huangjianbo@supos.com>
 * @date 20-5-11 下午3:56
 */
public interface AccountService {

    /**
     * 申请
     *
     * @param username
     * @param description
     * @return
     */
    AccountBO create(String username, String description);
    AccountBO create(String username, String description, boolean isApp);

    /**
     * 注销
     *
     * @param username
     */
    void destroy(String username);
    void destroy(List<Long> ids);

    /**
     * 更新备注
     *
     * @param id
     * @param description
     */
    void update(Long id, String description);

    /**
     * 查询
     *
     * @param username
     * @return
     */
    List<AccountBO> find(String username);

    /**
     * 根据AK/SK查询
     *
     * @param ak AccessKey
     * @param sk SecretKey
     * @return
     */
    AccountBO findByAkAndSk(String ak, String sk);

    /**
     * 通过AK查询
     *
     * @param ak
     * @return
     */
    AccountBO findByAk(String ak);

    /**
     * 查询所有
     *
     * @return
     */
    List<AccountPO> findAll();
}
