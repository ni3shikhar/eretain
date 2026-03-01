package com.eretain.ui.controller;

import com.eretain.ui.service.ApiService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Controller
public class AuthController {

    private final ApiService apiService;

    public AuthController(ApiService apiService) {
        this.apiService = apiService;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        HttpServletResponse response,
                        Model model) {
        try {
            Map<String, Object> result = apiService.login(username, password);

            if (apiService.isSuccess(result)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) apiService.extractData(result);
                if (data != null && data.containsKey("token")) {
                    String token = (String) data.get("token");

                    Cookie cookie = new Cookie("JWT_TOKEN", token);
                    cookie.setPath("/");
                    cookie.setHttpOnly(true);
                    cookie.setMaxAge(24 * 60 * 60); // 24 hours
                    response.addCookie(cookie);

                    return "redirect:/dashboard";
                }
            }

            String errorMsg = apiService.getErrorMessage(result);
            model.addAttribute("error", errorMsg != null ? errorMsg : "Invalid username or password");
            return "login";

        } catch (Exception e) {
            model.addAttribute("error", "Unable to connect to authentication service. Please try again later.");
            return "login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("JWT_TOKEN", null);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return "redirect:/login?logout";
    }
}
