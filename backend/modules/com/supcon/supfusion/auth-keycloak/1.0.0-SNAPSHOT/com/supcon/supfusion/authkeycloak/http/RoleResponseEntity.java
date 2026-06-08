package com.supcon.supfusion.authkeycloak.http;


import com.supcon.supfusion.authkeycloak.entity.RoleEntity;
import lombok.Data;

import java.util.List;

@Data
public class RoleResponseEntity {
    private List<RoleEntity> list;
}
