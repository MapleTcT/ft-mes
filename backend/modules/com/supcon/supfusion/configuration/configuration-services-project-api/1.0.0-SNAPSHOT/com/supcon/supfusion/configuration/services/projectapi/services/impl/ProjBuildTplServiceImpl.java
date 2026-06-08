package com.supcon.supfusion.configuration.services.projectapi.services.impl;

import com.supcon.supfusion.configuration.services.projectapi.services.ProjBuildTplService;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.Map;

/**
 * @Description:
 * @Version
 * @Auther: xiakaili
 * @Date: 2021/2/23
 */
@Service("projBuildTplService")
public class ProjBuildTplServiceImpl implements ProjBuildTplService,InitializingBean {

    private Configuration cfg;
    private Object lockObj = new Object();

    @Override
    public void buildTpl(String tpl, String basepath, String path,
                         Map<String, Object> map) {
        try {
            File dir = new File(basepath);
            // 多线程同时生成，避免创建文件夹时冲突，此处只能同步单线程运行
            synchronized (lockObj) {
                if (!dir.exists()) {
                    if (!dir.mkdirs()) {
                        throw new Exception("can not make template's dir.");
                    }
                }
            }
            Writer writer = buildWriter(basepath + File.separator + path);
            // logger.debug("准备生成 文件至:" + basepath + File.separator + path);
            renderFtl(tpl, writer, map);
            writer.close();
            // logger.debug("生成 文件至:" + basepath + File.separator + path);
        } catch (Exception e) {
            try {
                throw new Exception(e);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }

    private void renderFtl(String ftl, Writer writer, Map<String, Object> map) throws Exception {

        try {
            Template template = cfg.getTemplate(ftl);
            template.process(map, writer);
        } catch (IOException e) {
            throw new Exception(e);
        } catch (TemplateException e) {
            throw new Exception(e);
        }
    }

    private Writer buildWriter(String path) throws UnsupportedEncodingException, FileNotFoundException {
        Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(path)), "UTF-8"));
        return writer;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        cfg = new Configuration();
        cfg.setClassForTemplateLoading(this.getClass(), "template");
        /*if (PropertyHolder.isDebugMode()) {
            cfg.setClassForTemplateLoading(this.getClass(), "template");
        } else {
            File f = new File(PropertyHolder.get().getWorkspacePath(), "module-template");
            if (!f.exists())
                f.mkdirs();
            cfg.setDirectoryForTemplateLoading(f);
        }*/
        cfg.setObjectWrapper(new DefaultObjectWrapper());
        cfg.setDefaultEncoding("UTF-8");
        cfg.setOutputEncoding("UTF-8");
        cfg.setSetting("number_format", "#");
    }

}
