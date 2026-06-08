package com.supcon.supfusion.theme.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;
import com.supcon.supfusion.theme.dao.PersonalThemeMapper;
import com.supcon.supfusion.theme.dao.po.PersonalThemePO;
import com.supcon.supfusion.theme.dao.po.SystemThemePO;
import com.supcon.supfusion.theme.service.PersonalThemeService;
import com.supcon.supfusion.theme.service.SystemThemeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class PersonalThemeServiceImpl extends ServiceImpl<PersonalThemeMapper, PersonalThemePO> implements PersonalThemeService {

    @Autowired
    SystemThemeService systemThemeService;

    @Override
    public List<PersonalThemePO> queryPersonalThemeList() {
        Long userId = UserContext.getUserContext().getUserId();
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq(PersonalThemePO.getUserIdFieldName(), userId);
        List<PersonalThemePO> personalThemePOList = list(queryWrapper);
        for (PersonalThemePO personalThemePO : personalThemePOList) {
            SystemThemePO systemThemePO = systemThemeService.querySystemThemePOByTheme(personalThemePO.getTheme());
            if (Objects.nonNull(systemThemePO)) {
                personalThemePO.setLogo(systemThemePO.getLogo());
            }
        }
        return personalThemePOList;
    }

    @Override
    @Transactional
    public void updatePersonalTheme(PersonalThemePO personalThemePO) {
        Long userId = UserContext.getUserContext().getUserId();
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq(PersonalThemePO.getUserIdFieldName(), userId);
        remove(queryWrapper);

        personalThemePO.setUserId(userId);
        personalThemePO.setType("theme");
        personalThemePO.setStatus(1);
        save(personalThemePO);
    }

    @Override
    public PersonalThemePO queryPersonalTheme() {
        Long userId = UserContext.getUserContext().getUserId();
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq(PersonalThemePO.getUserIdFieldName(), userId);
        queryWrapper.eq(PersonalThemePO.getStatusFieldName(), 1);
        PersonalThemePO personalThemePO = getOne(queryWrapper);
        if (Objects.isNull(personalThemePO)) {
            personalThemePO = new PersonalThemePO();
            SystemThemePO systemThemePO = systemThemeService.querySystemThemePO();
            BeanUtils.copyProperties(systemThemePO, personalThemePO);
        } else {
            SystemThemePO systemThemePO = systemThemeService.querySystemThemePOByTheme(personalThemePO.getTheme());
            if (Objects.nonNull(systemThemePO)) {
                personalThemePO.setLogo(systemThemePO.getLogo());
            }
        }
        return personalThemePO;
    }

    @Override
    public void deletePersonalTheme() {
        Long userId = UserContext.getUserContext().getUserId();
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq(PersonalThemePO.getUserIdFieldName(), userId);
        remove(queryWrapper);
    }

}
