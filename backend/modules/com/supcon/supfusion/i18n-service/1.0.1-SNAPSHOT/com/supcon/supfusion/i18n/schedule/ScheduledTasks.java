package com.supcon.supfusion.i18n.schedule;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import com.supcon.supfusion.framework.boot.scaffold.dbp.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.supcon.supfusion.i18n.common.config.I18nProperties;
import com.supcon.supfusion.i18n.common.until.Constants;
import com.supcon.supfusion.i18n.common.until.FilePathUtil;
import com.supcon.supfusion.i18n.common.until.MyFileUtils;
import com.supcon.supfusion.i18n.common.until.ResourcePropertiesWrapper;
import com.supcon.supfusion.i18n.dao.ExcelDao;
import com.supcon.supfusion.i18n.dao.I18nIndexDao;
import com.supcon.supfusion.i18n.dao.I18nResourceDao;
import com.supcon.supfusion.i18n.dao.I18nTokenDao;
import com.supcon.supfusion.i18n.dao.po.ExcelPO;
import com.supcon.supfusion.i18n.dao.po.I18nIndexPO;
import com.supcon.supfusion.i18n.dao.po.I18nResourcePO;
import com.supcon.supfusion.i18n.dao.po.I18nTokenPO;
import com.supcon.supfusion.i18n.dao.vo.I18nResourceVO;
import com.supcon.supfusion.module.registry.ModuleEnum;

import lombok.extern.slf4j.Slf4j;

/**
 * @Description:
 * @Author: ShenZhiqiang
 * @Date: Create in  19:36 2020/6/23
 * @Modified:
 */
@Component
@Slf4j
public class ScheduledTasks {
    /**
     * "0/5 * *  * * ?"   每5秒触发
     * "0 0 12 * * ?"    每天中午十二点触发
     * "0 15 10 ? * *"    每天早上10：15触发
     * "0 15 10 * * ?"    每天早上10：15触发
     * "0 15 10 * * ? *"    每天早上10：15触发
     * "0 15 10 * * ? 2005"    2005年的每天早上10：15触发
     * "0 * 14 * * ?"    每天从下午2点开始到2点59分每分钟一次触发
     * "0 0/5 14 * * ?"    每天从下午2点开始到2：55分结束每5分钟一次触发
     * "0 0/5 14,18 * * ?"    每天的下午2点至2：55和6点至6点55分两个时间段内每5分钟一次触发
     * "0 0-5 14 * * ?"    每天14:00至14:05每分钟一次触发
     * "0 10,44 14 ? 3 WED"    三月的每周三的14：10和14：44触发
     * "0 15 10 ? * MON-FRI"    每个周一、周二、周三、周四、周五的10：15触发
     */
    @Autowired
    private I18nTokenDao i18nTokenDao;
    @Autowired
    private ExcelDao excelDao;
    @Autowired
    private I18nProperties i18nProperties;
    @Autowired
    private I18nResourceDao i18nResourceDao;
    @Autowired
    private DataSourceConnectionProperties dataSourceConnectionProperties;
    @Autowired
    private I18nIndexDao i18nIndexDao;

    @Scheduled(cron = "0 0/2 *  * * ?")
    public void scheduledCronDeleteI18nTokenPO() throws ParseException {
        List<I18nTokenPO> i18nTokenPOS = i18nTokenDao.selectAllTokenPO();
        SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (I18nTokenPO i18nTokenPO : i18nTokenPOS) {
        	Date createTime = sf.parse(i18nTokenPO.getCreateTime());
            if (System.currentTimeMillis() - 60 * 2 * 1000 > createTime.getTime()) {
                //log.info("----->处理上传资源操作超过2分钟还未完成的token记录<------当前时间：" + System.currentTimeMillis());
                i18nTokenDao.deleteOneByModuleCodeAndToken(i18nTokenPO);
            }
        }
    }

    //定时任务-修改解析超过分钟还未完成的excelPO
    @Scheduled(cron = "0 0/15 *  *  * ?")
    public void scheduledCronDeleteExcelPO() throws ParseException {
        LambdaQueryWrapper<ExcelPO> queryWrapper = new QueryWrapper<ExcelPO>().lambda().eq(ExcelPO::getStatus, Constants.ONE_INT);
        List<ExcelPO> excelPOS = excelDao.selectList(queryWrapper);
        SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        for (ExcelPO excelPO : excelPOS) {
        	Date createTime = sf.parse(excelPO.getCreateTime());
            if (System.currentTimeMillis() - 15 * 60 * 1000 > createTime.getTime()) {
                //log.info("----->执行处理超过指定时间还未完成的excel导入任务<------导入文件记录id:" + excelPO.getId());
                excelPO.setStatus(3);
                excelPO.setErrorMessage("服务器解析文件超时，修改任务状态");
                excelPO.setValid(Constants.ZERO_STR);
                UpdateWrapper<ExcelPO> userUpdateWrapper = new UpdateWrapper<>();
                userUpdateWrapper.eq("id", excelPO.getId());
                excelDao.update(excelPO, userUpdateWrapper);
            }
        }
    }
    
    //定时将修改过的国际化key对应的键值对更新进文件
    @Scheduled(cron = "0/10 * *  *  * ?")
    public void scheduledCronUpdateResourceFile() {
        //log.info("----->执行处理 获取改变的国际化key的文件读取当前文件中所有的国际化key 同步文件任务<------");
        //获取改变的国际化key的文件读取当前文件中所有的国际化key
        MyFileUtils.createDir(FilePathUtil.getFilePath(i18nProperties) + Constants.EXCEL_FILE_IMPORT_UPDATE_PATH);
        File updateFile = new File(FilePathUtil.getFilePath(i18nProperties) + Constants.EXCEL_FILE_IMPORT_UPDATE_PATH);
        File[] fs = updateFile.listFiles();
        if (fs != null && fs.length > 0) {
            for (File f : fs) {
                List<String> keys = new ArrayList<>();
                if (f.isFile()) {
                    try (BufferedReader bf = new BufferedReader(new FileReader(f))) {
                        String str;
                        while (true) {
                            try {
                                if (!((str = bf.readLine()) != null)) break;
                                String key = str.replace(Constants.STR_POINT_DOU, Constants.STR_NO_SPACE);
                                System.out.println(key);
                                //log.info("----->读取当前文件中所有的国际化key:"+key);
                                keys.add(key);
                            } catch (IOException e) {
                                log.error(e.getMessage());
                            }
                        }
                    } catch (FileNotFoundException e) {
                        log.error(e.getMessage());
                    } catch (IOException e) {
                        log.error(e.getMessage());
                    }
                    //根据国际化key查询数据库
                    if (keys != null && keys.size() > 0) {
                        getI18nResourceByKey(keys);
                    }
                    if (!f.delete()) {
                        log.error(f.getName() + Constants.DELETE_ERROR);
                    }
                }
            }
        }

    }


    private void getI18nResourceByKey(List<String> keys) {
        List<I18nResourcePO> i18ns = new ArrayList<>();
        LambdaQueryWrapper<I18nResourcePO> query = new QueryWrapper<I18nResourcePO>().lambda()
        		.eq(I18nResourcePO::getTenantId, Constants.DEFAULT_TENANT)
        		.in(I18nResourcePO::getI18nKey, keys);
        String dbType = Optional.ofNullable(dataSourceConnectionProperties.getSystem())
                .map(SystemConnectionProperties::getDbType)
                .orElse(null);
        if (dbType != null && !dbType.equals(Constants.STR_NO_SPACE) && keys != null && keys.size() > 0) {
            if (dbType.equals(Constants.DB_TYPE_MARIADB)) {
                i18ns = i18nResourceDao.selectList(query);
            } else if (dbType.equals(Constants.DB_TYPE_MYSQL)) {
                i18ns = i18nResourceDao.selectList(query);
            } else if (dbType.equals(Constants.DB_TYPE_ORACLE)) {
                Integer num = 1000;
                if (keys.size() < num) {
                    i18ns = i18nResourceDao.selectList(query);
                } else {
                    Integer pageSize = 1;
                    if (keys.size() % num > 0) {
                        pageSize = keys.size() / num + 1;
                    } else {
                        pageSize = keys.size() / num;
                    }
                    List<I18nResourcePO> i18nResourcePOS = new ArrayList<>();
                    for (int q = 0; q < pageSize; q++) {
                        if (q == pageSize - 1) {
                        	query = new QueryWrapper<I18nResourcePO>().lambda()
                            		.eq(I18nResourcePO::getTenantId, Constants.DEFAULT_TENANT)
                            		.in(I18nResourcePO::getI18nKey, keys.subList(q * num, keys.size()));
                            i18nResourcePOS = i18nResourceDao.selectList(query);
                            i18ns.addAll(i18nResourcePOS);
                            i18nResourcePOS.clear();
                        } else {
                        	query = new QueryWrapper<I18nResourcePO>().lambda()
                            		.eq(I18nResourcePO::getTenantId, Constants.DEFAULT_TENANT)
                            		.in(I18nResourcePO::getI18nKey, keys.subList(q * num, (q + 1) * num));
                            i18nResourcePOS = i18nResourceDao.selectList(query);
                            i18ns.addAll(i18nResourcePOS);
                            i18nResourcePOS.clear();
                        }
                    }
                }
            } else if (dbType.equals(Constants.DB_TYPE_SQLSERVER)) {
                Integer num = 200;
                if (keys.size() < num) {
                    i18ns = i18nResourceDao.selectList(query);
                } else {
                    Integer pageSize = 1;
                    if (keys.size() % num > 0) {
                        pageSize = keys.size() / num + 1;
                    } else {
                        pageSize = keys.size() / num;
                    }
                    List<I18nResourcePO> i18nResourcePOS = new ArrayList<>();
                    for (int q = 0; q < pageSize; q++) {
                        if (q == pageSize - 1) {
                        	query = new QueryWrapper<I18nResourcePO>().lambda()
                            		.eq(I18nResourcePO::getTenantId, Constants.DEFAULT_TENANT)
                            		.in(I18nResourcePO::getI18nKey, keys.subList(q * num, keys.size()));
                            i18nResourcePOS = i18nResourceDao.selectList(query);
                            i18ns.addAll(i18nResourcePOS);
                            i18nResourcePOS.clear();
                        } else {
                        	query = new QueryWrapper<I18nResourcePO>().lambda()
                            		.eq(I18nResourcePO::getTenantId, Constants.DEFAULT_TENANT)
                            		.in(I18nResourcePO::getI18nKey, keys.subList(q * num, (q + 1) * num));
                            i18nResourcePOS = i18nResourceDao.selectList(query);
                            i18ns.addAll(i18nResourcePOS);
                            i18nResourcePOS.clear();
                        }
                    }
                }
            }
        } else {
            i18ns = i18nResourceDao.selectList(query);
        }
        //从自己的数据库查询所有的模块code
        List<I18nIndexPO> i18nIndexPOS = i18nIndexDao.queryAllModuleCode();
        Set<String> i18nModuleSet = new HashSet<>();
        i18nIndexPOS.forEach(i18nIndexPO -> {
            if (i18nIndexPO != null && i18nIndexPO.getModuleCode() != null) {
                i18nModuleSet.add(i18nIndexPO.getModuleCode());
            }
        });
        // deleteI18nResourceList 需要删除的VO列表
        List<I18nResourceVO> deleteI18nResourceList = new ArrayList<>();
        for (int i = 0; i < keys.size(); i++) {
            I18nResourceVO i18NResourceVO = new I18nResourceVO();
            i18NResourceVO.setI18nKey(keys.get(i));
            String moduleCode = keys.get(i).substring(0, keys.get(i).indexOf(Constants.STR_POINT));
            if (i18nModuleSet.contains(moduleCode)) {
                i18NResourceVO.setModuleCode(moduleCode);
            } else {
                i18NResourceVO.setModuleCode(ModuleEnum.DEFAULT.getModuleId());
            }
            deleteI18nResourceList.add(i18NResourceVO);
        }
        //一个key 多个value value map的方式存 i18n_resource_list数据库最新信息
        List<I18nResourceVO> addI18nResourceList = new ArrayList<>();
        for (int i = 0; i < keys.size(); i++) {
            I18nResourceVO i18NResourceVO = new I18nResourceVO();
            for (I18nResourcePO i18nEntity : i18ns) {
                if (i18nEntity.getI18nKey().equals(keys.get(i))) {
                    i18NResourceVO.setI18nKey(keys.get(i).toString());
                    i18NResourceVO.getI18nValues().put(i18nEntity.getLanguCode(), i18nEntity.getI18nValue());
                    i18NResourceVO.setModuleCode(i18nEntity.getModuleCode());
                }
            }
            addI18nResourceList.add(i18NResourceVO);
        }
        //根据国际化key 先删除
        //根据对应的环境开发或者生产环境 修改对应文件夹中的国际化键值对
        if (!i18nProperties.getProfile().equals(Constants.PRO_ENVIRO)) {
            //System.out.println("当前环境是productDev环境。");
            for (I18nResourceVO i18NResourceVO : deleteI18nResourceList) {
                String moduleDir = MyFileUtils.findDir(FilePathUtil.getFilePath(i18nProperties) + Constants.RESOURCE_FILE_PATH, i18NResourceVO.getModuleCode());
                deleteKeyValues(i18NResourceVO, moduleDir);
            }
            for (I18nResourceVO i18NResourceVO : addI18nResourceList) {
                String moduleDir = MyFileUtils.findDir(FilePathUtil.getFilePath(i18nProperties) + Constants.RESOURCE_FILE_PATH, i18NResourceVO.getModuleCode());
                addKeyValues(i18NResourceVO, moduleDir);
            }
        } else if (i18nProperties.getProfile().equals(Constants.PRO_ENVIRO)) {
            //System.out.println("当前环境是product环境。");
            for (I18nResourceVO i18NResourceVO : deleteI18nResourceList) {
                MyFileUtils.createDir(FilePathUtil.getFilePath(i18nProperties) + Constants.RESOURCE_FILE_CUSTOM_PATH);
                String moduleDir = MyFileUtils.findDir(FilePathUtil.getFilePath(i18nProperties) + Constants.RESOURCE_FILE_CUSTOM_PATH, i18NResourceVO.getModuleCode());
                deleteKeyValues(i18NResourceVO, moduleDir);
            }
            for (I18nResourceVO i18NResourceVO : addI18nResourceList) {
                MyFileUtils.createDir(FilePathUtil.getFilePath(i18nProperties) + Constants.RESOURCE_FILE_CUSTOM_PATH);
                String moduleDir = MyFileUtils.findDir(FilePathUtil.getFilePath(i18nProperties) + Constants.RESOURCE_FILE_CUSTOM_PATH, i18NResourceVO.getModuleCode());
                addKeyValues(i18NResourceVO, moduleDir);
            }
        }

    }

    private void deleteKeyValues(I18nResourceVO i18NResourceVO, String moduleDir) {
        File file = new File(moduleDir);
        //先删除当前国际化key 在对应文件中存储的国际化值
        if (file.isDirectory()) {
            File[] fileDirs = file.listFiles();
            if (fileDirs != null && fileDirs.length > 0) {
                for (File file1 : fileDirs) {
                    if (file1.isDirectory()) {
                    } else if (file1.isFile()) {
                        String fileName = file1.getName();
                        if (fileName.contains(Constants.STR_LINE)) {
                            ResourcePropertiesWrapper.removeProperty(file1.getAbsolutePath(), i18NResourceVO.getI18nKey());
                        }
                    }
                }
            }
        }
    }

    private void addKeyValues(I18nResourceVO i18NResourceVO, String moduleDir) {
        Map values = i18NResourceVO.getI18nValues();
        //循环判断当前的这个国际化主键下所有键值对 更新进文件
        for (Object languageCode : values.keySet()) {
            if (values.get(languageCode) != null) {
                updateLanguageProperties(i18NResourceVO.getI18nKey(), languageCode.toString(), values.get(languageCode).toString(), i18NResourceVO.getModuleCode(), moduleDir);
            }
        }
    }

    private void updateLanguageProperties(String i18nKey, String langu_code, String i18nValue, String moduleCode, String moduleDir) {
        File file = new File(moduleDir);
        //先删除当前国际化key 在对应文件中存储的国际化值
        if (file.isDirectory()) {
            File[] fileDirs = file.listFiles();
            Boolean hasFile = false;
            if (fileDirs != null && fileDirs.length > 0) {
                for (File file1 : fileDirs) {
                    if (file1.isDirectory()) {
                    } else if (file1.isFile()) {
                        String fileName = file1.getName();
                        if (fileName.contains(Constants.STR_LINE)) {
                            String langu_code1 = fileName.substring(fileName.indexOf(Constants.STR_LINE, Constants.ONE_INT) + 1);
                            String langu_code2 = langu_code1.substring(0, langu_code1.indexOf(Constants.STR_POINT, Constants.ONE_INT));
                            if (langu_code2.equals(langu_code)) {
                                ResourcePropertiesWrapper.writeToProperties(i18nKey, i18nValue, file1.getAbsolutePath());
                                hasFile = true;
                            }
                        }
                    }
                }
            }
            if (!hasFile) {
                //当前还有没有该语言类型的properties文件
                String languageFileStr = moduleDir + Constants.PATH + moduleCode + Constants.STR_LINE + langu_code + Constants.STR_POINT + Constants.PROPERTIES_LOW;
                File languageFile = new File(languageFileStr);
                try {
                    if (!languageFile.createNewFile()) {
                        log.error(languageFile.getName() + Constants.CREATE_ERROR);
                    }
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
                ResourcePropertiesWrapper.writeToProperties(i18nKey, i18nValue, languageFileStr);
            }
        }
    }


}