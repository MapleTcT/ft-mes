package com.supcon.supfusion.base.services.impl;

import com.supcon.supfusion.base.entities.International;
import com.supcon.supfusion.base.entities.Language;
import com.supcon.supfusion.base.services.InternationalService;
import com.supcon.supfusion.base.services.ModuleRegistryService;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.i18n.resource.utils.MessageResourceWrapper;
import com.supcon.supfusion.framework.scaffold.hibernate.page.Page;
import com.supcon.supfusion.i18n.service.api.MessageResourceService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import java.io.*;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
public class InternationalServiceImpl implements InternationalService {

    @Autowired
    private MessageResourceWrapper messageResourceWrapper;
    @Autowired
    private MessageResourceService messageResourceService;
    @Autowired
    private ModuleRegistryService moduleRegistryService;

    private final static Map<String, Locale> localeMap = new HashMap();

    static {
        localeMap.put("zh_CN", Locale.SIMPLIFIED_CHINESE);
        localeMap.put("zh_HK", new Locale("zh", "HK"));
        localeMap.put("zh_TW", Locale.TRADITIONAL_CHINESE);
        localeMap.put("en_US", Locale.US);
    }

    @Override
    public String getI18nValue(String key) {
        return this.getI18nValue(key, null, null);
    }

    @Override
    public String getI18nValue(String key, Object[] args) {
        return this.getI18nValue(key, args, getLocale());
    }

    @Override
    public String getI18nValue(String key, Object[] args, String language) {
        if (StringUtils.isEmpty(key)) {
            return key;
        }
        if (StringUtils.isEmpty(language)) {
            language = Optional.ofNullable(RpcContext.getContext().getLanguage())
                    .map(Locale::toString)
                    .orElse(getLocale());
        }
        return messageResourceWrapper.getMessageNotBlankWithArgument(key, args, localeMap.get(language));
    }

    @Override
    public List<String> getI18nKey(String value) {
        List list = new ArrayList();
        Map<String, String> stringStringMap = messageResourceService.MessageResourceSearchOne(value, getLocale());
        for (String s : stringStringMap.values()) {
            if (value.equals(s)) {
                list.add(stringStringMap.get(s));
            }
        }
        return list;
    }

    @Override
    public void internationalPage(Page<Map<String, Object>> page, International example) {
        Map<String, Map<String, Map<String, String>>> appI18nResource = messageResourceService.getAllModuleAllLanguageResource();
        Map<String, Map<String, String>> allInternationalMap = new HashMap<>();
        Map<String, String> zhMap = new HashMap<>();
        Map<String, String> usMap = new HashMap<>();
        Map<String, String> hkMap = new HashMap<>();
        for (Map.Entry<String, Map<String, Map<String, String>>> stringMapEntry : appI18nResource.entrySet()) {
            Map<String, Map<String, String>> value = stringMapEntry.getValue();
            for (String s : value.keySet()) {
                if ("zh_CN".equals(s)) {
                    zhMap.putAll(value.get(s));
                } else if ("en_US".equals(s)) {
                    usMap.putAll(value.get(s));
                } else if ("zh_HK".equals(s)) {
                    hkMap.putAll(value.get(s));
                }
            }
        }
        allInternationalMap.put("zh_CN", zhMap);
        allInternationalMap.put("en_US", usMap);
        allInternationalMap.put("zh_HK", hkMap);
        if (null != allInternationalMap && !allInternationalMap.isEmpty()) {
            Map<String, Map<String, Object>> tempResult = new LinkedHashMap<String, Map<String, Object>>();
            List<Map<String, Object>> result = new LinkedList<Map<String, Object>>();
            Set<String> keySet = new HashSet<String>();
            for (Map.Entry<String, Map<String, String>> entry : allInternationalMap.entrySet()) {
                String language = entry.getKey();
                Map<String, String> map = entry.getValue();
                if (null != map && !map.isEmpty()) {
                    for (Map.Entry<String, String> entry2 : map.entrySet()) {
                        // 国际化key
                        String key = entry2.getKey();
                        keySet.add(key);
                        // 国际化值
                        String value = entry2.getValue();
                        if (null != example) {
                            if (null != example.getKey() && key.indexOf(example.getKey()) < 0) {
                                continue;
                            }
                            if (null != example.getValue() && value.indexOf(example.getValue()) < 0) {
                                continue;
                            }
                        }
                        Map<String, Object> map1 = tempResult.get(key);
                        if (null == map1) {
                            map1 = new LinkedHashMap<String, Object>();
                            map1.put("key", key);
                        }
                        map1.put(language, value);
                        tempResult.put(key, map1);
                    }
                }
            }
            //fix chandao-880 如果根据国际化值查询时遍历map的key将未匹配到的其他语言赋值到map中
            if (null != example && null != example.getValue()) {
                for (Map.Entry<String, Map<String, String>> entry : allInternationalMap.entrySet()) {
                    String language = entry.getKey();
                    // 获取指定语言下的所有国际化结果集
                    Map<String, String> map = entry.getValue();
                    //取得map中的key
                    if (null != map && !map.isEmpty()) {
                        for (String searchKey : tempResult.keySet()) {
                            //找到当前key已缓存的语言
                            List<String> currentLanguage = new ArrayList<>();
                            for (String searchLanguage : tempResult.get(searchKey).keySet()) {
                                if (!"key".equals(searchLanguage)) {
                                    currentLanguage.add(searchLanguage);
                                }
                            }
                            for (String searchLanguage : currentLanguage) {
                                if (language != searchLanguage) {
                                    // 其他语言国际化值
                                    String value = map.get(searchKey);
                                    Map<String, Object> map1 = tempResult.get(searchKey);
                                    if (null == map1) {
                                        map1 = new LinkedHashMap<String, Object>();
                                        map1.put("key", searchKey);
                                    }
                                    map1.put(language, value);
                                    tempResult.put(searchKey, map1);
                                }
                            }
                        }
                    }
                }
            }
            // 按key升序排序
            List<Map.Entry<String, Map<String, Object>>> tempList = new ArrayList<Map.Entry<String, Map<String, Object>>>(tempResult.entrySet());
            Collections.sort(tempList, new Comparator<Map.Entry<String, Map<String, Object>>>() {
                @Override
                public int compare(Map.Entry<String, Map<String, Object>> o1, Map.Entry<String, Map<String, Object>> o2) {
                    return o1.getKey().compareToIgnoreCase(o2.getKey());
                }
            });
            for (Map.Entry<String, Map<String, Object>> entry : tempList) {
                if (null != example && null != example.getLanguage()) {
                    if (null == entry.getValue().get(example.getLanguage())) {
                        continue;
                    }
                }
                result.add(entry.getValue());
            }
            int count = result.size();
            page.setTotalCount(count);
            if (count > 0) {
                int startIndex = page.getFirst() - 1;
                if (startIndex + page.getPageSize() > count) {
                    page.setResult(result.subList(startIndex, count));
                } else {
                    page.setResult(result.subList(startIndex, startIndex + page.getPageSize()));
                }
            }
        }
    }

    /**
     * 袁阳 2020.11.7
     * 更新国际化缓存
     */
    @Override
    public void refreshI18n() {
        messageResourceWrapper.initiativeRefreshCache();
    }

    @Override
    public String addInternational(String key) {
        if (key.indexOf("$&#") <= 0) {
            return key;
        }
        String[] values = key.split("\\$&#");
        if (!(values.length > 0 && values[0].startsWith("key="))) {
            return key;
        }
        Map i18nMap = new HashMap();
        for (int i = 1; i < values.length; i++) {
            if (values[i].indexOf("=") < 0 || values[i].endsWith("="))
                continue;
            String[] split = values[i].split("=");
            if (split.length == 1) {
                i18nMap.put(split[0], "");
            } else {
                i18nMap.put(split[0], split[1]);
            }
        }
        String realkey = values[0].substring(4);
        addInternational(realkey, i18nMap);
        return realkey;
    }

    @Override
    public List<International> getInternationals(String key) {
        Map<String, Map<String, Object>> allLanguage = messageResourceService.getAllLanguage();
        if (allLanguage == null || allLanguage.isEmpty()) {
            return null;
        }
        List<International> internationals = new ArrayList<>(allLanguage.keySet().size());
        allLanguage.forEach((language, value) -> {
            String i18nValue = messageResourceWrapper.getMessageWithArgument(key, null, localeMap.get(language));
            if (null == i18nValue) {
                i18nValue = "";
            }
            International international = new International(key, i18nValue, language);
            internationals.add(international);
        });
        return internationals;
    }

    @Override
    public void addInternational(String key, Map valueMap) {
        if (key.indexOf("\\.") > 0)
            return;
        String moduleCode = key.split("\\.")[0];
        try {
            Map map = new HashMap();
            map.put(International.I18N_KEY, key);
            map.put(International.MODULE_CODE, moduleCode);
            map.put(International.I18N_VALUE, valueMap);
            Result result = messageResourceService.messageResourceAddOrUpdateOne(map);
            //刷新国际化缓存
            messageResourceWrapper.initiativeRefreshCache();
            log.info(result.getMessage());
        } catch (RuntimeException e) {
            log.error("add i18n[" + key + "]error" + e.getMessage());
        }
    }

    @Override
    public String createNewInternational(String messageKey) {
        String newKey = "";
        String oldKey = "";
        if (null != messageKey && messageKey.trim().length() > 0) {
            String[] values = messageKey.split("\\$&#");
            for (String item : values) {
                if (item.startsWith("key=")) {
                    oldKey = item.substring(4);
                    break;
                }
            }
            if (oldKey.length() > 0) {
                int index = oldKey.lastIndexOf(".");
                if (index <= 0) {
                    newKey = oldKey + System.currentTimeMillis();
                } else {
                    newKey = oldKey.substring(0, index).concat(".randon").concat(System.currentTimeMillis() + "");
                }
                newKey = "key=" + newKey;
                for (String item : values) {
                    if (!item.startsWith("key=")) {
                        newKey += "$&#" + item;
                    }
                }
            } else {
                int index = messageKey.lastIndexOf(".");
                if (index <= 0) {
                    newKey = messageKey + System.currentTimeMillis();
                } else {
                    newKey = messageKey.substring(0, index).concat(".randon").concat(System.currentTimeMillis() + "");
                }
                Map map = new HashMap();
                for (Language language : getAllLanguage()) {
                    map.put(language.getKey(), getI18nValue(messageKey, null, language.getKey()));
                }
                this.addInternational(newKey, map);
            }
        }
        return newKey;
    }

    @Override
    public List<Language> getAllLanguage() {
        Map<String, Map<String, Object>> allLanguage = null;
        try {
            allLanguage = messageResourceService.getAllLanguage();
        } catch (RuntimeException exception) {
            log.warn(exception.getMessage(), exception);
        }
        if (allLanguage == null || allLanguage.size() == 0) {
            return getDefaultAllLanguage();
        }
        List<Language> list = new ArrayList<>(allLanguage.size());
        for (String s : allLanguage.keySet()) {
            Language language = new Language(s, true, s, String.valueOf(allLanguage.get(s).get("langu_name")));
            list.add(language);
        }
        return list;
    }

    @Override
    public void initInternational(String moduleCode, File i18nFiles[]) {
        if (i18nFiles == null || i18nFiles.length == 0) {
            return;
        }
        moduleCode = moduleCode.split("_")[0];
        Map i18nMap = new HashMap();
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
        i18nMap.put("moduleCode", moduleCode);
        i18nMap.put("moduleVersion", moduleCode + now.format(formatter));
        for (File i18nFile : i18nFiles) {
            if (i18nFile.getName().startsWith("reg_module")) {
                Map<String, Object> regPropertiesI18nMap = getRegPropertiesI18nMap(moduleCode, i18nFile);
                messageResourceService.messageResourceAddOrUpdateOne(regPropertiesI18nMap);
                continue;
            }
            //reg_category.properties  需要排除 不然这个也会被认为是当前上载模块的国际化资源上传
            if(i18nFile.getName().startsWith("reg_category")){
                continue;
            }
            if(i18nFile.getName().startsWith(moduleCode+"_") && i18nFile.getName().toUpperCase().endsWith(".PROPERTIES")){
                MultipartFile multipartFile = i18nFiles2MultipartFile(moduleCode, i18nFile);
                messageResourceService.messageResourceUploadProFile(multipartFile, i18nMap);
                log.info("------------------------------>上传国际化资源文件<-------------------------------:模块名：" + moduleCode);
            }
        }
    }
    @Override
    public void initProjInternational(String moduleCode, File i18nFiles) {
        moduleCode = moduleCode.split("_")[0];
        moduleRegistryService.registryModule(moduleCode);
        Map i18nMap = new HashMap();
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
        i18nMap.put("moduleCode", moduleCode);
        i18nMap.put("moduleVersion", moduleCode + now.format(formatter));
        MultipartFile multipartFile1 =getMultipartFile(i18nFiles,moduleCode+".zip");
        messageResourceService.messageResourceUploadCustomFile(multipartFile1, i18nMap);
                log.info("------------------------------>上传国际化资源文件<-------------------------------:模块名：" + moduleCode);
    }
    @Override
    public String initI18nKey(String moduleCode) {
        return messageResourceService.initI18nKey(moduleCode);
    }

    @Override
    public List<String> initI18nKeys(String moduleCode, Integer num) {
        return messageResourceService.initI18nKeys(moduleCode, num);
    }

    @Override
    public void messageResourceAddOrUpdateList(Map<String, String> map, String moduleCode, String language) {
        try {
            Result result = messageResourceService.messageResourceAddOrUpdateList(map, moduleCode, language);
            //刷新国际化缓存
            messageResourceWrapper.initiativeRefreshCache();
            log.info(result.getMessage());
        } catch (RuntimeException e) {
            log.error("add i18n error" + e.getMessage());
        }
    }

    private Map<String, Object> getRegPropertiesI18nMap(String moduleCode, File file) {
        Map map = new HashMap();
        map.put(International.I18N_KEY, "reg.moduleName." + moduleCode);
        map.put(International.MODULE_CODE, "reg");
        try {
            URL url = file.toURI().toURL();
            Properties properties = new Properties();
            BufferedReader bf = new BufferedReader(new InputStreamReader(url.openStream()));
            properties.load(bf);

            Map<String, String> keyValueMap = new HashMap(properties.size());
            for (Object key : properties.keySet()) {
                keyValueMap.put(String.valueOf(key), String.valueOf(properties.get(key)));
            }
            map.put(International.I18N_VALUE, keyValueMap);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return map;
    }

    private MultipartFile i18nFiles2MultipartFile(String moduleCode, File file) {
        String language = zh_CN.getKey();
        String filename = file.getName();
        if (filename.startsWith("package_")) {
            language = filename.split("package_")[1].split("\\.")[0];
        } else if (filename.startsWith(moduleCode)) {
            language = filename.substring(filename.length() - 16, filename.length() - 11);
        }
        if ("zh_TW".equals(language)) {
            language = "zh_HK";
        }
        String fileName = moduleCode + "_" + language + ".properties";
        MultipartFile multipartFile = getMultipartFile(file, fileName);
        return multipartFile;
    }

    private MultipartFile getMultipartFile(File file, String fileName) {
        final DiskFileItem item = new DiskFileItem("file", MediaType.MULTIPART_FORM_DATA_VALUE, true, fileName, 100000000, file.getParentFile());
        try {
            OutputStream os = item.getOutputStream();
            os.write(FileUtils.readFileToByteArray(file));
        } catch (IOException e) {
            log.error(e.getMessage(), e); // do nothing!
        }
        return new CommonsMultipartFile(item);
    }

    private final Language zh_CN = new Language("zh_CN", true, "zh_CN", "中文(简体)");
    private final Language zh_HK = new Language("zh_HK", true, "zh_HK", "中文(繁体)");
    private final Language en_US = new Language("en_US", true, "en_US", "英文(美国)");

    private List<Language> getDefaultAllLanguage() {
        return Arrays.asList(zh_CN, zh_HK, en_US);
    }

    @Override
    public String getLocale() {
        return "zh".equals(LocaleContextHolder.getLocale().toString())?"zh_CN":LocaleContextHolder.getLocale().toString();
    }
}
