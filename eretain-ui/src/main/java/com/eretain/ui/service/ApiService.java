package com.eretain.ui.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.*;

/**
 * Generic API service to communicate with backend microservices through the API Gateway.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApiService {

    private final WebClient apiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * GET request returning parsed response data.
     */
    public Map<String, Object> get(String uri, String token) {
        try {
            return apiClient.get()
                    .uri(uri)
                    .headers(h -> h.setBearerAuth(token))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (WebClientResponseException e) {
            log.error("GET {} failed: {} - {}", uri, e.getStatusCode(), e.getResponseBodyAsString());
            return errorResponse(e.getMessage());
        } catch (Exception e) {
            log.error("GET {} failed: {}", uri, e.getMessage());
            return errorResponse(e.getMessage());
        }
    }

    /**
     * POST request with body.
     */
    public Map<String, Object> post(String uri, Object body, String token) {
        try {
            var bodySpec = apiClient.post()
                    .uri(uri)
                    .headers(h -> h.setBearerAuth(token));
            if (body != null) {
                return bodySpec.contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(body)
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                        .block();
            }
            return bodySpec.retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (WebClientResponseException e) {
            log.error("POST {} failed: {} - {}", uri, e.getStatusCode(), e.getResponseBodyAsString());
            return parseErrorResponse(e);
        } catch (Exception e) {
            log.error("POST {} failed: {}", uri, e.getMessage());
            return errorResponse(e.getMessage());
        }
    }

    /**
     * PUT request with body.
     */
    public Map<String, Object> put(String uri, Object body, String token) {
        try {
            return apiClient.put()
                    .uri(uri)
                    .headers(h -> h.setBearerAuth(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (WebClientResponseException e) {
            log.error("PUT {} failed: {} - {}", uri, e.getStatusCode(), e.getResponseBodyAsString());
            return parseErrorResponse(e);
        } catch (Exception e) {
            log.error("PUT {} failed: {}", uri, e.getMessage());
            return errorResponse(e.getMessage());
        }
    }

    /**
     * DELETE request.
     */
    public Map<String, Object> delete(String uri, String token) {
        try {
            return apiClient.delete()
                    .uri(uri)
                    .headers(h -> h.setBearerAuth(token))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (WebClientResponseException e) {
            log.error("DELETE {} failed: {} - {}", uri, e.getStatusCode(), e.getResponseBodyAsString());
            return errorResponse(e.getMessage());
        } catch (Exception e) {
            log.error("DELETE {} failed: {}", uri, e.getMessage());
            return errorResponse(e.getMessage());
        }
    }

    /**
     * POST multipart file upload request.
     */
    public Map<String, Object> uploadFile(String uri, MultipartFile file, String token) {
        try {
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("file", file.getResource());

            return apiClient.post()
                    .uri(uri)
                    .headers(h -> h.setBearerAuth(token))
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (WebClientResponseException e) {
            log.error("UPLOAD {} failed: {} - {}", uri, e.getStatusCode(), e.getResponseBodyAsString());
            return parseErrorResponse(e);
        } catch (Exception e) {
            log.error("UPLOAD {} failed: {}", uri, e.getMessage());
            return errorResponse(e.getMessage());
        }
    }

    /**
     * GET request returning raw byte array (for file downloads).
     */
    public byte[] getBytes(String uri, String token) {
        return apiClient.get()
                .uri(uri)
                .headers(h -> h.setBearerAuth(token))
                .retrieve()
                .bodyToMono(byte[].class)
                .block();
    }

    /**
     * Login request (no auth header needed).
     */
    public Map<String, Object> login(String username, String password) {
        try {
            Map<String, String> body = Map.of("username", username, "password", password);
            return apiClient.post()
                    .uri("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (WebClientResponseException e) {
            log.error("Login failed: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return parseErrorResponse(e);
        } catch (Exception e) {
            log.error("Login failed: {}", e.getMessage());
            return errorResponse("Connection failed. Please check if backend services are running.");
        }
    }

    /**
     * Extract JWT token from the HTTP request's JWT_TOKEN cookie.
     */
    public String extractToken(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("JWT_TOKEN".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public Object extractData(Map<String, Object> response) {
        if (response == null) return null;
        return response.get("data");
    }

    public boolean isSuccess(Map<String, Object> response) {
        if (response == null) return false;
        Object success = response.get("success");
        return Boolean.TRUE.equals(success);
    }

    public String getErrorMessage(Map<String, Object> response) {
        if (response == null) return "No response from server";
        Object message = response.get("message");
        return message != null ? message.toString() : "Unknown error";
    }

    private Map<String, Object> errorResponse(String message) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", false);
        resp.put("message", message);
        return resp;
    }

    private Map<String, Object> parseErrorResponse(WebClientResponseException e) {
        try {
            return objectMapper.readValue(e.getResponseBodyAsString(),
                    objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, Object.class));
        } catch (Exception ex) {
            return errorResponse(e.getMessage());
        }
    }
}
