package com.Mediatron.Lab_Management.data_dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HomepageStatisticsSummaryDto {
    private long publicationCount;
    private long journalAndConferenceCount;
    private long classifiedConferenceCount;
}
