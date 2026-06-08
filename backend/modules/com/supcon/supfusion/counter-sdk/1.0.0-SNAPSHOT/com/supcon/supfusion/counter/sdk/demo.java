package com.supcon.supfusion.counter.sdk;

import com.supcon.supfusion.counter.sdk.generator.DefaultCodeGenerator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class demo {

//    @TestConfiguration
//    static class testService{
//        @Bean
//        public CodeGenerator getBean(){
//            return new DefaultCodeGenerator();
//        }
//    }

    @Autowired
    private CodeGenerator codeGenerator;

    @Test
    public void test1() throws Exception {
//        System.out.println(codeGenerator.testAutoConfig());
    }

}
