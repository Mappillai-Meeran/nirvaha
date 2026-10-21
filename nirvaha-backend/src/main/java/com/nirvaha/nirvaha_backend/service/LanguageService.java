package com.nirvaha.nirvaha_backend.service;

import com.nirvaha.nirvaha_backend.entity.Incident;
import com.nirvaha.nirvaha_backend.entity.IncidentCategory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class LanguageService {

    public static final String LANGUAGE_MODEL_VERSION = "nirvaha-language-v1";
    public static final String PROVIDER_LOCAL_RULES = "LOCAL_RULES";

    public static class LanguageDetectionResult {
        private String detectedLanguage;
        private double confidence;
        private String originalText;
        private String normalizedText;
        private String normalizationStatus;
        private IncidentCategory suggestedCategory;
        private String provider;
        private String modelVersion;

        public LanguageDetectionResult(String detectedLanguage, double confidence, String originalText,
                                       String normalizedText, String normalizationStatus,
                                       IncidentCategory suggestedCategory, String provider, String modelVersion) {
            this.detectedLanguage = detectedLanguage;
            this.confidence = confidence;
            this.originalText = originalText;
            this.normalizedText = normalizedText;
            this.normalizationStatus = normalizationStatus;
            this.suggestedCategory = suggestedCategory;
            this.provider = provider;
            this.modelVersion = modelVersion;
        }

        public String getDetectedLanguage() { return detectedLanguage; }
        public double getConfidence() { return confidence; }
        public String getOriginalText() { return originalText; }
        public String getNormalizedText() { return normalizedText; }
        public String getNormalizationStatus() { return normalizationStatus; }
        public IncidentCategory getSuggestedCategory() { return suggestedCategory; }
        public String getProvider() { return provider; }
        public String getModelVersion() { return modelVersion; }
    }

    private static final Set<String> SUPPORTED_LANGUAGES = Set.of("EN", "TA", "HI", "ML", "TE");

    public LanguageDetectionResult processText(String text, String userPreferredLang) {
        if (text == null || text.trim().isEmpty()) {
            return new LanguageDetectionResult("EN", 1.0, "", "", "COMPLETED", IncidentCategory.OTHER, PROVIDER_LOCAL_RULES, LANGUAGE_MODEL_VERSION);
        }

        String lang = detectLanguage(text, userPreferredLang);
        double confidence = (userPreferredLang != null && SUPPORTED_LANGUAGES.contains(userPreferredLang.toUpperCase())) ? 1.0 : 0.92;

        IncidentCategory category = mapTextToCategory(text);
        String normalized = generateNormalizedText(text, lang, category);

        return new LanguageDetectionResult(lang, confidence, text, normalized, "COMPLETED", category, PROVIDER_LOCAL_RULES, LANGUAGE_MODEL_VERSION);
    }

    public String detectLanguage(String text) {
        return detectLanguage(text, null);
    }

    public String detectLanguage(String text, String userPreferredLang) {
        if (userPreferredLang != null && SUPPORTED_LANGUAGES.contains(userPreferredLang.trim().toUpperCase())) {
            return userPreferredLang.trim().toUpperCase();
        }

        int taCount = 0, hiCount = 0, mlCount = 0, teCount = 0;

        for (char c : text.toCharArray()) {
            if (c >= '\u0B80' && c <= '\u0BFF') taCount++;
            else if (c >= '\u0900' && c <= '\u097F') hiCount++;
            else if (c >= '\u0D00' && c <= '\u0D7F') mlCount++;
            else if (c >= '\u0C00' && c <= '\u0C7F') teCount++;
        }

        if (taCount > 0 && taCount >= hiCount && taCount >= mlCount && taCount >= teCount) return "TA";
        if (hiCount > 0 && hiCount >= taCount && hiCount >= mlCount && hiCount >= teCount) return "HI";
        if (mlCount > 0 && mlCount >= taCount && mlCount >= hiCount && mlCount >= teCount) return "ML";
        if (teCount > 0 && teCount >= taCount && teCount >= hiCount && teCount >= mlCount) return "TE";

        return "EN";
    }

    public IncidentCategory mapTextToCategory(String text) {
        if (text == null) return IncidentCategory.OTHER;
        String lower = text.toLowerCase();

        // Tamil Keywords
        if (lower.contains("வடிகால்") || lower.contains("தண்ணீர்") || lower.contains("தேங்கு") || lower.contains("சாக்கடை") || lower.contains("கழிவுநீர்") || lower.contains("மழைநீர்")) {
            return IncidentCategory.DRAINAGE;
        }
        if (lower.contains("குழி") || lower.contains("பள்ளம்") || lower.contains("சாலை") || lower.contains("தார்")) {
            return IncidentCategory.POTHOLE;
        }
        if (lower.contains("குப்பை") || lower.contains("கழிவு") || lower.contains("துப்புரவு")) {
            return IncidentCategory.GARBAGE;
        }
        if (lower.contains("விளக்கு") || lower.contains("தெருவிளக்கு") || lower.contains("மின்சாரம்") || lower.contains("இருள்")) {
            return IncidentCategory.STREETLIGHT;
        }
        if (lower.contains("குடிநீர்") || lower.contains("தண்ணீர் குழாய்") || lower.contains("நீர் விநியோகம்")) {
            return IncidentCategory.WATER_SUPPLY;
        }

        // Hindi Keywords
        if (lower.contains("जल निकासी") || lower.contains("नाली") || lower.contains("जलभराव") || lower.contains("बरसात का पानी")) {
            return IncidentCategory.DRAINAGE;
        }
        if (lower.contains("गड्ढा") || lower.contains("खड्ड") || lower.contains("सड़क क्षतिग्रस्त")) {
            return IncidentCategory.POTHOLE;
        }
        if (lower.contains("कचरा") || lower.contains("कूड़ा") || lower.contains("सफाई")) {
            return IncidentCategory.GARBAGE;
        }
        if (lower.contains("लाइट") || lower.contains("स्ट्रीटलाइट") || lower.contains("बिजली") || lower.contains("अंधेरा")) {
            return IncidentCategory.STREETLIGHT;
        }
        if (lower.contains("पेयजल") || lower.contains("पानी आपूर्ति") || lower.contains("नल टूट गया")) {
            return IncidentCategory.WATER_SUPPLY;
        }

        // Malayalam Keywords
        if (lower.contains("ഡ്രെയിനേജ്") || lower.contains("വെള്ളക്കെട്ട്") || lower.contains("ഓട")) {
            return IncidentCategory.DRAINAGE;
        }
        if (lower.contains("കുഴി") || lower.contains("റോഡ് തകർന്നു")) {
            return IncidentCategory.POTHOLE;
        }
        if (lower.contains("അഴുക്ക്") || lower.contains("മാലിന്യം") || lower.contains("ചവറ്")) {
            return IncidentCategory.GARBAGE;
        }
        if (lower.contains("സ്ട്രീറ്റ് ലൈറ്റ്") || lower.contains("വെളിച്ചം ഇല്ല")) {
            return IncidentCategory.STREETLIGHT;
        }
        if (lower.contains("കുടിവെള്ളം") || lower.contains("വാട്ടർ കണക്ഷൻ")) {
            return IncidentCategory.WATER_SUPPLY;
        }

        // Telugu Keywords
        if (lower.contains("డ్రైనేజ్") || lower.contains("వర్షపు నీరు") || lower.contains("కాలువ")) {
            return IncidentCategory.DRAINAGE;
        }
        if (lower.contains("గోతి") || lower.contains("గుంత") || lower.contains("రోడ్డు పాడైంది")) {
            return IncidentCategory.POTHOLE;
        }
        if (lower.contains("చెత్త") || lower.contains("మాలిన్యం")) {
            return IncidentCategory.GARBAGE;
        }
        if (lower.contains("వీధి ದీపం") || lower.contains("కరెంటు") || lower.contains("లైటు")) {
            return IncidentCategory.STREETLIGHT;
        }
        if (lower.contains("మంచినీరు") || lower.contains("తాగునీరు")) {
            return IncidentCategory.WATER_SUPPLY;
        }

        // English Keywords
        if (lower.contains("drain") || lower.contains("flood") || lower.contains("waterlog") || lower.contains("sewer")) {
            return IncidentCategory.DRAINAGE;
        }
        if (lower.contains("pothole") || lower.contains("crater") || lower.contains("road damage")) {
            return IncidentCategory.POTHOLE;
        }
        if (lower.contains("garbage") || lower.contains("waste") || lower.contains("trash") || lower.contains("dumping")) {
            return IncidentCategory.GARBAGE;
        }
        if (lower.contains("light") || lower.contains("lamp") || lower.contains("darkness") || lower.contains("street light")) {
            return IncidentCategory.STREETLIGHT;
        }
        if (lower.contains("water") || lower.contains("pipe") || lower.contains("leak") || lower.contains("supply")) {
            return IncidentCategory.WATER_SUPPLY;
        }

        return IncidentCategory.OTHER;
    }

    public String generateNormalizedText(String text, String lang, IncidentCategory category) {
        if ("EN".equals(lang)) {
            return text;
        }
        return String.format("[Normalized %s Representation]: Report regarding %s issue - \"%s\"", lang, category.name(), text);
    }

    public void processIncidentMultilingualSupport(Incident incident, String requestedLanguage) {
        if (incident == null) return;

        String textToProcess = incident.getDescription() != null ? incident.getDescription() : incident.getTitle();
        LanguageDetectionResult result = processText(textToProcess, requestedLanguage);

        incident.setOriginalDescription(incident.getDescription());
        incident.setOriginalLanguage(result.getDetectedLanguage());
        incident.setNormalizedDescription(result.getNormalizedText());
        incident.setNormalizationStatus(result.getNormalizationStatus());
        incident.setLanguageConfidence(result.getConfidence());
        incident.setLanguageProvider(result.getProvider());

        if ((incident.getCategory() == null || incident.getCategory() == IncidentCategory.OTHER) && result.getSuggestedCategory() != null) {
            incident.setCategory(result.getSuggestedCategory());
        }
    }

    public Set<String> getSupportedLanguages() {
        return SUPPORTED_LANGUAGES;
    }

    public List<Map<String, String>> getSupportedLanguagesList() {
        return List.of(
            Map.of("code", "EN", "name", "English", "nativeName", "English"),
            Map.of("code", "TA", "name", "Tamil", "nativeName", "தமிழ்"),
            Map.of("code", "HI", "name", "Hindi", "nativeName", "हिन्दी"),
            Map.of("code", "ML", "name", "Malayalam", "nativeName", "മലയാളം"),
            Map.of("code", "TE", "name", "Telugu", "nativeName", "తెలుగు")
        );
    }
}
