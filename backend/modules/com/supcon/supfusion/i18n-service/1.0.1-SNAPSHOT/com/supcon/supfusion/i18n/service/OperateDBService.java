package com.supcon.supfusion.i18n.service;

import com.supcon.supfusion.i18n.bo.UploadResourceBO;
import com.supcon.supfusion.i18n.dao.po.I18nResourcePO;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface OperateDBService {
    /**
     * properties 资源文件路径 传入 将资源存入数据库
     */
    String readPropertiesToDB(String moduleCode, String newVersionCode, String destDir);

    /**
     * properties 资源文件路径 传入 将资源存入数据库 单个文件入库
     */
    String readPropertiesToDBAndCacheOneFile(UploadResourceBO uploadResource);

    /**
     *  工程期 properties 资源文件路径 传入 将资源存入数据库
     *
     */
    String readPropertiesToDBAndCacheFiles(UploadResourceBO uploadResourceBO);

    void saveListToDB(List<I18nResourcePO> tenantResources) throws InterruptedException;

    /**
     * 针对不同的数据库 批量插入时 参数集大小进行不同处理
     * mariadb
     * mysql  参数长度限制 mysql 参数限制 打开
     * oracle   修改每次批量插入的参数个数
     * sqlserver 修改每次批量插入的参数个数
     */
    boolean saveBatch(List<I18nResourcePO> list1);

    /**
     * 针对不同的数据库 批量删除时 参数集大小进行不同处理
     * mariadb
     * mysql
     * oracle
     * sqlserver
     */
    void delete(Set<String> i18nModuleSet);
    /**
     * 内部方法
     * @param i18n_value
     * @param moduleId
     * @param i18nKey
     * @param tenantId
     * @return
     */
    String createI18nResourceInternal(Map i18n_value, String moduleId, String i18nKey, String tenantId, List<I18nResourcePO> i18nResourcePOs);
    /**
     * 更新模块索引
     */
    String updateModuleIndexCode(String moduleCode);

    /**
     * 清楚某个模块的所有数据库信息
     */
    void deleteOneModuleAllResourceAndVersionAndIndex(String moduleCode);


    Map<String, List<I18nResourcePO>> selectByKey(List<String> i18nKys);

    List<String> selectKeysByKeys(Map<String, Object> whereMap,Boolean downAll);

    /**
     *  新增某个模块一批国际化 key value
     * @param language
     * @param moduleCode
     * @param map
     */
    List<I18nResourcePO> addOrUpdateList(String language, String moduleCode, Map<String, String> map);

    /**
     * 针对不同的数据库 批量删除时 参数集大小进行不同处理 根据语言 key集合删除
     * mariadb
     * mysql
     * oracle
     * sqlserver
     */
    void deleteListByKeyAndLanguage(String language,List<String> keyList,String moduleCode);
}
