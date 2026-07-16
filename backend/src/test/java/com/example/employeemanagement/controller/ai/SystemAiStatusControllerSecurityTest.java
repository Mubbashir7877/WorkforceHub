package com.example.employeemanagement.controller.ai;

import com.example.employeemanagement.dto.AiStatusResponse;
import com.example.employeemanagement.security.CustomUserDetailsService;
import com.example.employeemanagement.security.JwtService;
import com.example.employeemanagement.service.AiOperationalStatusService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SystemAiStatusController.class)
@Import(SystemAiStatusControllerSecurityTest.MethodSecurityTestConfig.class)
class SystemAiStatusControllerSecurityTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {}

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AiOperationalStatusService statusService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    void getStatus_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/system/ai/status"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "HR_ADMIN")
    void getStatus_hrAdmin_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/system/ai/status"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void getStatus_systemAdmin_returns200() throws Exception {
        when(statusService.getStatus()).thenReturn(new AiStatusResponse(false, false, "gpt-4o-mini", false, 0));

        mockMvc.perform(get("/api/v1/system/ai/status"))
                .andExpect(status().isOk());
    }
}
