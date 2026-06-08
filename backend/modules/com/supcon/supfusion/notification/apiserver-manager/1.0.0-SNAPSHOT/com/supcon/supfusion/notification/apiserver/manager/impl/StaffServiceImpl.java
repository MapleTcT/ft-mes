package com.supcon.supfusion.notification.apiserver.manager.impl;

import com.alibaba.fastjson.JSON;
import com.supcon.supfusion.auth.api.UserApiService;
import com.supcon.supfusion.auth.api.dto.UserDetailDTO;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiReference;
import com.supcon.supfusion.framework.cloud.common.exception.BizHttpStatusException;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.notification.apiserver.common.bean.RangeBO;
import com.supcon.supfusion.notification.apiserver.common.execption.NotificationApiServerError;
import com.supcon.supfusion.notification.apiserver.common.execption.NotificationApiServerExecption;
import com.supcon.supfusion.notification.apiserver.manager.StaffService;
import com.supcon.supfusion.notification.common.bean.RangeType;
import com.supcon.supfusion.notification.common.util.MapUtil;
import com.supcon.supfusion.notification.protocol.common.Address;
import com.supcon.supfusion.organization.api.PersonApiService;
import com.supcon.supfusion.organization.api.dto.PersonDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class StaffServiceImpl implements StaffService {

    @ServiceApiReference
    private PersonApiService personApiService;
    @ServiceApiReference
    private UserApiService userApiService;

    @Override
    public Map<String, List<Address>> getStaffAddress(List<RangeBO> rangeBOS, Set<String> protocols) {

        Map<String, List<Address>> protocolAddresses = new HashMap<>();
        List<String> staffCodes = new ArrayList<>();
        List<String> roleCodes = new ArrayList<>();
        List<String> deparentmentCodes = new ArrayList<>();
        List<String> positionCodes = new ArrayList<>();
        for (RangeBO rangeBO : rangeBOS) {
            if (RangeType.STAFF.equals(rangeBO.getRangeType())) {
                staffCodes.addAll(rangeBO.getCodes());
            } else if (RangeType.ROLE.equals(rangeBO.getRangeType())) {
                roleCodes.addAll(rangeBO.getCodes());
            } else if (RangeType.DEPARTMENT.equals(rangeBO.getRangeType())) {
                deparentmentCodes.addAll(rangeBO.getCodes());
            } else if (RangeType.POSITION.equals(rangeBO.getRangeType())) {
                positionCodes.addAll(rangeBO.getCodes());
            }
        }

        log.info("call personApiService.queryPersonByNotification param roleCodes: {}, positionCodes: {}, : deparentmentCodes: {}, staffCodes: {},",
                JSON.toJSON(roleCodes).toString(),
                JSON.toJSON(deparentmentCodes).toString(),
                JSON.toJSON(positionCodes).toString(),
                JSON.toJSON(staffCodes).toString());
        ListResult<PersonDTO> personDetailDTOListResult = personApiService.queryPersonByNotification(roleCodes, positionCodes, deparentmentCodes, staffCodes);
        Collection<PersonDTO> persons = personDetailDTOListResult.getList();
        if (persons == null || persons.size() == 0) {
            throw new BizHttpStatusException(NotificationApiServerError.ERROR_NO_RECEIVERS, 400);
        }
        log.info("personApiService return personDetailDTOListResult: {}", JSON.toJSON(persons).toString());


        /**
         * 人员去重
         */
        Set<String> personCodes = new HashSet<>();
        persons.stream().filter(person -> {

            if (personCodes.contains(person.getCode())) {
                return false;
            } else {
                personCodes.add(person.getCode());
                return true;
            }
        });
        protocols.stream().forEach(protocol -> {
            List<Address> addresses = new ArrayList<>();
            protocolAddresses.put(protocol, addresses);
            persons.stream().forEach(person -> {
                if ("email".equals(protocol)) {
                    Address address = new Address();
                    address.setStaffCode(person.getCode());
                    address.setStaffName(person.getName());
                    address.setUserName(person.getUserName());
                    address.setAddress(person.getEmail());
                    addresses.add(address);
                } else if ("stationLetter".equals(protocol) || "mobile".equals(protocol) || "app".equals(protocol)) {
                    Address address = new Address();
                    address.setStaffCode(person.getCode());
                    address.setStaffName(person.getName());
                    address.setUserName(person.getUserName());
                    address.setAddress(person.getUserName());
                    addresses.add(address);
                }else if(Objects.equals("sms",protocol)){
                    Address address = new Address();
                    address.setStaffCode(person.getCode());
                    address.setStaffName(person.getName());
                    address.setUserName(person.getUserName());
                    address.setAddress(person.getPhone());
                    addresses.add(address);
                } else {
                    Address address = new Address();
                    address.setStaffCode(person.getCode());
                    address.setStaffName(person.getName());
                    address.setUserName(person.getUserName());
                    addresses.add(address);
                }
            });

        });
        log.info("return protocolAddresses: {}", MapUtil.toJSONString(protocolAddresses));
        return protocolAddresses;
    }

    @Override
    public Map<String, List<Address>> getUserAddress(List<String> userIds, List<String> protocols) {

        Map<String, List<Address>> protocolAddresses = new HashMap<>();
        if (userIds == null) {
            throw new BizHttpStatusException(NotificationApiServerError.ERROR_NO_USER, 400);
        } else {
            /**
             * 不存在给admin推送消息，这是运维用的，不做业务操作.  和产品确认
             */
            userIds.remove("admin");
            if (userIds.size() == 0) {
                throw new BizHttpStatusException(NotificationApiServerError.ERROR_NO_USER, 400);
            }
        }
        Result<Map<String, UserDetailDTO>> result = userApiService.getBatchUsersDetailByName(userIds.toArray(new String[userIds.size()]));
        Map<String, UserDetailDTO> userDetailDTOMap = result.getData();
        if (userDetailDTOMap == null || userDetailDTOMap.size() == 0) {
            throw new BizHttpStatusException(NotificationApiServerError.ERROR_NO_USER, 400);
        }
        log.info("userApiService return UserDetailDTOS: {}", MapUtil.toJSONString(userDetailDTOMap));
        protocols.forEach(protocol -> {
            List<Address> list = new ArrayList<>();
            protocolAddresses.put(protocol, list);
            userDetailDTOMap.values().forEach(userInfo -> {
                Address address = new Address();
                String protocolAddress = null;
                if ("stationLetter".equals(protocol) || "mobile".equals(protocol) || "app".equals(protocol)) {
                    protocolAddress = userInfo.getUserName();
                } else if ("email".equals(protocol)) {
                    protocolAddress = userInfo.getEmail();
                } else if (Objects.equals("sms", protocol)) {
                    protocolAddress = userInfo.getPhone();
                }

                String staffCode = userInfo.getPersonCode();
                String staffName = userInfo.getPersonName();
                String userName = userInfo.getUserName();
                address.setAddress(protocolAddress);
                address.setStaffCode(staffCode);
                address.setStaffName(staffName);
                address.setUserName(userName);
                list.add(address);
            });
        });
        log.info("return protocolAddresses: {}", MapUtil.toJSONString(protocolAddresses));
        return protocolAddresses;
    }



}
