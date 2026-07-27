package com.example.employeemanagement.controller.ai;

import com.example.employeemanagement.dto.PolicyConflictReviewResponse;
import com.example.employeemanagement.dto.PolicyDocumentResponse;
import com.example.employeemanagement.security.CustomUserDetailsService;
import com.example.employeemanagement.security.JwtService;
import com.example.employeemanagement.service.HrPolicyDocumentService;
import com.example.employeemanagement.service.PolicyConflictReviewService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies role-based access control for HrPolicyController — restricted to
 * HR_ADMIN and SYSTEM_ADMIN. Uses WebMvcTest so only the web layer loads.
 */
@WebMvcTest(HrPolicyController.class)
@Import(HrPolicyControllerSecurityTest.MethodSecurityTestConfig.class)
class HrPolicyControllerSecurityTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {}

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private HrPolicyDocumentService documentService;

    @MockBean
    private PolicyConflictReviewService conflictReviewService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    private PolicyDocumentResponse sampleResponse() {
        return new PolicyDocumentResponse(
                1L, "Leave Policy", "desc", "LEAVE", "1.0", LocalDate.now(),
                false, "DRAFT", 0, null, null, "hr@example.com", Instant.now(), Instant.now());
    }

    // ── list ─────────────────────────────────────────────────────────────

    @Test
    void list_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/hr/policies"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void list_employee_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/hr/policies"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void list_manager_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/hr/policies"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR_ADMIN")
    void list_hrAdmin_returns200() throws Exception {
        when(documentService.list(any(), any(), anyInt(), anyInt())).thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/hr/policies"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void list_systemAdmin_returns200() throws Exception {
        when(documentService.list(any(), any(), anyInt(), anyInt())).thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/hr/policies"))
                .andExpect(status().isOk());
    }

    // ── create ───────────────────────────────────────────────────────────

    @Test
    void create_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/hr/policies").with(csrf())
                        .contentType("application/json")
                        .content("{\"title\":\"x\",\"category\":\"GENERAL\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void create_employee_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/hr/policies").with(csrf())
                        .contentType("application/json")
                        .content("{\"title\":\"x\",\"category\":\"GENERAL\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR_ADMIN")
    void create_hrAdmin_blankTitle_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/hr/policies").with(csrf())
                        .contentType("application/json")
                        .content("{\"title\":\"\",\"category\":\"GENERAL\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "HR_ADMIN")
    void create_hrAdmin_validRequest_returns201() throws Exception {
        when(documentService.create(any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/hr/policies").with(csrf())
                        .contentType("application/json")
                        .content("{\"title\":\"Leave Policy\",\"category\":\"LEAVE\"}"))
                .andExpect(status().isCreated());
    }

    // ── getById ──────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "HR_ADMIN")
    void getById_hrAdmin_returns200() throws Exception {
        when(documentService.getById(1L)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/hr/policies/1"))
                .andExpect(status().isOk());
    }

    // ── process / activate / deactivate ─────────────────────────────────

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void process_employee_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/hr/policies/1/process").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR_ADMIN")
    void process_hrAdmin_returns200() throws Exception {
        when(documentService.process(1L)).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/hr/policies/1/process").with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "HR_ADMIN")
    void activate_hrAdmin_returns200() throws Exception {
        when(documentService.activate(1L)).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/hr/policies/1/activate").with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void deactivate_systemAdmin_returns200() throws Exception {
        when(documentService.deactivate(1L)).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/hr/policies/1/deactivate").with(csrf()))
                .andExpect(status().isOk());
    }

    // ── review-conflicts ─────────────────────────────────────────────────

    @Test
    void reviewConflicts_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/hr/policies/1/review-conflicts").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void reviewConflicts_employee_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/hr/policies/1/review-conflicts").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR_ADMIN")
    void reviewConflicts_hrAdmin_returns200() throws Exception {
        when(conflictReviewService.reviewForConflicts(1L))
                .thenReturn(new PolicyConflictReviewResponse(1L, false, java.util.List.of(), Instant.now()));

        mockMvc.perform(post("/api/v1/hr/policies/1/review-conflicts").with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void reviewConflicts_systemAdmin_returns200() throws Exception {
        when(conflictReviewService.reviewForConflicts(1L))
                .thenReturn(new PolicyConflictReviewResponse(1L, false, java.util.List.of(), Instant.now()));

        mockMvc.perform(post("/api/v1/hr/policies/1/review-conflicts").with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void download_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/hr/policies/1/download"))
                .andExpect(status().isUnauthorized());
    }
}
