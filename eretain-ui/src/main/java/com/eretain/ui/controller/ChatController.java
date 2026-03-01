package com.eretain.ui.controller;

import com.eretain.ui.service.ApiService;
import com.eretain.ui.service.ChatService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for the AI Chat feature.
 * Restricted to ADMINISTRATOR and PMO users.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;
    private final ApiService apiService;

    /**
     * Chat page (full page view).
     */
    @GetMapping("/chat")
    public String chatPage(Authentication authentication, Model model) {
        verifyAccess(authentication);
        model.addAttribute("activeMenu", "chat");
        return "chat";
    }

    /**
     * Send a message to the AI assistant (AJAX endpoint).
     */
    @PostMapping("/chat/send")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendMessage(
            @RequestBody Map<String, String> request,
            Authentication authentication,
            HttpServletRequest httpRequest,
            HttpSession session) {

        verifyAccess(authentication);

        String userMessage = request.get("message");
        if (userMessage == null || userMessage.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Message cannot be empty"
            ));
        }

        String token = apiService.extractToken(httpRequest);
        String sessionId = session.getId() + "_" + authentication.getName();

        try {
            String response = chatService.chat(sessionId, userMessage, token);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", response
            ));
        } catch (Exception e) {
            log.error("Chat error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "An error occurred. Please try again."
            ));
        }
    }

    /**
     * Clear conversation history (AJAX endpoint).
     */
    @PostMapping("/chat/clear")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> clearHistory(
            Authentication authentication,
            HttpSession session) {

        verifyAccess(authentication);
        String sessionId = session.getId() + "_" + authentication.getName();
        chatService.clearHistory(sessionId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Conversation cleared"
        ));
    }

    private void verifyAccess(Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR"));
        boolean isPmo = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_PMO"));
        if (!isAdmin && !isPmo) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied: Chat is only available for Admin and PMO users");
        }
    }
}
