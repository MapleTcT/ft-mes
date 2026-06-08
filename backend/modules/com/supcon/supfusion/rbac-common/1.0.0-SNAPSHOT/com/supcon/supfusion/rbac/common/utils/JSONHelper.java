package com.supcon.supfusion.rbac.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.ResourceUtils;

import java.io.*;

@Slf4j
public class JSONHelper {
    /**
     * 通过文件名获取获取json格式字符串，
     *
     * @param filename 文件存放路径与配置文件路径规范一致
     * @return ResolveJsonFileToString
     * @throws
     */
    public String ResolveJsonFileToString(String filename) throws IOException {
        BufferedReader br = null;
        String result = null;
//            br = new BufferedReader(new InputStreamReader(getInputStream(path)));
        br = new BufferedReader(new InputStreamReader(getResFileStream(filename), "UTF-8"));
        StringBuffer message = new StringBuffer();
        String line = null;
        while ((line = br.readLine()) != null) {
            message.append(line);
        }
        if (br != null) {
            br.close();
        }
        String defaultString = message.toString();
        result = defaultString.replace("\r\n", "").replaceAll(" +", "");
        return result;
    }


    private File getResFile(String filename) throws FileNotFoundException {
        File file = new File(filename);
        if (!file.exists()) { // 如果同级目录没有，则去config下面找
            log.debug("不在同级目录，进入config目录查找");
            file = new File("config/" + filename);
        }
        Resource resource = new FileSystemResource(file);
        if (!resource.exists()) { //config目录下还是找不到，那就直接用classpath下的
            log.debug("不在config目录，进入classpath目录查找");
            file = ResourceUtils.getFile("classpath:" + filename);
        }
        return file;
    }

    /**
     * 通过文件名获取classpath路径下的文件流
     *
     * @param
     * @return
     * @throws
     */
    private InputStream getResFileStream(String filename) throws IOException {
        FileInputStream fin = null;
        Resource fileRource = new ClassPathResource(filename);
//        File file = getResFile(filename);
//        log.info("getResFile path={}",file);
//        fin = new FileInputStream(file);
        return fileRource.getInputStream();
    }
}
