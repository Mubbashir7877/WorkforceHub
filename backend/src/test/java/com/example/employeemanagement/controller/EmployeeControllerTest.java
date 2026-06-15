package com.example.employeemanagement.controller;

import com.example.employeemanagement.dto.EmployeeDto;
import com.example.employeemanagement.exception.ResourceNotFoundException;
import com.example.employeemanagement.service.EmployeeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EmployeeController.class)
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmployeeService employeeService;

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
    void deleteEmployee_existingId_returnsNoContent() throws Exception {
        doNothing().when(employeeService).deleteEmployee(1L);

        mockMvc.perform(delete("/api/v1/employees/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteEmployee_nonExistingId_returnsNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Employee not found with id: 99"))
                .when(employeeService).deleteEmployee(99L);

        mockMvc.perform(delete("/api/v1/employees/99"))
                .andExpect(status().isNotFound());
    }
}
