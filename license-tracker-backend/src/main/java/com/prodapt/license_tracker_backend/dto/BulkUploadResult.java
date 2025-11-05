package com.prodapt.license_tracker_backend.dto;



import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class BulkUploadResult {
    private int totalRecords;
    private int successCount;
    private int failureCount;
    private List<String> successMessages;
    private List<String> errorMessages;

    public BulkUploadResult() {
        this.successMessages = new ArrayList<>();
        this.errorMessages = new ArrayList<>();
    }

    public void addSuccess(String message) {
        this.successMessages.add(message);
        this.successCount++;
    }

    public void addError(String message) {
        this.errorMessages.add(message);
        this.failureCount++;
    }
}
