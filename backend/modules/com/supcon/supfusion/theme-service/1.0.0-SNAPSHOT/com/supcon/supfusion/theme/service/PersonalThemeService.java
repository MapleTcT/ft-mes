package com.supcon.supfusion.theme.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.supcon.supfusion.theme.dao.po.PersonalThemePO;

import java.util.List;

public interface PersonalThemeService extends IService<PersonalThemePO> {

    List<PersonalThemePO> queryPersonalThemeList();

    void updatePersonalTheme(PersonalThemePO personalThemePO);

    PersonalThemePO queryPersonalTheme();

    void deletePersonalTheme();

}
