package com.eretain.ui.controller;

import com.eretain.ui.service.ApiService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/rate-cards")
public class RateCardController {

    private final ApiService apiService;

    public RateCardController(ApiService apiService) {
        this.apiService = apiService;
    }

    @GetMapping
    @SuppressWarnings("unchecked")
    public String listRateCards(HttpServletRequest request, Model model) {
        String token = apiService.extractToken(request);
        try {
            Map<String, Object> result = apiService.get("/api/auth/rate-cards", token);
            if (apiService.isSuccess(result)) {
                List<Map<String, Object>> rateCards = (List<Map<String, Object>>) apiService.extractData(result);
                // Filter out soft-deleted entries
                if (rateCards != null) {
                    rateCards = rateCards.stream()
                            .filter(rc -> Boolean.TRUE.equals(rc.get("active")))
                            .toList();
                }
                model.addAttribute("rateCards", rateCards);
            }
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Unable to load rate cards");
        }
        model.addAttribute("activeMenu", "rate-cards");
        return "rate-cards/list";
    }

    @GetMapping("/new")
    public String newRateCard(Model model) {
        model.addAttribute("rateCard", new HashMap<String, Object>());
        model.addAttribute("isEdit", false);
        model.addAttribute("activeMenu", "rate-cards");
        return "rate-cards/form";
    }

    @GetMapping("/{id}")
    @SuppressWarnings("unchecked")
    public String viewRateCard(@PathVariable Long id, HttpServletRequest request, Model model) {
        String token = apiService.extractToken(request);
        try {
            Map<String, Object> result = apiService.get("/api/auth/rate-cards/" + id, token);
            if (apiService.isSuccess(result)) {
                Map<String, Object> rateCard = (Map<String, Object>) apiService.extractData(result);
                model.addAttribute("rateCard", rateCard);
                model.addAttribute("activeMenu", "rate-cards");
                return "rate-cards/view";
            }
        } catch (Exception e) {
            // fall through
        }
        return "redirect:/rate-cards";
    }

    @GetMapping("/{id}/edit")
    @SuppressWarnings("unchecked")
    public String editRateCard(@PathVariable Long id, HttpServletRequest request, Model model) {
        String token = apiService.extractToken(request);
        try {
            Map<String, Object> result = apiService.get("/api/auth/rate-cards/" + id, token);
            if (apiService.isSuccess(result)) {
                model.addAttribute("rateCard", apiService.extractData(result));
                model.addAttribute("isEdit", true);
                model.addAttribute("activeMenu", "rate-cards");
                return "rate-cards/form";
            }
        } catch (Exception e) {
            // fall through
        }
        return "redirect:/rate-cards";
    }

    @PostMapping
    public String saveRateCard(@RequestParam Map<String, String> formData,
                               HttpServletRequest request,
                               RedirectAttributes redirectAttributes) {
        String token = apiService.extractToken(request);
        Map<String, Object> payload = new HashMap<>();
        payload.put("roleName", formData.get("roleName"));
        payload.put("audaxRate", formData.get("audaxRate"));
        payload.put("fixedFeeRate", formData.get("fixedFeeRate"));
        payload.put("tmRate", formData.get("tmRate"));
        payload.put("currency", formData.getOrDefault("currency", "USD"));
        payload.put("description", formData.get("description"));

        try {
            String id = formData.get("id");
            Map<String, Object> result;
            if (id != null && !id.isEmpty()) {
                result = apiService.put("/api/auth/rate-cards/" + id, payload, token);
            } else {
                result = apiService.post("/api/auth/rate-cards", payload, token);
            }

            if (apiService.isSuccess(result)) {
                redirectAttributes.addFlashAttribute("successMessage", "Rate card saved successfully");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", apiService.getErrorMessage(result));
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to save rate card: " + e.getMessage());
        }
        return "redirect:/rate-cards";
    }

    @PostMapping("/{id}/delete")
    public String deleteRateCard(@PathVariable Long id,
                                 HttpServletRequest request,
                                 RedirectAttributes redirectAttributes) {
        String token = apiService.extractToken(request);
        try {
            Map<String, Object> result = apiService.delete("/api/auth/rate-cards/" + id, token);
            if (apiService.isSuccess(result)) {
                redirectAttributes.addFlashAttribute("successMessage", "Rate card deleted successfully");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", apiService.getErrorMessage(result));
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete rate card");
        }
        return "redirect:/rate-cards";
    }
}
