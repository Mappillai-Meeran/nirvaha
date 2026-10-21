package com.nirvaha.nirvaha_backend.dto;

import com.nirvaha.nirvaha_backend.entity.FeedbackType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ResidentFeedbackRequest {

    @NotNull(message = "Feedback type is required (CONFIRMED or DISPUTED)")
    private FeedbackType feedbackType;

    @Size(max = 2000, message = "Explanation must not exceed 2000 characters")
    private String explanation;

    public ResidentFeedbackRequest() {
    }

    public ResidentFeedbackRequest(FeedbackType feedbackType, String explanation) {
        this.feedbackType = feedbackType;
        this.explanation = explanation;
    }

    public FeedbackType getFeedbackType() {
        return feedbackType;
    }

    public void setFeedbackType(FeedbackType feedbackType) {
        this.feedbackType = feedbackType;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }
}
