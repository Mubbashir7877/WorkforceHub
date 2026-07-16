package com.example.employeemanagement.controller.ai;

import com.example.employeemanagement.dto.ChatRequest;
import com.example.employeemanagement.dto.ChatResponse;
import com.example.employeemanagement.dto.ConversationResponse;
import com.example.employeemanagement.dto.ConversationSummaryResponse;
import com.example.employeemanagement.service.HrAssistantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Employee-facing HR assistant chat. All authenticated users may use it;
 * conversation access is always ownership-scoped (see HrAssistantService).
 */
@RestController
@RequestMapping("/api/v1/ai/hr-assistant")
@RequiredArgsConstructor
public class HrAssistantController {

    private final HrAssistantService assistantService;

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        return ResponseEntity.ok(assistantService.chat(request));
    }

    @GetMapping("/conversations")
    public ResponseEntity<Page<ConversationSummaryResponse>> getConversations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(assistantService.getConversations(page, size));
    }

    @GetMapping("/conversations/{id}")
    public ResponseEntity<ConversationResponse> getConversation(@PathVariable Long id) {
        return ResponseEntity.ok(assistantService.getConversation(id));
    }

    @DeleteMapping("/conversations/{id}")
    public ResponseEntity<Void> deleteConversation(@PathVariable Long id) {
        assistantService.deleteConversation(id);
        return ResponseEntity.noContent().build();
    }
}
