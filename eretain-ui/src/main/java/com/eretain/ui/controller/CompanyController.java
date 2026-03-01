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
@RequestMapping("/company")
public class CompanyController {

    private final ApiService apiService;

    public CompanyController(ApiService apiService) {
        this.apiService = apiService;
    }

    @GetMapping
    @SuppressWarnings("unchecked")
    public String companyIndex(HttpServletRequest request, Model model) {
        String token = apiService.extractToken(request);
        try {
            Map<String, Object> buResult = apiService.get("/api/company/business-units", token);
            if (apiService.isSuccess(buResult)) {
                Object data = apiService.extractData(buResult);
                model.addAttribute("businessUnits", data);
                model.addAttribute("buCount", data instanceof List ? ((List<?>) data).size() : 0);
            }
        } catch (Exception e) {
            model.addAttribute("buCount", 0);
        }
        try {
            Map<String, Object> unitResult = apiService.get("/api/company/units", token);
            if (apiService.isSuccess(unitResult)) {
                Object data = apiService.extractData(unitResult);
                model.addAttribute("unitCount", data instanceof List ? ((List<?>) data).size() : 0);
            }
        } catch (Exception e) {
            model.addAttribute("unitCount", 0);
        }
        try {
            Map<String, Object> duResult = apiService.get("/api/company/delivery-units", token);
            if (apiService.isSuccess(duResult)) {
                Object data = apiService.extractData(duResult);
                model.addAttribute("duCount", data instanceof List ? ((List<?>) data).size() : 0);
            }
        } catch (Exception e) {
            model.addAttribute("duCount", 0);
        }
        model.addAttribute("activeMenu", "company");
        return "company/index";
    }

    // ======== Business Units ========

    @GetMapping("/business-units")
    @SuppressWarnings("unchecked")
    public String listBusinessUnits(HttpServletRequest request, Model model) {
        String token = apiService.extractToken(request);
        try {
            Map<String, Object> result = apiService.get("/api/company/business-units", token);
            if (apiService.isSuccess(result)) {
                model.addAttribute("businessUnits", apiService.extractData(result));
            }
        } catch (Exception e) {
            model.addAttribute("error", "Unable to load business units");
        }
        return "company/business-units";
    }

    @GetMapping("/business-units/new")
    public String newBusinessUnit(Model model) {
        model.addAttribute("businessUnit", new HashMap<String, Object>());
        model.addAttribute("isEdit", false);
        return "company/business-unit-form";
    }

    @GetMapping("/business-units/{id}/edit")
    public String editBusinessUnit(@PathVariable Long id, HttpServletRequest request, Model model) {
        String token = apiService.extractToken(request);
        try {
            Map<String, Object> result = apiService.get("/api/company/business-units/" + id, token);
            if (apiService.isSuccess(result)) {
                model.addAttribute("businessUnit", apiService.extractData(result));
                model.addAttribute("isEdit", true);
                return "company/business-unit-form";
            }
        } catch (Exception e) {
            // fall through
        }
        return "redirect:/company/business-units";
    }

    @PostMapping("/business-units")
    public String saveBusinessUnit(@RequestParam Map<String, String> formData,
                                   HttpServletRequest request,
                                   RedirectAttributes redirectAttributes) {
        String token = apiService.extractToken(request);
        Map<String, Object> payload = new HashMap<>(formData);

        try {
            String id = formData.get("id");
            Map<String, Object> result;
            if (id != null && !id.isEmpty()) {
                result = apiService.put("/api/company/business-units/" + id, payload, token);
            } else {
                payload.remove("id");
                result = apiService.post("/api/company/business-units", payload, token);
            }

            if (apiService.isSuccess(result)) {
                redirectAttributes.addFlashAttribute("success", "Business unit saved successfully");
            } else {
                redirectAttributes.addFlashAttribute("error", apiService.getErrorMessage(result));
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to save business unit");
        }
        return "redirect:/company/business-units";
    }

    @PostMapping("/business-units/{id}/delete")
    public String deleteBusinessUnit(@PathVariable Long id,
                                     HttpServletRequest request,
                                     RedirectAttributes redirectAttributes) {
        String token = apiService.extractToken(request);
        try {
            apiService.delete("/api/company/business-units/" + id, token);
            redirectAttributes.addFlashAttribute("success", "Business unit deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete business unit");
        }
        return "redirect:/company/business-units";
    }

    // ======== Units ========

    @GetMapping("/units")
    @SuppressWarnings("unchecked")
    public String listUnits(HttpServletRequest request, Model model) {
        String token = apiService.extractToken(request);
        try {
            Map<String, Object> result = apiService.get("/api/company/units", token);
            if (apiService.isSuccess(result)) {
                model.addAttribute("units", apiService.extractData(result));
            }
        } catch (Exception e) {
            model.addAttribute("error", "Unable to load units");
        }
        return "company/units";
    }

    @GetMapping("/units/new")
    public String newUnit(HttpServletRequest request, Model model) {
        String token = apiService.extractToken(request);
        model.addAttribute("unit", new HashMap<String, Object>());
        model.addAttribute("isEdit", false);
        loadBusinessUnitsForSelect(token, model);
        return "company/unit-form";
    }

    @GetMapping("/units/{id}/edit")
    public String editUnit(@PathVariable Long id, HttpServletRequest request, Model model) {
        String token = apiService.extractToken(request);
        try {
            Map<String, Object> result = apiService.get("/api/company/units/" + id, token);
            if (apiService.isSuccess(result)) {
                model.addAttribute("unit", apiService.extractData(result));
                model.addAttribute("isEdit", true);
                loadBusinessUnitsForSelect(token, model);
                return "company/unit-form";
            }
        } catch (Exception e) {
            // fall through
        }
        return "redirect:/company/units";
    }

    @PostMapping("/units")
    public String saveUnit(@RequestParam Map<String, String> formData,
                           HttpServletRequest request,
                           RedirectAttributes redirectAttributes) {
        String token = apiService.extractToken(request);
        Map<String, Object> payload = new HashMap<>(formData);

        try {
            String id = formData.get("id");
            Map<String, Object> result;
            if (id != null && !id.isEmpty()) {
                result = apiService.put("/api/company/units/" + id, payload, token);
            } else {
                payload.remove("id");
                result = apiService.post("/api/company/units", payload, token);
            }

            if (apiService.isSuccess(result)) {
                redirectAttributes.addFlashAttribute("success", "Unit saved successfully");
            } else {
                redirectAttributes.addFlashAttribute("error", apiService.getErrorMessage(result));
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to save unit");
        }
        return "redirect:/company/units";
    }

    @PostMapping("/units/{id}/delete")
    public String deleteUnit(@PathVariable Long id,
                             HttpServletRequest request,
                             RedirectAttributes redirectAttributes) {
        String token = apiService.extractToken(request);
        try {
            apiService.delete("/api/company/units/" + id, token);
            redirectAttributes.addFlashAttribute("success", "Unit deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete unit");
        }
        return "redirect:/company/units";
    }

    // ======== Delivery Units ========

    @GetMapping("/delivery-units")
    @SuppressWarnings("unchecked")
    public String listDeliveryUnits(HttpServletRequest request, Model model) {
        String token = apiService.extractToken(request);
        try {
            Map<String, Object> result = apiService.get("/api/company/delivery-units", token);
            if (apiService.isSuccess(result)) {
                model.addAttribute("deliveryUnits", apiService.extractData(result));
            }
        } catch (Exception e) {
            model.addAttribute("error", "Unable to load delivery units");
        }
        return "company/delivery-units";
    }

    @GetMapping("/delivery-units/new")
    public String newDeliveryUnit(HttpServletRequest request, Model model) {
        String token = apiService.extractToken(request);
        model.addAttribute("deliveryUnit", new HashMap<String, Object>());
        model.addAttribute("isEdit", false);
        loadUnitsForSelect(token, model);
        return "company/delivery-unit-form";
    }

    @GetMapping("/delivery-units/{id}/edit")
    public String editDeliveryUnit(@PathVariable Long id, HttpServletRequest request, Model model) {
        String token = apiService.extractToken(request);
        try {
            Map<String, Object> result = apiService.get("/api/company/delivery-units/" + id, token);
            if (apiService.isSuccess(result)) {
                model.addAttribute("deliveryUnit", apiService.extractData(result));
                model.addAttribute("isEdit", true);
                loadUnitsForSelect(token, model);
                return "company/delivery-unit-form";
            }
        } catch (Exception e) {
            // fall through
        }
        return "redirect:/company/delivery-units";
    }

    @PostMapping("/delivery-units")
    public String saveDeliveryUnit(@RequestParam Map<String, String> formData,
                                   HttpServletRequest request,
                                   RedirectAttributes redirectAttributes) {
        String token = apiService.extractToken(request);
        Map<String, Object> payload = new HashMap<>(formData);

        try {
            String id = formData.get("id");
            Map<String, Object> result;
            if (id != null && !id.isEmpty()) {
                result = apiService.put("/api/company/delivery-units/" + id, payload, token);
            } else {
                payload.remove("id");
                result = apiService.post("/api/company/delivery-units", payload, token);
            }

            if (apiService.isSuccess(result)) {
                redirectAttributes.addFlashAttribute("success", "Delivery unit saved successfully");
            } else {
                redirectAttributes.addFlashAttribute("error", apiService.getErrorMessage(result));
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to save delivery unit");
        }
        return "redirect:/company/delivery-units";
    }

    @PostMapping("/delivery-units/{id}/delete")
    public String deleteDeliveryUnit(@PathVariable Long id,
                                     HttpServletRequest request,
                                     RedirectAttributes redirectAttributes) {
        String token = apiService.extractToken(request);
        try {
            apiService.delete("/api/company/delivery-units/" + id, token);
            redirectAttributes.addFlashAttribute("success", "Delivery unit deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete delivery unit");
        }
        return "redirect:/company/delivery-units";
    }

    // ======== Helpers ========

    @SuppressWarnings("unchecked")
    private void loadBusinessUnitsForSelect(String token, Model model) {
        try {
            Map<String, Object> result = apiService.get("/api/company/business-units", token);
            if (apiService.isSuccess(result)) {
                model.addAttribute("businessUnits", apiService.extractData(result));
            }
        } catch (Exception e) {
            // empty list
        }
    }

    @SuppressWarnings("unchecked")
    private void loadUnitsForSelect(String token, Model model) {
        try {
            Map<String, Object> result = apiService.get("/api/company/units", token);
            if (apiService.isSuccess(result)) {
                model.addAttribute("units", apiService.extractData(result));
            }
        } catch (Exception e) {
            // empty list
        }
    }
}
