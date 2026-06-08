package com.supcon.supfusion.configuration.services.service.rpc;

import com.supcon.supfusion.configuration.services.config.CustomFeignConfiguration;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApi;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;


/**
 * @Description: TODO
 * @Version V1.0
 * @Auther: huning
 * @Date: 2020/9/8
 */
@FeignClient(name = "servicemanager", configuration = CustomFeignConfiguration.class)
@ServiceApi(path = "/servicemanager/msModule/")
public interface MsModuleServiceApi {


    @PostMapping("view/publish")
    void publishView(@RequestParam("viewCode") String viewCode,@RequestParam("isProj")Boolean isProj);

    @GetMapping("import-template/publish")
    void publishImportTemplate(@RequestParam("modelCode") String modelCode);
    @GetMapping("module/updatemd5")
    void updateLayoutJson(@RequestParam("modulecode") String moduleCode);

}
