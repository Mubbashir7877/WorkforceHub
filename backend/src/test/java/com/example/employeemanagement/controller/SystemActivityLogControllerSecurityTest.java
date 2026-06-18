package com.example.employeemanagement.controller;

import com.example.employeemanagement.security.CustomUserDetailsService;
import com.example.employeemanagement.security.JwtService;
import com.example.employeemanagement.service.SystemActivityLogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies that the @PreAuthorize("hasRole('SYSTEM_ADMIN')") annotation on
 * SystemActivityLogController is correctly enforced.  Filters remain active so
 * that an unauthenticated request returns 401 from the default Boot security chain.
 */
@WebMvcTest(SystemActivityLogController.class)
@Import(SystemActivityLogControllerSecurityTest.MethodSecurityTestConfig.class)
class SystemActivityLogControllerSecurityTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {}

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SystemActivityLogService systemActivityLogService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void getLogs_systemAdmin_returnsOk() throws Exception {
        when(systemActivityLogService.getLogs(anyInt(), anyInt(), isNull())).thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/system/activity-logs"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "HR_ADMIN")
    void getLogs_hrAdmin_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/system/activity-logs"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getLogs_employee_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/system/activity-logs"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getLogs_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/system/activity-logs"))
                .andExpect(status().isUnauthorized());
    }
}
