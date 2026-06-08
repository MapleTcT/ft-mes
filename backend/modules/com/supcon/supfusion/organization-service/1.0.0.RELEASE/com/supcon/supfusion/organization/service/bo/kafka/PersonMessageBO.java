package com.supcon.supfusion.organization.service.bo.kafka;


import com.supcon.supfusion.organization.service.bo.person.MainPositionBaseBO;
import com.supcon.supfusion.organization.service.bo.person.SystemCodeBO;
import com.supcon.supfusion.organization.service.bo.person.UserBO;
import lombok.*;

import java.util.List;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PersonMessageBO {

    private Long rowVersion;

    private Long id;

    private String code;

    private String name;

    private String phone;

    private String email;

    private String description;

    private SystemCodeBO gender;

    private SystemCodeBO status;

    private UserBO user;

    private MainPositionBaseBO mainPosition;

    private List<MainPositionBaseBO> positions;

    private String avatarUrl;

    private String signPicUrl;

    private String entryDate;

    private SystemCodeBO title;

    private String qualification;

    private SystemCodeBO education;

    private String major;

    private String idNumber;
}
