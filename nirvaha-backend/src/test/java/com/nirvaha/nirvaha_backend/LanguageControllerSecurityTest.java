package com.nirvaha.nirvaha_backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class LanguageControllerSecurityTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @DisplayName("Public access to supported languages endpoint")
    public void testGetSupportedLanguagesPublic() throws Exception {
        mockMvc.perform(get("/api/language/supported"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultLanguage").value("EN"))
                .andExpect(jsonPath("$.provider").value("LOCAL_RULES"))
                .andExpect(jsonPath("$.languageNames.TA").value("தமிழ் (Tamil)"));
    }

    @Test
    @DisplayName("Public access to language detection endpoint")
    public void testDetectLanguagePublic() throws Exception {
        String jsonPayload = "{\"text\": \"மழை பெய்தால் இங்கே தண்ணீர் தேங்குகிறது\"}";

        mockMvc.perform(post("/api/language/detect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detectedLanguage").value("TA"))
                .andExpect(jsonPath("$.suggestedCategory").value("DRAINAGE"))
                .andExpect(jsonPath("$.provider").value("LOCAL_RULES"));
    }
}
