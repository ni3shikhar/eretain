package com.eretain.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkUploadResultDTO {
    private int totalRecords;
    private int successCount;
    private int failureCount;

    @Builder.Default
    private List<RecordResult> results = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecordResult {
        private int rowNumber;
        private String projectCode;
        private String projectName;
        private String status; // SUCCESS or FAILED
        private String reason;
    }
}
