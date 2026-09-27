package com.Mediatron.Lab_Management.data_dto;

import java.util.ArrayList;
import java.util.List;

public class ChatbotMessageResponse {
    private String answer;
    private boolean personalized;
    private int sourceCount;
    private List<String> suggestions = new ArrayList<>();

    public ChatbotMessageResponse() {
    }

    public ChatbotMessageResponse(String answer, boolean personalized, int sourceCount, List<String> suggestions) {
        this.answer = answer;
        this.personalized = personalized;
        this.sourceCount = sourceCount;
        this.suggestions = suggestions == null ? new ArrayList<>() : suggestions;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public boolean isPersonalized() {
        return personalized;
    }

    public void setPersonalized(boolean personalized) {
        this.personalized = personalized;
    }

    public int getSourceCount() {
        return sourceCount;
    }

    public void setSourceCount(int sourceCount) {
        this.sourceCount = sourceCount;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions == null ? new ArrayList<>() : suggestions;
    }
}
