package com.supcon.supfusion.notification.apiserver.manager;


import com.supcon.supfusion.notification.apiserver.common.bean.RangeBO;
import com.supcon.supfusion.notification.protocol.common.Address;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface StaffService {
    Map<String, List<Address>> getStaffAddress(List<RangeBO> rangeBOS, Set<String> protocols);

    Map<String, List<Address>> getUserAddress(List<String> userIds, List<String> protocols);
}
