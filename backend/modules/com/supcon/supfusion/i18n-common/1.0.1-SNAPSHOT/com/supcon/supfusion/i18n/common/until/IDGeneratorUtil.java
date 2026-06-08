package com.supcon.supfusion.i18n.common.until;

import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

@Slf4j
public class IDGeneratorUtil {


    public static Set<String> setid = new HashSet<>();

    public static Long getId() {
        Long id1 = IDGenerator.newInstance().generate().longValue();
        Random random = new Random();
        int s = random.nextInt(1000000) + 1;
        //System.out.println("id:"+id1 * s);
        Double id = id1 * s * Math.random();
        long l = new Double(id).longValue();
        //System.out.println("l:"+l);
        String idStr = String.valueOf(l).replace("-", "").substring(0, 15);
        return Long.valueOf(idStr);

    }


    public static void main(String[] args) {
//        Long id = getId();
//        System.out.println(id);
        MyThread myThread = new MyThread();
        MyThread2 myThread2= new MyThread2();
        Thread TH1 = new Thread(myThread, "a");
        Thread TH2 = new Thread(myThread2, "b");
        TH1.start();
        TH2.start();
        System.out.println("xxx:"+setid.size());

    }

    public static void main2() {
        setid.clear();
        System.out.println("xxx1:"+setid.size());
        MyThread myThread = new MyThread();
        MyThread2 myThread2= new MyThread2();
        Thread TH1 = new Thread(myThread, "a");
        Thread TH2 = new Thread(myThread2, "b");
        TH1.start();
        TH2.start();
        System.out.println("xxx2:"+setid.size());
    }

    static class MyThread implements Runnable {
        @Override
        public void run() {
            System.out.println("开始1");
            log.info("======================开始1：生成key:100000个");
            Set<String> setid1 = new HashSet<>();
            for (int i = 0; i < 100000; i++) {
                setid1.add(IDGenerator.newInstance().generate().longValue()+"");
            }
            setid1.add("111");
            setid.addAll(setid1);
            System.out.println("结束1:"+setid.size());
            log.info("=====================结束1:"+setid.size());
        }
    }
    static class MyThread2 implements Runnable {
        @Override
        public void run() {
            System.out.println("开始2");
            log.info("====================开始2:生成key:100000个");
            Set<String> setid2 = new HashSet<>();
            for (int i = 0; i < 100000; i++) {
                setid2.add(IDGenerator.newInstance().generate().longValue()+"");
            }
            setid2.add("111");
            setid.addAll(setid2);
            System.out.println("结束2:"+setid.size());
            log.info("===================结束2:"+setid.size());
        }
    }
}
