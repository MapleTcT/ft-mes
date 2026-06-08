package com.supcon.supfusion.base.utils;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
    /**
     *
     *
     * @author zhuwei2
     * @version $Id$
     */
    @Data
    @Slf4j
    @Component
    public class RuntimeFlagHolder {
        private ThreadLocal<Boolean> runtimeFlag=new ThreadLocal<Boolean>();
        private static RuntimeFlagHolder runtimeFlagHolder;
        private RuntimeFlagHolder(){
        };
        public static RuntimeFlagHolder getInstance(){
            if(runtimeFlagHolder==null){
                runtimeFlagHolder=new RuntimeFlagHolder();
            }
            return runtimeFlagHolder;
        }
        public ThreadLocal<Boolean> getRuntimeFlag() {
            return runtimeFlag;
        }
    }
