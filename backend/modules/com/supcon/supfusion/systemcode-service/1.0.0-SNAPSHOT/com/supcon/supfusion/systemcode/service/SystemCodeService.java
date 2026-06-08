package com.supcon.supfusion.systemcode.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.systemcode.dao.po.SystemCodeDetailPO;
import com.supcon.supfusion.systemcode.dao.po.SystemCodePO;
import com.supcon.supfusion.systemcode.dao.po.SystemCodeSortPO;
import com.supcon.supfusion.systemcode.dao.po.SystemEntityDetailPO;
import com.supcon.supfusion.systemcode.dao.po.SystemEntityPO;

import java.util.List;
import java.util.Map;

/**
 * 数据字典,某一类下具体的编码管理本地调用服务接口
 * @author
 * @date 20-5-11 下午14:30
 */
public interface SystemCodeService extends IService<SystemCodePO>{
    /**
     * 新增编码值数据
     * @param systemCodePo
     */
    void addValue(SystemCodePO systemCodePo);

    /**
     * 新增编码值数据
     * RPC调用
     * @param systemCodePo
     */
    void addValueForRpc(SystemCodePO systemCodePo);

    /**
     * 查询指定系统编码的编码值数据(列表形式)
     * @param entityCode 系统编码
     * @param keyword 模糊查询关键字
     * @param page 翻页的页数
     * @param perPage 每页返回的元素数量
     * @return
     */
    PageResult<SystemCodePO> queryValueList(String entityCode, String keyword, Integer page, Integer perPage);

    /**
     * 查询编码值列表数据,不带分页信息
     * @param entityCode
     * @param keyword
     * @return
     */
    List<SystemCodePO> queryValueListNoPage(String entityCode, String keyword, String displayName, String code);

    /**
     * 查询编码值列表数据,供APP使用
     * @param entityCode
     * @return
     */
    List<SystemCodePO> queryValueListByApp(String entityCode);

    /**
     * 查询指定系统编码的编码值数据(树形形式)
     * @param systemEntityDetailPO
     */
    void queryValueTree(SystemEntityDetailPO systemEntityDetailPO);
    
    /**
     * 修改指定编码值数据
     * @param systemCodePo 系统编码内容
     */
    void updateValue(SystemCodePO systemCodePo);

    /**
     * 删除指定编码值数据
     * @param entityCode 系统字典项编码
     * @param code 系统编码
     */
    void deleteValue(String entityCode, String code);

    /**
     * 批量删除编码值数据
     * @param entityCode
     * @param list
     */
    void batchDeleteValues(String entityCode, List<String> list);

    /**
     * 查询指定系统编码的编码值的字节点数据(列表形式)
     * @param parentId 
     * @param keyword 模糊查询关键字
     * @param current 翻页的页数
     * @param pageSize 每页返回的元素数量
     * @return
     */
	PageResult<SystemCodePO> queryValueNodes(String entityCode, Long parentId, String keyword, Integer current, Integer pageSize);

	/**
	 * 通过id查询指定编码值的数据
	 * @param id
	 * @return
	 */
	SystemCodePO queryValueById(Long id);

    /**
     * 通过值的编码查询指定编码值的数据
     * @param code
     * @return
     */
    SystemCodePO queryCodeValueByCode(String code);

    /**
     * 通过系统编码的编码和值的编码查询指定编码值的数据
     * @param entityCode
     * @param code
     * @return
     */
	SystemCodePO queryCodeValueByCode(String entityCode, String code);

    SystemCodePO queryAllCodeValueByCode(String entityCode, String code);

    /**
     * 校验编码值是否存在
     * @param entityCode
     * @param code
     * @return
     */
    boolean validateCodeValueExist(String entityCode, String code);

    /**
     * 修改编码值的顺序
     * @param systemCodeSortPO
     */
    void modifyValueSort(SystemCodeSortPO systemCodeSortPO);

    /**
     * 通过系统字典项编码删除编码值数据
     * @param entityCode
     */
    void deleteValueByEntityCode(String entityCode);

    /**
     * 提供给baseService前端服务使用
     * @param systemEntityCode
     * @param id
     */
    List<SystemCodeDetailPO> queryCodeValueBaseTree(String systemEntityCode, String id);

    String queryDisplayName(Object object);

    /**
     * 通过系统编码实体Code和公司编码 查询对应语言系统编码键值MAP(有国际化)
     */
    Map<String, String> getSystemCodeList(String companyCode, String entityCode, Boolean senior);

    /**
     * 查询系统编码值列表数据,包含指定编码数据(LCDP使用)
     * @param entityCode 系统编码
     * @param code 值编码
     * @param page 翻页的页数
     * @param perPage 每页返回的元素数量
     * @return
     */
    Map<String, Object> queryEntityValueList(String entityCode, String code, Integer page, Integer perPage);

    /**
     * 通过系统编码批量查询编码列表
     * @param entityCodeList
     * @return
     */
    List<SystemCodePO> queryValueListByEntityCodes(List<String> entityCodeList);
}
