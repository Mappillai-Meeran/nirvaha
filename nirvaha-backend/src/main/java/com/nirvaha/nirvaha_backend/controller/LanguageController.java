package com.nirvaha.nirvaha_backend.controller;

import com.nirvaha.nirvaha_backend.service.LanguageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/language")
public class LanguageController {

    private final LanguageService languageService;

    public LanguageController(LanguageService languageService) {
        this.languageService = languageService;
    }

    @GetMapping("/supported")
    public ResponseEntity<Map<String, Object>> getSupportedLanguages() {
        Map<String, Object> response = new HashMap<>();
        response.put("supportedLanguages", languageService.getSupportedLanguages());
        response.put("defaultLanguage", "EN");
        response.put("modelVersion", LanguageService.LANGUAGE_MODEL_VERSION);
        response.put("provider", LanguageService.PROVIDER_LOCAL_RULES);

        Map<String, String> names = new HashMap<>();
        names.put("EN", "English");
        names.put("TA", "தமிழ் (Tamil)");
        names.put("HI", "हिन्दी (Hindi)");
        names.put("ML", "മലയാളം (Malayalam)");
        names.put("TE", "తెలుగు (Telugu)");
        response.put("languageNames", names);

        return ResponseEntity.ok(response);
    }

    public static class DetectLanguageRequest {
        private String text;
        private String preferredLanguage;

        public DetectLanguageRequest() {
        }

        public DetectLanguageRequest(String text, String preferredLanguage) {
            this.text = text;
            this.preferredLanguage = preferredLanguage;
        }

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public String getPreferredLanguage() { return preferredLanguage; }
        public void setPreferredLanguage(String preferredLanguage) { this.preferredLanguage = preferredLanguage; }
    }

    @PostMapping("/detect")
    public ResponseEntity<LanguageService.LanguageDetectionResult> detectLanguage(@RequestBody DetectLanguageRequest request) {
        LanguageService.LanguageDetectionResult result = languageService.processText(
                request.getText(), request.getPreferredLanguage()
        );
        return ResponseEntity.ok(result);
    }
}
