package com.nirvaha.nirvaha_backend;

import com.nirvaha.nirvaha_backend.entity.Incident;
import com.nirvaha.nirvaha_backend.entity.IncidentCategory;
import com.nirvaha.nirvaha_backend.service.LanguageService;
import com.nirvaha.nirvaha_backend.service.LanguageService.LanguageDetectionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class LanguageServiceTest {

    private LanguageService languageService;

    @BeforeEach
    public void setUp() {
        languageService = new LanguageService();
    }

    @Test
    @DisplayName("Detect Tamil script correctly")
    public void testDetectTamilScript() {
        String tamilText = "மழை பெய்தால் இங்கே தண்ணீர் தேங்குகிறது";
        LanguageDetectionResult result = languageService.processText(tamilText, null);
        assertEquals("TA", result.getDetectedLanguage());
        assertTrue(result.getConfidence() >= 0.80);
    }

    @Test
    @DisplayName("Detect Hindi script correctly")
    public void testDetectHindiScript() {
        String hindiText = "बारिश के बाद सड़क पर पानी भर जाता है";
        LanguageDetectionResult result = languageService.processText(hindiText, null);
        assertEquals("HI", result.getDetectedLanguage());
        assertTrue(result.getConfidence() >= 0.80);
    }

    @Test
    @DisplayName("Detect Malayalam script correctly")
    public void testDetectMalayalamScript() {
        String malayalamText = "റോഡിൽ വെള്ളക്കെട്ട് ഉണ്ട്";
        LanguageDetectionResult result = languageService.processText(malayalamText, null);
        assertEquals("ML", result.getDetectedLanguage());
        assertTrue(result.getConfidence() >= 0.80);
    }

    @Test
    @DisplayName("Detect Telugu script correctly")
    public void testDetectTeluguScript() {
        String teluguText = "రోడ్డుపై నీరు నిలిచిపోయింది";
        LanguageDetectionResult result = languageService.processText(teluguText, null);
        assertEquals("TE", result.getDetectedLanguage());
        assertTrue(result.getConfidence() >= 0.80);
    }

    @Test
    @DisplayName("Detect English text correctly")
    public void testDetectEnglishText() {
        String englishText = "Pothole on main road causing severe traffic congestion";
        LanguageDetectionResult result = languageService.processText(englishText, null);
        assertEquals("EN", result.getDetectedLanguage());
    }

    @Test
    @DisplayName("Multilingual normalization maps Tamil drainage to canonical DRAINAGE")
    public void testTamilDrainageNormalization() {
        Incident incident = new Incident();
        incident.setDescription("மழை பெய்தால் இங்கே தண்ணீர் தேங்குகிறது");

        languageService.processIncidentMultilingualSupport(incident, "TA");

        assertEquals("மழை பெய்தால் இங்கே தண்ணீர் தேங்குகிறது", incident.getOriginalDescription());
        assertEquals("TA", incident.getOriginalLanguage());
        assertEquals("COMPLETED", incident.getNormalizationStatus());
        assertEquals(IncidentCategory.DRAINAGE, incident.getCategory());
        assertNotNull(incident.getNormalizedDescription());
    }

    @Test
    @DisplayName("Multilingual normalization maps Hindi pothole to canonical POTHOLE")
    public void testHindiPotholeNormalization() {
        Incident incident = new Incident();
        incident.setDescription("सड़क पर बड़ा गड्ढा बन गया है");

        languageService.processIncidentMultilingualSupport(incident, null);

        assertEquals("सड़क पर बड़ा गड्ढा बन गया है", incident.getOriginalDescription());
        assertEquals("HI", incident.getOriginalLanguage());
        assertEquals("COMPLETED", incident.getNormalizationStatus());
        assertEquals(IncidentCategory.POTHOLE, incident.getCategory());
    }

    @Test
    @DisplayName("Explicit language selection overrides auto-detection when valid")
    public void testExplicitLanguageOverride() {
        Incident incident = new Incident();
        incident.setDescription("Water leakage near municipal tank");

        languageService.processIncidentMultilingualSupport(incident, "TE");

        assertEquals("TE", incident.getOriginalLanguage());
    }

    @Test
    @DisplayName("Cross-language reports map to identical canonical category for intelligence pipeline")
    public void testCrossLanguageCanonicalCategoryEquality() {
        Incident taIncident = new Incident();
        taIncident.setDescription("சாக்கடை நீர் தெருவில் வழிகிறது");
        languageService.processIncidentMultilingualSupport(taIncident, "TA");

        Incident hiIncident = new Incident();
        hiIncident.setDescription("नाली का पानी सड़क पर बह रहा है");
        languageService.processIncidentMultilingualSupport(hiIncident, "HI");

        Incident enIncident = new Incident();
        enIncident.setDescription("Overflowing sewage drain on street");
        languageService.processIncidentMultilingualSupport(enIncident, "EN");

        // Canonical category must be identical across languages
        assertEquals(IncidentCategory.DRAINAGE, taIncident.getCategory());
        assertEquals(IncidentCategory.DRAINAGE, hiIncident.getCategory());
        assertEquals(IncidentCategory.DRAINAGE, enIncident.getCategory());
    }

    @Test
    @DisplayName("Supported languages list contains EN, TA, HI, ML, TE")
    public void testSupportedLanguages() {
        List<Map<String, String>> supported = languageService.getSupportedLanguagesList();
        assertEquals(5, supported.size());

        List<String> codes = supported.stream().map(m -> m.get("code")).toList();
        assertTrue(codes.containsAll(List.of("EN", "TA", "HI", "ML", "TE")));
    }
}
