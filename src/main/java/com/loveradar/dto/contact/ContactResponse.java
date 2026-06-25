package com.loveradar.dto.contact;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactResponse {
    private String id;
    private String contactName;
    private String phoneNumber;
    private boolean trusted;
    private boolean linkedToUser;
    private int encounterCount;
    private String lastEncountered;
}
