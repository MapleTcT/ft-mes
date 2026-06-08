package com.supcon.supfusion.rbac.service.asyncTask;

import com.supcon.supfusion.rbac.dao.po.MenuSuposPO;
import com.supcon.supfusion.rbac.manager.II18nAdapter;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.RecursiveTask;

/**
 * @author tomcat
 * @date 21-6-2 上午9:04
 */
public class SetSuposMenuI18NNameTask extends RecursiveTask<List<MenuSuposPO>> {
    private final II18nAdapter i18nAdapterService;
    private static final int               THRESHOLD = 100;
    private final        int               startIndex;
    private final        int               endIndex;
    private final        List<MenuSuposPO> flat;

    public SetSuposMenuI18NNameTask(int startIndex, int endIndex, List<MenuSuposPO> flat, final II18nAdapter i18nAdapterService) {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.flat = flat;
        this.i18nAdapterService = i18nAdapterService;
    }

    @Override
    public List<MenuSuposPO> compute() {
        if ((endIndex - startIndex) <= THRESHOLD) {
            List<MenuSuposPO> subPos = flat.subList(startIndex, endIndex + 1);
            Locale locale = LocaleContextHolder.getLocale();

            subPos.stream().forEach(po -> {
                String name = i18nAdapterService.getRemoteMessage(po.getName(), null, locale);
                po.setNameDisplay(name);
            });
            return subPos;
        } else {
            final int middle = (startIndex + endIndex) / 2;
            SetSuposMenuI18NNameTask left = new SetSuposMenuI18NNameTask(startIndex, middle, flat, i18nAdapterService);
            left.fork();
            SetSuposMenuI18NNameTask right = new SetSuposMenuI18NNameTask(middle + 1, endIndex, flat, i18nAdapterService);
            right.fork();
            List<MenuSuposPO> parents = new ArrayList<>();
            parents.addAll(left.join());
            parents.addAll(right.join());
            return parents;
        }
    }
}
