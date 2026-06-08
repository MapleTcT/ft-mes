package com.supcon.supfusion.systemcode.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.systemcode.dao.po.SystemEntityPO;

import java.util.List;

/**
 * 数据字典,字典项系统编码管理本地调用服务接口
 * @author
 * @date 20-5-11 下午14:30
 */
public interface SystemEntityService extends IService<SystemEntityPO> {

    PageResult<SystemEntityPO> queryEntities(String keyword, String moduleId, int current, int pageSize);

    SystemEntityPO queryEntityByCode(String code);

    boolean validateEntityExist(String code);

    SystemEntityPO queryEntityById(Long id);

    void addEntity(SystemEntityPO systemEntityPO);

    /**
     * 新增Entity
     * RPC调用
     * @param systemEntityPO
     */
    void addEntityForRpc(SystemEntityPO systemEntityPO);

    void updateEntity(SystemEntityPO systemEntityPO);

    void deleteEntityByCode(String code);

    void batchDeleteEntities(List<String> codeList);

    void deleteEntityByModuleId(String moduleId);

    List<SystemEntityPO> getEntityByModuleIds(List<String> moduleIds);

    List<SystemEntityPO> getEntityByModuleId(String moduleId);

    PageResult<SystemEntityPO> queryEntitiesByModuleIds(List<String> moduleIdList, int current, int pageSize);

}
