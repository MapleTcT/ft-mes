package com.supcon.supfusion.theme.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.supcon.supfusion.theme.dao.po.SystemThemePO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface SystemThemeService extends IService<SystemThemePO> {

    List<SystemThemePO> querySystemThemeList();

    SystemThemePO querySystemThemePO();

    SystemThemePO querySystemThemePOByTheme(String theme);

    void updateSystemTheme(SystemThemePO systemThemePO);

    void uploadLogo(MultipartFile file, String theme);
}
