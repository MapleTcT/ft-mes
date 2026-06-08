package com.supcon.supfusion.i18n.until;

import com.supcon.supfusion.i18n.dao.I18nResourceDao;
import com.supcon.supfusion.i18n.dao.po.I18nResourcePO;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CountDownLatch;

@Slf4j
public class MultiThreadSaveDataToDB extends Thread {

    private List<I18nResourcePO> i18nrs;
    private I18nResourceDao i18nResourceDao;
    private CountDownLatch countDownLatch;

    public void setI18nrs(List<I18nResourcePO> i18nrs) {
        this.i18nrs = i18nrs;
    }

    public void setI18nResourceDao(I18nResourceDao i18nResourceDao) {
        this.i18nResourceDao = i18nResourceDao;
    }

    public void setCountDownLatch(CountDownLatch countDownLatch) {
        this.countDownLatch = countDownLatch;
    }

    @Override
    public void run() {
        try {
            i18nResourceDao.saveBatch(i18nrs);
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            if (countDownLatch != null) {
                countDownLatch.countDown();
            }
        }
    }
}
