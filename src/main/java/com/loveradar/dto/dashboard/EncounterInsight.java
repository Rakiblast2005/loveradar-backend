package com.loveradar.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EncounterInsight {
    private String contactName;
    private int encounterCount;
    private String lastEncountered;
}
