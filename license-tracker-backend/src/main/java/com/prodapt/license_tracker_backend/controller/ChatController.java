package com.prodapt.license_tracker_backend.controller;

import com.prodapt.license_tracker_backend.ai.LicenseTrackerAssistant;
import com.prodapt.license_tracker_backend.dto.ChatRequestDto;
import com.prodapt.license_tracker_backend.dto.ChatResponseDto;
import com.prodapt.license_tracker_backend.entities.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai/chat")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE_OFFICER', 'PROCUREMENT_OFFICER', 'IT_AUDITOR', 'COMPLIANCE_LEAD', 'PROCUREMENT_LEAD', 'PRODUCT_OWNER')")
public class ChatController {

    private final LicenseTrackerAssistant licenseTrackerAssistant;

    @PostMapping("/message")
    public ResponseEntity<ChatResponseDto> sendMessage(@RequestBody ChatRequestDto request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(
                        new ChatResponseDto("Please log in to use the AI assistant.", null)
                );
            }

            String username = authentication.getName();
            String chatId = "user-" + username;

            log.info("Processing chat message for user: {}, chatId: {}", username, chatId);

            // Call AI Assistant
            String response = licenseTrackerAssistant.chat(chatId, request.getMessage());

            log.info("AI response generated for chatId: {}", chatId);

            return ResponseEntity.ok(new ChatResponseDto(response, chatId));

        } catch (Exception e) {
            log.error("Error processing chat message", e);
            return ResponseEntity.status(500).body(
                    new ChatResponseDto("Sorry, I encountered an error. Please try again.", null)
            );
        }
    }

    @DeleteMapping("/clear/{chatId}")
    public ResponseEntity<Void> clearChatHistory(@PathVariable String chatId) {
        try {
            log.info("Chat history clear requested for chatId: {}", chatId);
            // Note: InMemoryChatMemoryStore automatically manages memory
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error clearing chat history", e);
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("AI Assistant is running!");
    }
}
