package com.eretain.project.service;

import com.eretain.common.enums.BillabilityCategory;
import com.eretain.common.enums.ProjectStatus;
import com.eretain.common.enums.ProjectType;
import com.eretain.project.dto.BulkUploadResultDTO;
import com.eretain.project.dto.BulkUploadResultDTO.RecordResult;
import com.eretain.project.entity.Project;
import com.eretain.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BulkUploadService {

    private final ProjectRepository projectRepository;

    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("M/d/yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("MM-dd-yyyy"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ofPattern("MMM dd, yyyy"),
            DateTimeFormatter.ofPattern("MMMM dd, yyyy"),
            DateTimeFormatter.ofPattern("dd MMM yyyy"),
            DateTimeFormatter.ofPattern("dd-MMM-yyyy"),
    };

    private static final String[] EXPECTED_HEADERS = {
            "Project Code", "Project Name", "Description", "Project Type",
            "Billability Category", "Status", "Client Name", "Start Date",
            "End Date", "Budget", "Currency"
    };

    /**
     * Generate CSV template content for project bulk upload.
     */
    public String generateTemplate() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", EXPECTED_HEADERS)).append("\n");
        // Sample row
        sb.append("PRJ-001,Sample Project,Project description,TIME_AND_MATERIAL,BILLABLE,PROPOSED,Acme Corp,2026-04-01,2026-12-31,100000.00,USD\n");
        sb.append("PRJ-002,Internal Tool,Internal tool dev,INTERNAL,NON_BILLABLE,ACTIVE,,2026-05-01,2026-11-30,,USD\n");
        return sb.toString();
    }

    /**
     * Process bulk upload CSV file.
     */
    @Transactional
    public BulkUploadResultDTO processUpload(MultipartFile file) {
        List<RecordResult> results = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;
        int rowNumber = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.trim().isEmpty()) {
                results.add(RecordResult.builder()
                        .rowNumber(0).status("FAILED")
                        .reason("File is empty or missing header row")
                        .build());
                return buildResult(0, 0, 1, results);
            }

            // Validate header
            String validationError = validateHeader(headerLine);
            if (validationError != null) {
                results.add(RecordResult.builder()
                        .rowNumber(0).status("FAILED")
                        .reason(validationError)
                        .build());
                return buildResult(0, 0, 1, results);
            }

            String line;
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.trim().isEmpty()) continue;

                try {
                    String[] fields = parseCsvLine(line);
                    RecordResult result = processRow(rowNumber, fields);
                    results.add(result);
                    if ("SUCCESS".equals(result.getStatus())) {
                        successCount++;
                    } else {
                        failureCount++;
                    }
                } catch (Exception e) {
                    failureCount++;
                    results.add(RecordResult.builder()
                            .rowNumber(rowNumber).status("FAILED")
                            .reason("Unexpected error: " + e.getMessage())
                            .build());
                }
            }

        } catch (Exception e) {
            log.error("Failed to process bulk upload file", e);
            results.add(RecordResult.builder()
                    .rowNumber(0).status("FAILED")
                    .reason("Failed to read file: " + e.getMessage())
                    .build());
            failureCount++;
        }

        return buildResult(rowNumber, successCount, failureCount, results);
    }

    private String validateHeader(String headerLine) {
        String[] headers = parseCsvLine(headerLine);
        if (headers.length < EXPECTED_HEADERS.length) {
            return "Invalid header. Expected " + EXPECTED_HEADERS.length + " columns but found " + headers.length +
                    ". Expected: " + String.join(", ", EXPECTED_HEADERS);
        }
        for (int i = 0; i < EXPECTED_HEADERS.length; i++) {
            if (!EXPECTED_HEADERS[i].equalsIgnoreCase(headers[i].trim())) {
                return "Invalid header at column " + (i + 1) + ". Expected '" + EXPECTED_HEADERS[i] +
                        "' but found '" + headers[i].trim() + "'";
            }
        }
        return null;
    }

    private RecordResult processRow(int rowNumber, String[] fields) {
        // Validate minimum fields
        if (fields.length < 4) {
            return RecordResult.builder()
                    .rowNumber(rowNumber).status("FAILED")
                    .reason("Insufficient columns. At least Project Code, Project Name, Project Type, and Billability Category are required")
                    .build();
        }

        String projectCode = getField(fields, 0);
        String projectName = getField(fields, 1);
        String description = getField(fields, 2);
        String projectTypeStr = getField(fields, 3);
        String billabilityStr = getField(fields, 4);
        String statusStr = getField(fields, 5);
        String clientName = getField(fields, 6);
        String startDateStr = getField(fields, 7);
        String endDateStr = getField(fields, 8);
        String budgetStr = getField(fields, 9);
        String currencyStr = getField(fields, 10);

        // Validate required fields
        List<String> errors = new ArrayList<>();

        if (projectCode == null || projectCode.isEmpty()) {
            errors.add("Project Code is required");
        }
        if (projectName == null || projectName.isEmpty()) {
            errors.add("Project Name is required");
        }
        if (projectTypeStr == null || projectTypeStr.isEmpty()) {
            errors.add("Project Type is required");
        }
        if (billabilityStr == null || billabilityStr.isEmpty()) {
            errors.add("Billability Category is required");
        }

        if (!errors.isEmpty()) {
            return RecordResult.builder()
                    .rowNumber(rowNumber).projectCode(projectCode).projectName(projectName)
                    .status("FAILED").reason(String.join("; ", errors))
                    .build();
        }

        // Validate enum values
        ProjectType projectType;
        try {
            projectType = ProjectType.valueOf(projectTypeStr.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            errors.add("Invalid Project Type: '" + projectTypeStr + "'. Valid values: FIXED_PRICE, TIME_AND_MATERIAL, RETAINER, INTERNAL, SUPPORT");
        }

        BillabilityCategory billabilityCategory;
        try {
            billabilityCategory = BillabilityCategory.valueOf(billabilityStr.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            errors.add("Invalid Billability Category: '" + billabilityStr + "'. Valid values: BILLABLE, NON_BILLABLE, INTERNAL, INVESTMENT");
        }

        ProjectStatus status = ProjectStatus.PROPOSED;
        if (statusStr != null && !statusStr.isEmpty()) {
            try {
                status = ProjectStatus.valueOf(statusStr.toUpperCase().trim());
            } catch (IllegalArgumentException e) {
                errors.add("Invalid Status: '" + statusStr + "'. Valid values: PROPOSED, ACTIVE, ON_HOLD, COMPLETED, CANCELLED");
            }
        }

        // Validate dates (supports multiple formats: yyyy-MM-dd, MM/dd/yyyy, dd/MM/yyyy, dd-MM-yyyy, etc.)
        LocalDate startDate = null;
        if (startDateStr != null && !startDateStr.isEmpty()) {
            startDate = parseDate(startDateStr.trim());
            if (startDate == null) {
                errors.add("Invalid Start Date format: '" + startDateStr + "'. Accepted formats: yyyy-MM-dd, MM/dd/yyyy, dd/MM/yyyy, dd-MM-yyyy, dd.MM.yyyy, dd MMM yyyy");
            }
        }

        LocalDate endDate = null;
        if (endDateStr != null && !endDateStr.isEmpty()) {
            endDate = parseDate(endDateStr.trim());
            if (endDate == null) {
                errors.add("Invalid End Date format: '" + endDateStr + "'. Accepted formats: yyyy-MM-dd, MM/dd/yyyy, dd/MM/yyyy, dd-MM-yyyy, dd.MM.yyyy, dd MMM yyyy");
            }
        }

        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            errors.add("End Date cannot be before Start Date");
        }

        // Validate budget
        BigDecimal budget = null;
        if (budgetStr != null && !budgetStr.isEmpty()) {
            try {
                budget = new BigDecimal(budgetStr.trim());
                if (budget.compareTo(BigDecimal.ZERO) < 0) {
                    errors.add("Budget cannot be negative");
                }
            } catch (NumberFormatException e) {
                errors.add("Invalid Budget: '" + budgetStr + "'. Must be a number");
            }
        }

        if (!errors.isEmpty()) {
            return RecordResult.builder()
                    .rowNumber(rowNumber).projectCode(projectCode).projectName(projectName)
                    .status("FAILED").reason(String.join("; ", errors))
                    .build();
        }

        // Check duplicate project code
        if (projectRepository.existsByProjectCode(projectCode)) {
            return RecordResult.builder()
                    .rowNumber(rowNumber).projectCode(projectCode).projectName(projectName)
                    .status("FAILED").reason("Project code already exists: " + projectCode)
                    .build();
        }

        // Build and save project
        String currency = (currencyStr != null && !currencyStr.isEmpty()) ? currencyStr.trim().toUpperCase() : "USD";

        Project project = Project.builder()
                .projectCode(projectCode)
                .name(projectName)
                .description(description)
                .projectType(ProjectType.valueOf(projectTypeStr.toUpperCase().trim()))
                .billabilityCategory(BillabilityCategory.valueOf(billabilityStr.toUpperCase().trim()))
                .status(status)
                .clientName(clientName)
                .startDate(startDate)
                .endDate(endDate)
                .budget(budget)
                .currency(currency)
                .build();

        projectRepository.save(project);

        return RecordResult.builder()
                .rowNumber(rowNumber).projectCode(projectCode).projectName(projectName)
                .status("SUCCESS").reason("Project created successfully")
                .build();
    }

    /**
     * Attempt to parse a date string using multiple common formats.
     * Returns null if no format matches.
     */
    private LocalDate parseDate(String dateStr) {
        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                return LocalDate.parse(dateStr, formatter);
            } catch (DateTimeParseException ignored) {
                // Try next format
            }
        }
        return null;
    }

    private String getField(String[] fields, int index) {
        if (index >= fields.length) return null;
        String val = fields[index].trim();
        // Remove surrounding quotes
        if (val.startsWith("\"") && val.endsWith("\"")) {
            val = val.substring(1, val.length() - 1);
        }
        return val.isEmpty() ? null : val;
    }

    /**
     * Simple CSV line parser that handles quoted fields with commas.
     */
    private String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString().trim());

        return fields.toArray(new String[0]);
    }

    private BulkUploadResultDTO buildResult(int total, int success, int failure, List<RecordResult> results) {
        return BulkUploadResultDTO.builder()
                .totalRecords(total)
                .successCount(success)
                .failureCount(failure)
                .results(results)
                .build();
    }
}
