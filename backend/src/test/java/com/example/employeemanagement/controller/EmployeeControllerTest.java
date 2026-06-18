package com.example.employeemanagement.controller;

import com.example.employeemanagement.dto.EmployeeDto;
import com.example.employeemanagement.dto.EmployeeSelfUpdateRequest;
import com.example.employeemanagement.entity.User;
import com.example.employeemanagement.exception.ResourceNotFoundException;
import com.example.employeemanagement.security.CustomUserDetailsService;
import com.example.employeemanagement.security.JwtService;
import com.example.employeemanagement.security.UserPrincipal;
import com.example.employeemanagement.service.EmployeeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Verifies HTTP-layer behavior (status codes, request/response shape) with the
 * service mocked out. This slice does not load {@code SecurityConfig}, so
 * {@code @PreAuthorize} role/ownership enforcement is not exercised here - see
 * {@link EmployeeAuthorizationIntegrationTest} for that.
 */
@WebMvcTest(EmployeeController.class)
@AutoConfigureMockMvc(addFilters = false)
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmployeeService employeeService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(Long userId) {
        User user = new User("principal@example.com", "hash");
        user.setId(userId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new UserPrincipal(user), null, List.of()));
    }

    @Test
    void getAllEmployees_returnsOkWithList() throws Exception {
        EmployeeDto dto = new EmployeeDto(1L, "John", "Doe", "john.doe@example.com");
        when(employeeService.getAllEmployees()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].firstName").value("John"))
                .andExpect(jsonPath("$[0].email").value("john.doe@example.com"));
    }

    @Test
    void getEmployeeById_existingId_returnsOk() throws Exception {
        EmployeeDto dto = new EmployeeDto(1L, "John", "Doe", "john.doe@example.com");
        when(employeeService.getEmployeeById(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/employees/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("John"));
    }

    @Test
    void getEmployeeById_nonExistingId_returnsNotFound() throws Exception {
        when(employeeService.getEmployeeById(99L))
                .thenThrow(new ResourceNotFoundException("Employee not found with id: 99"));

        mockMvc.perform(get("/api/v1/employees/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void createEmployee_validPayload_returnsCreated() throws Exception {
        EmployeeDto input = new EmployeeDto(null, "John", "Doe", "john.doe@example.com");
        EmployeeDto saved = new EmployeeDto(1L, "John", "Doe", "john.doe@example.com");
        when(employeeService.createEmployee(any(EmployeeDto.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void createEmployee_blankFirstName_returnsBadRequest() throws Exception {
        EmployeeDto invalid = new EmployeeDto(null, "", "Doe", "john@example.com");

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.firstName").exists());
    }

    @Test
    void createEmployee_invalidEmail_returnsBadRequest() throws Exception {
        EmployeeDto invalid = new EmployeeDto(null, "John", "Doe", "not-an-email");

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    void updateEmployee_existingId_returnsOk() throws Exception {
        EmployeeDto input = new EmployeeDto(null, "Jane", "Smith", "jane@example.com");
        EmployeeDto updated = new EmployeeDto(1L, "Jane", "Smith", "jane@example.com");
        when(employeeService.updateEmployee(eq(1L), any(EmployeeDto.class))).thenReturn(updated);

        mockMvc.perform(put("/api/v1/employees/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Jane"));
    }

    @Test
    void deactivateEmployee_existingId_returnsNoContent() throws Exception {
        doNothing().when(employeeService).deactivateEmployee(1L);

        mockMvc.perform(delete("/api/v1/employees/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deactivateEmployee_nonExistingId_returnsNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Employee not found with id: 99"))
                .when(employeeService).deactivateEmployee(99L);

        mockMvc.perform(delete("/api/v1/employees/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getOwnEmployee_authenticatedUser_returnsOwnProfile() throws Exception {
        authenticateAs(7L);
        EmployeeDto dto = new EmployeeDto(1L, "John", "Doe", "john.doe@example.com");
        when(employeeService.getOwnEmployee(7L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/employees/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("john.doe@example.com"));
    }

    @Test
    void updateOwnEmployee_authenticatedUser_returnsUpdatedProfile() throws Exception {
        authenticateAs(7L);
        EmployeeSelfUpdateRequest request = new EmployeeSelfUpdateRequest();
        request.setFirstName("Johnny");
        request.setLastName("Doerite");
        EmployeeDto updated = new EmployeeDto(1L, "Johnny", "Doerite", "john.doe@example.com");
        when(employeeService.updateOwnEmployee(eq(7L), any(EmployeeSelfUpdateRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/v1/employees/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Johnny"));
    }
}
