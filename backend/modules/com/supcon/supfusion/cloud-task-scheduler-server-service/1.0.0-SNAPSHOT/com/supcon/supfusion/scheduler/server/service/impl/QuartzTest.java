package com.supcon.supfusion.scheduler.server.service.impl;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

import com.supcon.supfusion.scheduler.server.service.job.ScheduleQuartzJob;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

public class QuartzTest {

    public static void main(String[] args) {

        try {

            //从调度程序工厂获取一个调度程序的实例
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();

            //显示调度程序的名称（这里会展示我们在quartz.properties文件中的名称）
            System.out.println("scheduleName = " + scheduler.getSchedulerName());

            /** 重要:
             *  定义一个job，并绑定到我们自定义的HelloJob的class对象
             *  这里并不会马上创建一个HelloJob实例，实例创建是在scheduler安排任务触发执行时创建的
             *  这种机制也为后面使用Spring集成提供了便利
             */
            JobDetail job = newJob(ScheduleQuartzJob.class).withIdentity("job1", "group1").build();

            // 声明一个触发器，现在就执行(schedule.start()方法开始调用的时候执行)；并且每间隔2秒就执行一次
            Trigger trigger = newTrigger().withIdentity("trigger1", "group1").withSchedule(simpleSchedule().withIntervalInSeconds(2).repeatForever()).build();
            CronTrigger cronTrigger = newTrigger().withIdentity("trigger1", "group1").withSchedule(cronSchedule("*/2 * * * * ?")).build();
            // 告诉quartz使用定义的触发器trigger安排执行任务job
            scheduler.scheduleJob(job, cronTrigger);

            //启动任务调度程序,内部机制是线程的启动
            scheduler.start();

            //关闭任务调度程序,如果不关闭，调度程序schedule会一直运行着
            //scheduler.shutdown();

        } catch (SchedulerException e) {

            e.printStackTrace();
        }
    }
}
