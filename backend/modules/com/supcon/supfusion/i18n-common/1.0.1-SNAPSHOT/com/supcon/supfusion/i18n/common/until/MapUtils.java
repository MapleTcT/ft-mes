package com.supcon.supfusion.i18n.common.until;

import java.util.*;

public class MapUtils {
    //按照key 排序 倒叙
    public static Map<String, String> sortMapByKeyDesc(Map<String, String> oriMap) {
        Map<String, String> sortedMap = new LinkedHashMap<String, String>();
        try {
            if (oriMap != null && !oriMap.isEmpty()) {
                List<Map.Entry<String, String>> entryList = new ArrayList<Map.Entry<String, String>>(oriMap.entrySet());
                Collections.sort(entryList,
                        new Comparator<Map.Entry<String, String>>() {
                            public int compare(Map.Entry<String, String> entry1,
                                               Map.Entry<String, String> entry2) {
                                int value1 = 0, value2 = 0;
                                try {
                                    value1 = Integer.parseInt(entry1.getKey());
                                    value2 = Integer.parseInt(entry2.getKey());
                                } catch (NumberFormatException e) {
                                    value1 = 0;
                                    value2 = 0;
                                }
                                return value2 - value1;
                            }
                        });
                Iterator<Map.Entry<String, String>> iter = entryList.iterator();
                Map.Entry<String, String> tmpEntry = null;
                while (iter.hasNext()) {
                    tmpEntry = iter.next();
                    sortedMap.put(tmpEntry.getKey(), tmpEntry.getValue());
                }
            }
        } catch (Exception e) {
        }
        return sortedMap;
    }

    //按照key 排序 升序
    public static Map<String, String> sortMapByKeyAsc(Map<String, String> oriMap) {
        Map<String, String> sortedMap = new LinkedHashMap<String, String>();
        try {
            if (oriMap != null && !oriMap.isEmpty()) {
                List<Map.Entry<String, String>> entryList = new ArrayList<Map.Entry<String, String>>(oriMap.entrySet());
                Collections.sort(entryList,
                        new Comparator<Map.Entry<String, String>>() {
                            public int compare(Map.Entry<String, String> entry2,
                                               Map.Entry<String, String> entry1) {
                                int value2 = 0, value1 = 0;
                                try {
                                    value2 = Integer.parseInt(entry1.getKey());
                                    value1 = Integer.parseInt(entry2.getKey());
                                } catch (NumberFormatException e) {
                                    value2 = 0;
                                    value1 = 0;
                                }
                                return value1 - value2;
                            }
                        });
                Iterator<Map.Entry<String, String>> iter = entryList.iterator();
                Map.Entry<String, String> tmpEntry = null;
                while (iter.hasNext()) {
                    tmpEntry = iter.next();
                    sortedMap.put(tmpEntry.getKey(), tmpEntry.getValue());
                }
            }
        } catch (Exception e) {
        }
        return sortedMap;
    }


    //根据value 倒叙
    public static Map<String, Long> sortMapByValueDesc(Map<String, Long> oriMap) {
        Map<String, Long> sortedMap = new LinkedHashMap<String, Long>();
        try {
            if (oriMap != null && !oriMap.isEmpty()) {
                List<Map.Entry<String, Long>> entryList = new ArrayList<Map.Entry<String, Long>>(oriMap.entrySet());
                Collections.sort(entryList,
                        new Comparator<Map.Entry<String, Long>>() {
                            public int compare(Map.Entry<String, Long> entry1,
                                               Map.Entry<String, Long> entry2) {
                                long value1 = 0, value2 = 0;
                                try {
                                    value1 = entry1.getValue();
                                    value2 = entry2.getValue();
                                } catch (NumberFormatException e) {
                                    value1 = 0;
                                    value2 = 0;
                                }
                                if (value2 > value1) {
                                    return 1;
                                } else if (value2 < value1) {
                                    return -1;
                                } else {
                                    return 0;
                                }
                            }
                        });
                Iterator<Map.Entry<String, Long>> iter = entryList.iterator();
                Map.Entry<String, Long> tmpEntry = null;
                while (iter.hasNext()) {
                    tmpEntry = iter.next();
                    sortedMap.put(tmpEntry.getKey(), tmpEntry.getValue());
                }
            }
        } catch (Exception e) {
        }
        return sortedMap;
    }

}
