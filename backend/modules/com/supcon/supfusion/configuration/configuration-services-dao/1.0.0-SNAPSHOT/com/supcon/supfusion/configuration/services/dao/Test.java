package com.supcon.supfusion.configuration.services.dao;

public class Test {
    public static void main(String[] args) {
        String a = "hh" + "\"a\"";
        System.out.println("a = " + a);
         a = a.replace("\"", "`");
        System.out.println("replace = " + a);
    }
}
