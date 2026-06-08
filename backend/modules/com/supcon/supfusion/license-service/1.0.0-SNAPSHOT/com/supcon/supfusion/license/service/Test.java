package com.supcon.supfusion.license.service;

import com.supcon.supfusion.license.common.constants.Constants;
import com.supcon.supfusion.license.common.utils.date.DateHelper;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

public class Test {
    public static void main(String[] args) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        String s = DateHelper.millToHour(3600000);
        System.out.println("s = " + s);


    }
}
