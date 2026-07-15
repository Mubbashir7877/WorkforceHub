package com.example.employeemanagement.controller;

import com.example.employeemanagement.dto.*;
import com.example.employeemanagement.security.CustomUserDetailsService;
import com.example.employeemanagement.security.JwtService;
import com.example.employeemanagement.service.TimeClockService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies role-based access control for TimeClockController endpoints.
 * Uses WebMvcTest so only the web layer loads — no Kafka or DB required.
 */
@WebMvcTest(TimeClockController.class)
@Import(TimeClockControllerSecurityTest.MethodSecurityTestConfig.class)
class TimeClockControllerSecurityTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {}

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TimeClockService timeClockService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    // ── /status ──────────────────────────────────────────────────────────

    @Test
    void getStatus_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/time-clock/status"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getStatus_employee_returns200() throws Exception {
        when(timeClockService.getStatus()).thenReturn(new TimeClockStatusResponse(false, null, null));

        mockMvc.perform(get("/api/v1/time-clock/status"))
                .andExpect(status().isOk());
    }

    // ── /clock-in ─────────────────────────────────────────────────────────

    @Test
    void clockIn_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/time-clock/clock-in").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void clockIn_employee_returns201() throws Exception {
        when(timeClockService.clockIn())
                .thenReturn(new ClockInResponse(1L, 1L, Instant.now(), "Clocked in."));

        mockMvc.perform(post("/api/v1/time-clock/clock-in").with(csrf()))
                .andExpect(status().isCreated());
    }

    // ── /clock-out ────────────────────────────────────────────────────────

    @Test
    void clockOut_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/time-clock/clock-out").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void clockOut_employee_returns200() throws Exception {
        when(timeClockService.clockOut())
                .thenReturn(new ClockOutResponse(1L, 1L, Instant.now().minusSeconds(3600),
                        Instant.now(), 60L, "Clocked out."));

        mockMvc.perform(post("/api/v1/time-clock/clock-out").with(csrf()))
                .andExpect(status().isOk());
    }

    // ── /events ───────────────────────────────────────────────────────────

    @Test
    void getEvents_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/time-clock/events"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getEvents_employee_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/time-clock/events"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getEvents_manager_returns200() throws Exception {
        when(timeClockService.getEventLogs(anyInt(), anyInt(), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/time-clock/events"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "HR_ADMIN")
    void getEvents_hrAdmin_returns200() throws Exception {
        when(timeClockService.getEventLogs(anyInt(), anyInt(), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/time-clock/events"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void getEvents_systemAdmin_returns200() throws Exception {
        when(timeClockService.getEventLogs(anyInt(), anyInt(), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/time-clock/events"))
                .andExpect(status().isOk());
    }
}
