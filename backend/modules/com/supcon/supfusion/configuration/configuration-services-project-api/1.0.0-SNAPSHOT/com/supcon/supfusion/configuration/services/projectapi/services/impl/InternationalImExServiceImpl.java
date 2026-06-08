package com.supcon.supfusion.configuration.services.projectapi.services.impl;

import com.supcon.supfusion.base.services.InternationalService;
import com.supcon.supfusion.base.utils.RuntimeFlagHolder;
import com.supcon.supfusion.configuration.services.entity.Module;
import com.supcon.supfusion.configuration.services.exceptions.EcException;
import com.supcon.supfusion.configuration.services.i18n.InternationalResource;
import com.supcon.supfusion.configuration.services.projectapi.services.ProjImportExportService;
import com.supcon.supfusion.configuration.services.service.ModelService;
import com.supcon.supfusion.configuration.services.utils.PropertyHolder;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import com.supcon.supfusion.i18n.service.api.MessageResourceService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Zip;
import org.apache.tools.ant.types.FileSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 *
 *
 * @author zhuwei2
 * @version $Id$
 */
@Slf4j
@Transactional
@ServiceApiService
public class InternationalImExServiceImpl implements ProjImportExportService {
    private static final String BAP_CUSTOM_L10N_FILE_PATH = PropertyHolder.get().getCustomL10nPath();
    private static final String DIR_NAME = "l10n";
    private static final String L10N_REGEX = "^\\w+\\.properties$";
    @Autowired
    private ModelService modelService;
    @Autowired
    private InternationalService internationalService;
    @Autowired
    private MessageResourceService messageResourceService;
    @Override
    @Transactional
    public void importProjConfig(String moduleCode, String path) {
        String dirPath=path+File.separator+DIR_NAME;
        RuntimeFlagHolder.getInstance().getRuntimeFlag().set(true);
        Module module=modelService.getModule(moduleCode);
        RuntimeFlagHolder.getInstance().getRuntimeFlag().set(false);
        File dir = new File(dirPath);
        if(dir.exists()){
            List<File> customFiles = new LinkedList<File>();
            com.supcon.supfusion.configuration.services.utils.FileUtils.listDirectory(dir, customFiles, L10N_REGEX);
            if(!customFiles.isEmpty()){
                File files[] = customFiles.toArray(new File[customFiles.size()]);
                File zipFile = compress(path);
                internationalService.initProjInternational(module.getCode(), zipFile);
            }
        }else{
            return;
        }
    }
    private File compress(String path){
        File f = new File(PropertyHolder.get().getProjPath() + File.separator +DIR_NAME+".zip");
        if (f.exists()) {
            f.delete();// delete if exist.
        }
        String zpath = path+File.separator+DIR_NAME;
        Zip zip = new Zip();
        Project p = new Project();
        zip.setProject(p);
        zip.setDestFile(f);
        FileSet fileSet = new FileSet();
        fileSet.setProject(p);
        File file = new File(zpath);
        fileSet.setDir(file);
        zip.add(fileSet);
        zip.execute();
        return zip.getDestFile();
    }
    @Override
    @Transactional
    public void exportProjConfig(String moduleCode, String path) {
        String dirPath=path+File.separator+DIR_NAME;
        File dir = new File(dirPath);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new EcException("can not make ProjConfig's dir.");
            }
        }
//        RuntimeFlagHolder.getInstance().getRuntimeFlag().set(true);
        Module module=modelService.getModule(moduleCode);
//        RuntimeFlagHolder.getInstance().getRuntimeFlag().set(false);
        File customRoot  = new File(BAP_CUSTOM_L10N_FILE_PATH + "/" + module.getArtifact());
        List<File> customFiles = new LinkedList<File>();
        internationOutExport(module, dirPath);
        com.supcon.supfusion.configuration.services.utils.FileUtils.listDirectory(customRoot, customFiles, L10N_REGEX);
        if(!customFiles.isEmpty()){
            for(File file : customFiles){
                try {
                    FileUtils.copyFileToDirectory(file, dir);
                } catch (IOException e) {
                    throw new EcException(e);
                }
            }
        }
    }
    @Transactional
    public void internationOutExport(Module module, String l10nPath) {
        String artifact = module.getArtifact();
        File l10nDir = new File(l10nPath);
        if (!l10nDir.exists() || !l10nDir.isDirectory()) {
            l10nDir.mkdirs();
        }
        // 删除以前的package文件
        List<File> srcFiles = new LinkedList<File>();
        com.supcon.supfusion.configuration.services.utils.FileUtils.listDirectory(l10nDir, srcFiles, "^package_\\w+\\.properties$");
        srcFiles.forEach(file -> file.delete());
        Map<String, Map<String, String>> i18nResourceMap = messageResourceService.downloadCustomFiles(artifact);
//        Map<String, Map<String, String>> i18nResourceMap = messageResourceService.downloadFiles(artifact);
        if (null != i18nResourceMap) {
            for(Map.Entry<String, Map<String, String>> i18nResource: i18nResourceMap.entrySet()){
                Map<String, String> keyValueMap = i18nResource.getValue();
                if (null != keyValueMap && !keyValueMap.isEmpty()) {
                    String fileName = artifact + "_" + i18nResource.getKey() + ".properties";
                    Properties properties = new Properties();
                    properties.putAll(keyValueMap);
                    BufferedWriter bw = null;
                    FileWriter fw = null;
                    try {
                        File i18nFile = new File(l10nPath + File.separator + fileName);
                        if (i18nFile.exists()) {
                            i18nFile.delete();
                        }
                        fw = new FileWriter(i18nFile);
                        bw = new BufferedWriter(fw);
                        properties.store(bw, null);
                    } catch (IOException e) {
                        log.error(e.getMessage(), e);
                    } finally {
                        try {
                            if (null != fw) {
                                fw.close();
                            }
                            if (null != bw) {
                                bw.close();
                            }
                        } catch (IOException e) {
                            log.error(e.getMessage(), e);
                        }
                    }
                }
            }
        }

        Properties properties = new Properties();
        Properties moduleCategoryProperties = new Properties();
        InternationalResource.getAllLanguages().forEach(language -> {
            String i18nValue = InternationalResource.get(module.getName(), language);
            if (null != module.getName() && !module.getName().equals(i18nValue)) {
                properties.put(language.toString(), i18nValue);
            }
            String i18nCategoryValue = InternationalResource.get(module.getCategory(), language);
            if (null != module.getCategory() && !module.getCategory().equals(i18nCategoryValue)) {
                moduleCategoryProperties.put(language.toString(), i18nCategoryValue);
            }
        });
    }
}
