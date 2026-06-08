/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.supcon.supfusion.authkeycloak.entity;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserEntity {

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Long id;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String userName;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Boolean hasLock;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Boolean valid;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String password;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<Company> companies;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Long personId;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String personName;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String personCode;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String phone;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String email;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Long companyId;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Integer userType;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Long userDirectoryId;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String ldapUserName;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Long positionId;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String positionName;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String positionCode;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Long positionCompanyId;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Long departmentId;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String departmentName;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String departmentCode;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String companyName;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String companyCode;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String companyType;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Company {
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private Long companyId;
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private String companyName;
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private String companyCode;
    }
}
