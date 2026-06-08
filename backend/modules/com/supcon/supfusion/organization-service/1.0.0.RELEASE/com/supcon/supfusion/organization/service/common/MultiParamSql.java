package com.supcon.supfusion.organization.service.common;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.supcon.supfusion.organization.dao.mapper.department.DepartmentMapper;
import com.supcon.supfusion.organization.dao.mapper.person.PersonMapper;
import com.supcon.supfusion.organization.dao.mapper.position.PositionMapper;
import com.supcon.supfusion.organization.dao.mapper.position.PositionPersonMapper;
import com.supcon.supfusion.organization.dao.po.department.DepartmentAddPO;
import com.supcon.supfusion.organization.dao.po.person.PersonAddPO;
import com.supcon.supfusion.organization.dao.po.position.PositionAddPO;
import com.supcon.supfusion.organization.dao.po.position.PositionPersonPO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiParamSql {

    /**
     * 人员多参数
     *
     * @param perIds
     * @param personWrapper
     * @param columnName
     * @param personMapper
     * @return
     */
    public static List<PersonAddPO> forPerson(List perIds, QueryWrapper<PersonAddPO> personWrapper, String columnName, PersonMapper personMapper) {
        List<String> ids = new ArrayList<>(perIds);
        int batch = ids.size() / 1000;

        List<PersonAddPO> resultList = new ArrayList<>();
        if (0 == batch) {
            QueryWrapper<PersonAddPO> clone = personWrapper.clone();
            clone.in(columnName, ids);
            resultList.addAll(personMapper.selectList(clone));
            return resultList;
        } else {
            for (int i = 0; i < batch; i++) {
                QueryWrapper<PersonAddPO> clone = personWrapper.clone();
                clone.in(columnName, ids.subList(i * 1000, i * 1000 + 1000));
                resultList.addAll(personMapper.selectList(clone));
            }
            if (ids.size() % 1000 != 0) {
                QueryWrapper<PersonAddPO> clone = personWrapper.clone();
                clone.in(columnName, ids.subList(batch * 1000, ids.size()));
                resultList.addAll(personMapper.selectList(clone));
            }
            return resultList;
        }
    }

    /**
     * 部门多参数
     *
     * @param perIds
     * @param departmentWrapper
     * @param columnName
     * @param departmentMapper
     * @return
     */
    public static List<DepartmentAddPO> forDepartment(List perIds, QueryWrapper<DepartmentAddPO> departmentWrapper, String columnName, DepartmentMapper departmentMapper) {
        List ids = new ArrayList<>(perIds);
        int batch = ids.size() / 1000;

        List<DepartmentAddPO> resultList = new ArrayList<>();
        if (0 == batch) {
            QueryWrapper<DepartmentAddPO> clone = departmentWrapper.clone();
            clone.in(columnName, ids);
            resultList.addAll(departmentMapper.selectList(clone));
            return resultList;
        } else {
            for (int i = 0; i < batch; i++) {
                QueryWrapper<DepartmentAddPO> clone = departmentWrapper.clone();
                clone.in(columnName, ids.subList(i * 1000, i * 1000 + 1000));
                resultList.addAll(departmentMapper.selectList(clone));
            }
            if (ids.size() % 1000 != 0) {
                QueryWrapper<DepartmentAddPO> clone = departmentWrapper.clone();
                clone.in(columnName, ids.subList(batch * 1000, ids.size()));
                resultList.addAll(departmentMapper.selectList(clone));
            }
            return resultList;
        }
    }

    /**
     * 岗位多参数
     *
     * @param perIds
     * @param positionWrapper
     * @param columnName
     * @param positionMapper
     * @return
     */
    public static List<PositionAddPO> forPosition(List perIds, QueryWrapper<PositionAddPO> positionWrapper, String columnName, PositionMapper positionMapper) {
        List ids = new ArrayList<>(perIds);
        int batch = ids.size() / 1000;

        List<PositionAddPO> resultList = new ArrayList<>();
        if (0 == batch) {
            QueryWrapper<PositionAddPO> clone = positionWrapper.clone();
            clone.in(columnName, ids);
            resultList.addAll(positionMapper.selectList(clone));
            return resultList;
        } else {
            for (int i = 0; i < batch; i++) {
                QueryWrapper<PositionAddPO> clone = positionWrapper.clone();
                clone.in(columnName, ids.subList(i * 1000, i * 1000 + 1000));
                resultList.addAll(positionMapper.selectList(clone));
            }
            if (ids.size() % 1000 != 0) {
                QueryWrapper<PositionAddPO> clone = positionWrapper.clone();
                clone.in(columnName, ids.subList(batch * 1000, ids.size()));
                resultList.addAll(positionMapper.selectList(clone));
            }
            return resultList;
        }
    }

    /**
     * 人员岗位多参数
     *
     * @param perIds
     * @param positionPersonQueryWrapper
     * @param columnName
     * @param positionPersonMapper
     * @return
     */
    public static List<PositionPersonPO> forPersonPosition(List perIds, QueryWrapper<PositionPersonPO> positionPersonQueryWrapper, String columnName, PositionPersonMapper positionPersonMapper) {
        List ids = new ArrayList<>(perIds);
        int batch = ids.size() / 1000;

        List<PositionPersonPO> resultList = new ArrayList<>();
        if (0 == batch) {
            QueryWrapper<PositionPersonPO> clone = positionPersonQueryWrapper.clone();
            clone.in(columnName, ids);
            resultList.addAll(positionPersonMapper.selectList(clone));
            return resultList;
        } else {
            for (int i = 0; i < batch; i++) {
                QueryWrapper<PositionPersonPO> clone = positionPersonQueryWrapper.clone();
                clone.in(columnName, ids.subList(i * 1000, i * 1000 + 1000));
                resultList.addAll(positionPersonMapper.selectList(clone));
            }
            if (ids.size() % 1000 != 0) {
                QueryWrapper<PositionPersonPO> clone = positionPersonQueryWrapper.clone();
                clone.in(columnName, ids.subList(batch * 1000, ids.size()));
                resultList.addAll(positionPersonMapper.selectList(clone));
            }
            return resultList;
        }
    }

    /**
     * 人员岗位多参数
     *
     * @param perIds
     * @param positionPersonQueryWrapper
     * @param columnName
     * @param positionPersonMapper
     * @return
     */
    public static List<Map<String, Object>> forPersonPositionMap(List perIds, QueryWrapper<PositionPersonPO> positionPersonQueryWrapper, String columnName, PositionPersonMapper positionPersonMapper) {
        List ids = new ArrayList<>(perIds);
        int batch = ids.size() / 1000;

        List<Map<String, Object>> resultList = new ArrayList<>();
        if (0 == batch) {
            QueryWrapper<PositionPersonPO> clone = positionPersonQueryWrapper.clone();
            clone.in(columnName, ids);
            resultList.addAll(positionPersonMapper.selectMaps(clone));
            return resultList;
        } else {
            Map<String, Object> map = new HashMap<>();
            for (int i = 0; i < batch; i++) {
                QueryWrapper<PositionPersonPO> clone = positionPersonQueryWrapper.clone();
                clone.in(columnName, ids.subList(i * 1000, i * 1000 + 1000));
                commonForMap(positionPersonMapper, map, clone);
            }
            if (ids.size() % 1000 != 0) {
                QueryWrapper<PositionPersonPO> clone = positionPersonQueryWrapper.clone();
                clone.in(columnName, ids.subList(batch * 1000, ids.size()));
                commonForMap(positionPersonMapper, map, clone);
            }

            for (Map.Entry<String, Object> entry : map.entrySet()) {
                Map<String, Object> hashMap = new HashMap<>();
                hashMap.put("POSITIONID", entry.getKey());
                hashMap.put("COUNT", entry.getValue());
                resultList.add(hashMap);
            }
            return resultList;
        }
    }

    private static void commonForMap(PositionPersonMapper positionPersonMapper, Map<String, Object> map, QueryWrapper<PositionPersonPO> clone) {
        List<Map<String, Object>> mapList = positionPersonMapper.selectMaps(clone);
        for (Map<String, Object> singleMap : mapList) {
            Integer count = 0;
            String positionId = "";
            for (Map.Entry<String, Object> entry : singleMap.entrySet()) {
                if ("COUNT".equals(entry.getKey())) {
                    count = Integer.parseInt(entry.getValue().toString());
                } else if ("POSITIONID".equals(entry.getKey())) {
                    positionId = entry.getValue().toString();
                }
            }
            if (!map.containsKey(positionId)) {
                map.put(positionId, count);
            } else {
                map.put(positionId, (Integer) map.get(positionId) + count);
            }
        }
    }
}
