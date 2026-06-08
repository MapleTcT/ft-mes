package com.supcon.supfusion.configuration.services.projectapi.services.impl;

import com.supcon.supfusion.base.utils.RuntimeFlagHolder;
import com.supcon.supfusion.configuration.services.entity.View;
import com.supcon.supfusion.configuration.services.exceptions.EcException;
import com.supcon.supfusion.configuration.services.i18n.InternationalResource;
import com.supcon.supfusion.configuration.services.projectapi.services.ProjImportExportAdmin;
import com.supcon.supfusion.configuration.services.projectapi.services.ProjImportExportService;
import com.supcon.supfusion.configuration.services.service.ModelService;
import com.supcon.supfusion.configuration.services.service.ViewService;
import com.supcon.supfusion.configuration.services.utils.PropertyHolder;
import com.supcon.supfusion.configuration.services.utils.SpringContextHolder;
import com.supcon.supfusion.configuration.services.utils.UnZipFile;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Zip;
import org.apache.tools.ant.types.FileSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @Description:
 * @Version
 * @Auther: xiakaili
 * @Date: 2021/2/23
 */
@Service("projImportExportAdmin")
@Transactional
@Slf4j
public class ProjImportExportAdminImpl implements ProjImportExportAdmin {
    private static final String ZIP_NAME = "ProjectConfig";
    private static final String[] SERVICE_NAME={"ModuleInfo","International","View","CustomProperty"};
    /*@Value("${entityconf.projPath}")
    private String projPath;*/
    private Map<String, ProjImportExportService> serviceMap=null;
    @Autowired
    private ViewService viewServiceFoundation;
    @Autowired
    private ModelService modelServiceFoundation;
    @Override
    @Transactional(timeout = -1, propagation = Propagation.REQUIRED)
    public void importProj(File configFlie, List<String> filter) {
        if(serviceMap==null){
            initServiceMap();
        }
        String uploaded = PropertyHolder.get().getProjPath() + File.separator + "uploaded" + File.separator + "up" + System.currentTimeMillis();
        File uploadedFile = new File(uploaded);
        if (!uploadedFile.exists()) {
            uploadedFile.mkdirs();
        }
        try {
            FileUtils.copyFileToDirectory(configFlie, uploadedFile);
        } catch (IOException e) {}
        String base = PropertyHolder.get().getProjPath() + File.separator + "unziped";
        File file = new File(base + File.separator + "tmp");
        if (file.exists())
            try {
                FileUtils.deleteDirectory(file);
            } catch (IOException e) {}
        file.mkdirs();
        UnZipFile.unzip(configFlie, file);
        File[] files=file.listFiles();
        if(files != null){
            for(File f:files){
                if(f.isDirectory()){
                    String mcode=f.getName();
                    //对导入的还原包进行是否发布过的校验 by zhushizhang
//                    ProjectFlagHolder.getInstance().getProjFlag().set(true);
                    RuntimeFlagHolder.getInstance().getRuntimeFlag().set(true);
                    List<View> viewList=viewServiceFoundation.findViewsByModuleCode(mcode);
                    if(viewList.size()==0){
                        throw new EcException(mcode+ InternationalResource.get("ec.module.module")+InternationalResource.get("ec.engine.publishstste.not"));
                    }
                    //校验结束
                    String targetPath=base+File.separator+"tmp"+File.separator+mcode;
                    for(String k:filter){
                        ProjImportExportService service=serviceMap.get(k);
                        if(service!=null){
                            service.importProjConfig(mcode, targetPath);
                        }
                    }
                    RuntimeFlagHolder.getInstance().getRuntimeFlag().set(false);
                }
            }
        }
    }

    @Override
    @Transactional
    public void exportProj(List<String> moduleCodes) {
        if(serviceMap==null){
            initServiceMap();
        }
        File f = new File(PropertyHolder.get().getProjPath() + File.separator +ZIP_NAME);
        if (f.exists()) {
            deleteDir(f);// delete if exist.
        }
        for(String mcode:moduleCodes){
            String targetPath=PropertyHolder.get().getProjPath()+File.separator+ZIP_NAME+File.separator+mcode;
            for(String k:serviceMap.keySet()){
                serviceMap.get(k).exportProjConfig(mcode, targetPath);
            }
        }
        compress();
    }

    public void initServiceMap() {
        ApplicationContext context = SpringContextHolder.getApplicationContext();
        serviceMap = new HashMap<>();
        if (context != null) {
            Map<String, ProjImportExportService> map = context.getBeansOfType(ProjImportExportService.class);
            if(map !=null){
                for(String name: map.keySet()){
                    for(String n:SERVICE_NAME){
                        if(name.toLowerCase().contains(n.toLowerCase())){
                            serviceMap.put(n,map.get(name));
                        }
                    }
                }
            }
        }
    }
    private File compress(){
        File f = new File(PropertyHolder.get().getProjPath() + File.separator +ZIP_NAME+".zip");
        if (f.exists()) {
            f.delete();// delete if exist.
        }
        String path = PropertyHolder.get().getProjPath() + File.separator + ZIP_NAME;
        Zip zip = new Zip();
        Project p = new Project();
        zip.setProject(p);
        zip.setDestFile(f);
        FileSet fileSet = new FileSet();
        fileSet.setProject(p);
        File file = new File(path);
        fileSet.setDir(file);
        zip.add(fileSet);
        zip.execute();
        return zip.getDestFile();
    }

    private  boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            //递归删除目录中的子目录下
            if(children != null){
                for (int i=0; i<children.length; i++) {
                    boolean success = deleteDir(new File(dir, children[i]));
                    if (!success) {
                        return false;
                    }
                }
            }
        }
        // 目录此时为空，可以删除
        return dir.delete();
    }

    @Override
    public String getZipFilePath() {
        return PropertyHolder.get().getProjPath() + File.separator + ZIP_NAME+".zip";
    }

}
