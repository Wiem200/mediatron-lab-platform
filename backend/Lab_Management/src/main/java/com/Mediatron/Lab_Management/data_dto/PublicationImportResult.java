package com.Mediatron.Lab_Management.data_dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PublicationImportResult {
    private Integer memberId;
    private String memberName;
    private String queryUsed;
    private Integer totalFetched;
    private Integer importedCount;
    private Integer linkedExistingCount;
    private Integer skippedCount;
    private String message;
}