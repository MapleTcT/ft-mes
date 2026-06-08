package com.supcon.supfusion.rbac.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.supcon.supfusion.rbac.dao.po.MenuOperateCodeUrlRefPO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * <p>
 * 菜单操作编码URL关联表 服务类
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-22
 */
public interface IMenuOperateCodeUrlRefService extends IService<MenuOperateCodeUrlRefPO> {

    void deleteByAppId(String appName);

    void updateUrl(String appName);

    void removeRedisUrl(List<MenuOperateCodeUrlRefPO> menuOperateCodeUrlRefPOS);

    void addRedisUrl(List<MenuOperateCodeUrlRefPO> menuOperateCodeUrlRefPOS);
}
