/*
 * Decompiled with CFR 0.152.
 */
package com.supcon.supos.suposgateway.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class CollectionUtil {
    private CollectionUtil() {
    }

    public static Collection getDifferent(Collection collmax, Collection collmin) {
        LinkedList<Object> csReturn = new LinkedList<Object>();
        Collection max = collmax;
        Collection min = collmin;
        if (collmax.size() < collmin.size()) {
            max = collmin;
            min = collmax;
        }
        HashMap map = new HashMap(max.size());
        for (Object e : max) {
            map.put(e, 1);
        }
        for (Object e : min) {
            if (map.get(e) == null) {
                csReturn.add(e);
                continue;
            }
            map.put(e, 2);
        }
        for (Map.Entry entry : map.entrySet()) {
            if ((Integer)entry.getValue() != 1) continue;
            csReturn.add(entry.getKey());
        }
        return csReturn;
    }

    public static List<String> getSame(Collection collmax, Collection collmin) {
        LinkedList csReturn = new LinkedList();
        Collection max = collmax;
        Collection min = collmin;
        if (collmax.size() < collmin.size()) {
            max = collmin;
            min = collmax;
        }
        HashMap map = new HashMap(max.size());
        for (Object object : max) {
            map.put(object, 1);
        }
        for (Object object : min) {
            if (map.get(object) == null) continue;
            csReturn.add(object);
        }
        return csReturn;
    }

    public static Collection getDiffentNoDuplicate(Collection collmax, Collection collmin) {
        return new HashSet(CollectionUtil.getDifferent(collmax, collmin));
    }

    public static List<String> getDifferentList(List<String> list1, List<String> list2) {
        HashMap<String, Integer> map = new HashMap<String, Integer>(list1.size() + list2.size());
        ArrayList<String> diff = new ArrayList<String>();
        List<String> maxList = list1;
        List<String> minList = list2;
        if (list2.size() > list1.size()) {
            maxList = list2;
            minList = list1;
        }
        for (String string : maxList) {
            map.put(string, 1);
        }
        for (String string : minList) {
            Integer cc = (Integer)map.get(string);
            if (cc != null) {
                cc = cc + 1;
                map.put(string, cc);
                continue;
            }
            map.put(string, 1);
        }
        for (Map.Entry entry : map.entrySet()) {
            if ((Integer)entry.getValue() != 1) continue;
            diff.add((String)entry.getKey());
        }
        return diff;
    }
}

