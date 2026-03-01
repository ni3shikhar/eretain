package com.eretain.ui.service;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.models.*;
import com.azure.core.credential.AzureKeyCredential;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service that integrates Azure OpenAI with eRetain data.
 * Uses function calling to query real project, allocation, timesheet, and utilization data.
 */
@Service
@Slf4j
public class ChatService {

    private final ApiService apiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${azure.openai.endpoint}")
    private String endpoint;

    @Value("${azure.openai.api-key}")
    private String apiKey;

    @Value("${azure.openai.deployment}")
    private String deployment;

    // Store conversation history per session
    private final Map<String, List<ChatRequestMessage>> conversationHistory = new ConcurrentHashMap<>();

    private OpenAIClient client;

    public ChatService(ApiService apiService) {
        this.apiService = apiService;
    }

    @PostConstruct
    public void init() {
        if (apiKey == null || apiKey.isBlank() || apiKey.equals("your-api-key-here")) {
            log.warn("Azure OpenAI API key is not configured. Chat functionality will be disabled.");
            this.client = null;
            return;
        }
        this.client = new OpenAIClientBuilder()
                .endpoint(endpoint)
                .credential(new AzureKeyCredential(apiKey))
                .buildClient();
        log.info("Azure OpenAI client initialized with endpoint: {}", endpoint);
    }

    /**
     * Process a user message and return the AI response.
     */
    public String chat(String sessionId, String userMessage, String token) {
        if (client == null) {
            return "Chat is currently unavailable. Azure OpenAI API key is not configured.";
        }
        // Get or create conversation history
        List<ChatRequestMessage> messages = conversationHistory.computeIfAbsent(sessionId, k -> {
            List<ChatRequestMessage> initial = new ArrayList<>();
            initial.add(new ChatRequestSystemMessage(getSystemPrompt()));
            return initial;
        });

        // Add user message
        messages.add(new ChatRequestUserMessage(userMessage));

        // Build options with function definitions
        ChatCompletionsOptions options = new ChatCompletionsOptions(new ArrayList<>(messages))
                .setTools(getToolDefinitions());

        try {
            // Call Azure OpenAI
            ChatCompletions completions = client.getChatCompletions(deployment, options);
            ChatChoice choice = completions.getChoices().get(0);

            // Handle function calls in a loop (may need multiple rounds)
            int maxIterations = 5;
            int iteration = 0;
            while (choice.getFinishReason() == CompletionsFinishReason.TOOL_CALLS && iteration < maxIterations) {
                iteration++;
                ChatResponseMessage responseMessage = choice.getMessage();

                // Add assistant message with tool calls
                ChatRequestAssistantMessage assistantMsg = new ChatRequestAssistantMessage("");
                assistantMsg.setToolCalls(responseMessage.getToolCalls());
                messages.add(assistantMsg);

                // Execute each tool call
                for (ChatCompletionsToolCall toolCall : responseMessage.getToolCalls()) {
                    if (toolCall instanceof ChatCompletionsFunctionToolCall functionCall) {
                        String functionName = functionCall.getFunction().getName();
                        String arguments = functionCall.getFunction().getArguments();
                        log.info("Executing function: {} with args: {}", functionName, arguments);

                        String result = executeFunctionCall(functionName, arguments, token);

                        // Add tool result
                        ChatRequestToolMessage toolMessage = new ChatRequestToolMessage(result, functionCall.getId());
                        messages.add(toolMessage);
                    }
                }

                // Call Azure OpenAI again with tool results
                options = new ChatCompletionsOptions(new ArrayList<>(messages))
                        .setTools(getToolDefinitions());
                completions = client.getChatCompletions(deployment, options);
                choice = completions.getChoices().get(0);
            }

            // Get final text response
            String response = choice.getMessage().getContent();
            if (response == null || response.isBlank()) {
                response = "I apologize, I couldn't generate a response. Please try rephrasing your question.";
            }

            // Add assistant response to history
            messages.add(new ChatRequestAssistantMessage(response));

            // Trim history if too long (keep system prompt + last 20 messages)
            if (messages.size() > 22) {
                ChatRequestMessage systemMsg = messages.get(0);
                List<ChatRequestMessage> trimmed = new ArrayList<>();
                trimmed.add(systemMsg);
                trimmed.addAll(messages.subList(messages.size() - 20, messages.size()));
                conversationHistory.put(sessionId, trimmed);
            }

            return response;
        } catch (Exception e) {
            log.error("Azure OpenAI chat error: {}", e.getMessage(), e);
            return "I'm sorry, I encountered an error processing your request. Please try again. (" + e.getMessage() + ")";
        }
    }

    /**
     * Clear conversation history for a session.
     */
    public void clearHistory(String sessionId) {
        conversationHistory.remove(sessionId);
    }

    private String getSystemPrompt() {
        return """
                You are eRetain AI Assistant — an intelligent helper for the eRetain resource management platform.
                You help Administrators and PMO users query and understand data about:
                - **Projects**: project names, statuses, start/end dates, business units, delivery units
                - **Allocations**: employee assignments to projects, roles, allocation percentages, statuses
                - **Timesheets**: employee time entries, hours logged, approval statuses
                - **Utilization**: employee utilization rates and summaries
                - **Rate Cards**: per-role hourly rates including Audax Rate, Fixed Fee Rate, and T&M Rate

                IMPORTANT GUIDELINES:
                1. Always use the available tools/functions to fetch real data before answering questions.
                2. Present data in a clear, formatted way using tables or lists.
                3. When showing employee or project data, include relevant details.
                4. If a query is ambiguous, ask for clarification.
                5. You can combine data from multiple sources to answer complex questions.
                6. Be concise but thorough.
                7. If you can't find the requested data, say so clearly.
                8. Format numbers and dates in a readable way.
                """;
    }

    private List<ChatCompletionsToolDefinition> getToolDefinitions() {
        List<ChatCompletionsToolDefinition> tools = new ArrayList<>();

        // 1. Get all projects
        tools.add(createFunction("get_all_projects",
                "Get a list of all projects with their details including name, status, dates, business unit, delivery unit",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "status", Map.of("type", "string", "description", "Optional filter by project status (ACTIVE, COMPLETED, ON_HOLD, CANCELLED)")
                        ),
                        "required", List.of()
                )));

        // 2. Get project by ID
        tools.add(createFunction("get_project_by_id",
                "Get details of a specific project by its ID",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "projectId", Map.of("type", "integer", "description", "The project ID")
                        ),
                        "required", List.of("projectId")
                )));

        // 3. Get all allocations
        tools.add(createFunction("get_all_allocations",
                "Get all employee allocations to projects including role, percentage, and status",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "status", Map.of("type", "string", "description", "Optional filter by allocation status")
                        ),
                        "required", List.of()
                )));

        // 4. Get allocations by project
        tools.add(createFunction("get_allocations_by_project",
                "Get all allocations for a specific project",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "projectId", Map.of("type", "integer", "description", "The project ID")
                        ),
                        "required", List.of("projectId")
                )));

        // 5. Get allocations by employee
        tools.add(createFunction("get_allocations_by_employee",
                "Get all allocations for a specific employee",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "employeeId", Map.of("type", "integer", "description", "The employee ID")
                        ),
                        "required", List.of("employeeId")
                )));

        // 6. Get all timesheets
        tools.add(createFunction("get_all_timesheets",
                "Get all timesheets with details including employee, hours, status, and dates",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "status", Map.of("type", "string", "description", "Optional filter by timesheet status (DRAFT, SUBMITTED, APPROVED, REJECTED)")
                        ),
                        "required", List.of()
                )));

        // 7. Get timesheets by employee
        tools.add(createFunction("get_timesheets_by_employee",
                "Get all timesheets for a specific employee",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "employeeId", Map.of("type", "integer", "description", "The employee ID")
                        ),
                        "required", List.of("employeeId")
                )));

        // 8. Get utilization report
        tools.add(createFunction("get_utilization_report",
                "Get utilization data and summary for an employee, showing how their time is distributed",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "employeeId", Map.of("type", "integer", "description", "The employee ID")
                        ),
                        "required", List.of("employeeId")
                )));

        // 9. Get employee names
        tools.add(createFunction("get_employee_names",
                "Get a mapping of employee IDs to their display names. Use this to resolve employee names from IDs.",
                Map.of(
                        "type", "object",
                        "properties", Map.of(),
                        "required", List.of()
                )));

        // 10. Get projects report
        tools.add(createFunction("get_projects_report",
                "Get a detailed projects report with aggregated data",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "status", Map.of("type", "string", "description", "Optional filter by project status")
                        ),
                        "required", List.of()
                )));

        // 11. Get allocations report
        tools.add(createFunction("get_allocations_report",
                "Get a detailed allocations report showing employee assignments across projects",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "projectId", Map.of("type", "integer", "description", "Optional filter by project ID"),
                                "employeeId", Map.of("type", "integer", "description", "Optional filter by employee ID")
                        ),
                        "required", List.of()
                )));

        // 12. Get timesheets report
        tools.add(createFunction("get_timesheets_report",
                "Get a detailed timesheets report",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "employeeId", Map.of("type", "integer", "description", "Optional filter by employee ID")
                        ),
                        "required", List.of()
                )));

        // 13. Get all rate cards
        tools.add(createFunction("get_all_rate_cards",
                "Get all role rate cards showing Audax Rate, Fixed Fee Rate, and T&M Rate per hour for each role",
                Map.of(
                        "type", "object",
                        "properties", Map.of(),
                        "required", List.of()
                )));

        return tools;
    }

    private ChatCompletionsToolDefinition createFunction(String name, String description, Map<String, Object> parameters) {
        try {
            String paramJson = objectMapper.writeValueAsString(parameters);
            ChatCompletionsFunctionToolDefinitionFunction funcDef =
                    new ChatCompletionsFunctionToolDefinitionFunction(name);
            funcDef.setDescription(description);
            funcDef.setParameters(com.azure.core.util.BinaryData.fromString(paramJson));
            return new ChatCompletionsFunctionToolDefinition(funcDef);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create function definition: " + name, e);
        }
    }

    @SuppressWarnings("unchecked")
    private String executeFunctionCall(String functionName, String arguments, String token) {
        try {
            Map<String, Object> args = objectMapper.readValue(
                    arguments, new TypeReference<Map<String, Object>>() {});

            Object result = switch (functionName) {
                case "get_all_projects" -> {
                    String status = (String) args.get("status");
                    String uri = status != null ? "/api/projects?status=" + status : "/api/projects?page=0&size=100";
                    yield fetchData(uri, token);
                }
                case "get_project_by_id" -> {
                    int projectId = ((Number) args.get("projectId")).intValue();
                    yield fetchData("/api/projects/" + projectId, token);
                }
                case "get_all_allocations" -> {
                    String uri = "/api/allocations";
                    yield fetchData(uri, token);
                }
                case "get_allocations_by_project" -> {
                    int projectId = ((Number) args.get("projectId")).intValue();
                    yield fetchData("/api/allocations/project/" + projectId, token);
                }
                case "get_allocations_by_employee" -> {
                    int employeeId = ((Number) args.get("employeeId")).intValue();
                    yield fetchData("/api/allocations/employee/" + employeeId, token);
                }
                case "get_all_timesheets" -> {
                    String status = (String) args.get("status");
                    String uri = status != null ? "/api/timesheets?status=" + status + "&page=0&size=100" : "/api/timesheets?page=0&size=100";
                    yield fetchData(uri, token);
                }
                case "get_timesheets_by_employee" -> {
                    int employeeId = ((Number) args.get("employeeId")).intValue();
                    yield fetchData("/api/timesheets/employee/" + employeeId, token);
                }
                case "get_utilization_report" -> {
                    int employeeId = ((Number) args.get("employeeId")).intValue();
                    yield fetchData("/api/reports/utilization/" + employeeId, token);
                }
                case "get_employee_names" -> {
                    yield fetchData("/api/auth/users/names", token);
                }
                case "get_projects_report" -> {
                    String status = (String) args.get("status");
                    String uri = status != null ? "/api/reports/projects?status=" + status : "/api/reports/projects";
                    yield fetchData(uri, token);
                }
                case "get_allocations_report" -> {
                    StringBuilder uri = new StringBuilder("/api/reports/allocations?");
                    if (args.containsKey("projectId")) {
                        uri.append("projectId=").append(((Number) args.get("projectId")).intValue()).append("&");
                    }
                    if (args.containsKey("employeeId")) {
                        uri.append("employeeId=").append(((Number) args.get("employeeId")).intValue()).append("&");
                    }
                    yield fetchData(uri.toString(), token);
                }
                case "get_timesheets_report" -> {
                    StringBuilder uri = new StringBuilder("/api/reports/timesheets?");
                    if (args.containsKey("employeeId")) {
                        uri.append("employeeId=").append(((Number) args.get("employeeId")).intValue()).append("&");
                    }
                    yield fetchData(uri.toString(), token);
                }
                case "get_all_rate_cards" -> {
                    yield fetchData("/api/auth/rate-cards", token);
                }
                default -> Map.of("error", "Unknown function: " + functionName);
            };

            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.error("Function call execution failed for {}: {}", functionName, e.getMessage(), e);
            return "{\"error\": \"Failed to execute " + functionName + ": " + e.getMessage() + "\"}";
        }
    }

    private Object fetchData(String uri, String token) {
        try {
            Map<String, Object> response = apiService.get(uri, token);
            if (response == null) {
                return Map.of("error", "No response from API");
            }
            // If the response has a "data" field (standard API envelope), extract it
            if (response.containsKey("success") && response.containsKey("data")) {
                if (Boolean.TRUE.equals(response.get("success"))) {
                    return response.get("data");
                } else {
                    return Map.of("error", response.getOrDefault("message", "API returned failure"));
                }
            }
            // Otherwise return the raw response
            return response;
        } catch (Exception e) {
            log.error("Failed to fetch data from {}: {}", uri, e.getMessage());
            return Map.of("error", "Failed to fetch data: " + e.getMessage());
        }
    }
}
