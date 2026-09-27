package com.Mediatron.Lab_Management.controllers;

import com.Mediatron.Lab_Management.data_dto.ChatbotMessageRequest;
import com.Mediatron.Lab_Management.data_dto.ChatbotMessageResponse;
import com.Mediatron.Lab_Management.services.ChatbotService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping("/message")
    public ResponseEntity<ChatbotMessageResponse> message(
            @Valid @RequestBody ChatbotMessageRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(chatbotService.reply(request, authentication));
    }
}
