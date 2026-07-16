package com.example.employeemanagement.controller.ai;

import com.example.employeemanagement.dto.ChatResponse;
import com.example.employeemanagement.dto.ConversationResponse;
import com.example.employeemanagement.security.CustomUserDetailsService;
import com.example.employeemanagement.security.JwtService;
import com.example.employeemanagement.service.HrAssistantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies HrAssistantController is open to all authenticated roles (no
 * @PreAuthorize restriction) and rejects unauthenticated/invalid requests.
 */
@WebMvcTest(HrAssistantController.class)
@Import(HrAssistantControllerSecurityTest.MethodSecurityTestConfig.class)
class HrAssistantControllerSecurityTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {}

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HrAssistantService assistantService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    void chat_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/ai/hr-assistant/chat").with(csrf())
                        .contentType("application/json")
                        .content("{\"message\":\"How many leave days do I have?\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void chat_employee_blankMessage_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/ai/hr-assistant/chat").with(csrf())
                        .contentType("application/json")
                        .content("{\"message\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void chat_employee_validRequest_returns200() throws Exception {
        when(assistantService.chat(any())).thenReturn(
                new ChatResponse("You get 15 days.", true, List.of(), 1L, Instant.now()));

        mockMvc.perform(post("/api/v1/ai/hr-assistant/chat").with(csrf())
                        .contentType("application/json")
                        .content("{\"message\":\"How many leave days do I have?\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void chat_manager_validRequest_returns200() throws Exception {
        when(assistantService.chat(any())).thenReturn(
                new ChatResponse("Answer.", false, List.of(), 2L, Instant.now()));

        mockMvc.perform(post("/api/v1/ai/hr-assistant/chat").with(csrf())
                        .contentType("application/json")
                        .content("{\"message\":\"question\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void getConversations_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/ai/hr-assistant/conversations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "HR_ADMIN")
    void getConversations_hrAdmin_returns200() throws Exception {
        when(assistantService.getConversations(anyInt(), anyInt())).thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/ai/hr-assistant/conversations"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getConversation_ownedByUser_returns200() throws Exception {
        when(assistantService.getConversation(1L)).thenReturn(
                new ConversationResponse(1L, Instant.now(), Instant.now(), List.of()));

        mockMvc.perform(get("/api/v1/ai/hr-assistant/conversations/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void deleteConversation_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/ai/hr-assistant/conversations/1").with(csrf()))
                .andExpect(status().isNoContent());
    }
}
