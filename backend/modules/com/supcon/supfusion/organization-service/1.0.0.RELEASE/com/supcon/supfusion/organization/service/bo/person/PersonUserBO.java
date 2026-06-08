package com.supcon.supfusion.organization.service.bo.person;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PersonUserBO {

    private Long personId;

    private Long userId;

    private String userName;
}
