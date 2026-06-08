package com.supcon.supfusion.rbac.service.asyncTask;

import com.supcon.supfusion.rbac.dao.po.MenuInfoPO;
import com.supcon.supfusion.rbac.manager.II18nAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.RecursiveTask;

@Slf4j
public class SetMenuI18NNameTask extends RecursiveTask<List<MenuInfoPO>> {
    private final II18nAdapter i18nAdapterService;
    private static final int THRESHOLD = 100;
    private final int startIndex;
    private final int endIndex;
    private final List<MenuInfoPO> flat;

    public SetMenuI18NNameTask(int startIndex, int endIndex, List<MenuInfoPO> flat, final II18nAdapter i18nAdapterService) {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.flat = flat;
        this.i18nAdapterService = i18nAdapterService;
    }

    @Override
    public List<MenuInfoPO> compute() {
        if ((endIndex - startIndex) <= THRESHOLD) {
            List<MenuInfoPO> menuInfoPOS = flat.subList(startIndex, endIndex + 1);
            Locale locale = LocaleContextHolder.getLocale();

            menuInfoPOS.stream().forEach(menuInfoPO -> {
                String name = i18nAdapterService.getRemoteMessage(menuInfoPO.getName(), null, locale);
                menuInfoPO.setNameDisplay(name);
            });
            return menuInfoPOS;
        } else {
            final int middle = (startIndex + endIndex) / 2;
            SetMenuI18NNameTask left = new SetMenuI18NNameTask(startIndex, middle, flat,i18nAdapterService);
            left.fork();
            SetMenuI18NNameTask right = new SetMenuI18NNameTask(middle + 1, endIndex, flat,i18nAdapterService);
            right.fork();
            List<MenuInfoPO> parents = new ArrayList<>();
            parents.addAll(left.join());
            parents.addAll(right.join());
            return parents;
        }
    }
}