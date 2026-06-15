package com.example.employeemanagement.service;

import com.example.employeemanagement.dto.EmployeeDto;
import com.example.employeemanagement.entity.Employee;
import com.example.employeemanagement.exception.ResourceNotFoundException;
import com.example.employeemanagement.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private EmployeeServiceImpl employeeService;

    private Employee employee;
    private EmployeeDto employeeDto;

    @BeforeEach
    void setUp() {
        employee = new Employee(1L, "John", "Doe", "john.doe@example.com");
        employeeDto = new EmployeeDto(1L, "John", "Doe", "john.doe@example.com");
    }

    @Test
    void getAllEmployees_returnsListOfDtos() {
        when(employeeRepository.findAll()).thenReturn(List.of(employee));

        List<EmployeeDto> result = employeeService.getAllEmployees();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFirstName()).isEqualTo("John");
        assertThat(result.get(0).getEmail()).isEqualTo("john.doe@example.com");
    }

    @Test
    void getAllEmployees_emptyList_returnsEmptyList() {
        when(employeeRepository.findAll()).thenReturn(List.of());

        List<EmployeeDto> result = employeeService.getAllEmployees();

        assertThat(result).isEmpty();
    }

    @Test
    void getEmployeeById_existingId_returnsDto() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        EmployeeDto result = employeeService.getEmployeeById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getLastName()).isEqualTo("Doe");
    }

    @Test
    void getEmployeeById_nonExistingId_throwsResourceNotFoundException() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.getEmployeeById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void createEmployee_savesAndReturnsDto() {
        when(employeeRepository.save(any(Employee.class))).thenReturn(employee);

        EmployeeDto result = employeeService.createEmployee(employeeDto);

        assertThat(result.getFirstName()).isEqualTo("John");
        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    void updateEmployee_existingId_updatesAndReturnsDto() {
        EmployeeDto updateRequest = new EmployeeDto(null, "Jane", "Smith", "jane.smith@example.com");
        Employee updatedEmployee = new Employee(1L, "Jane", "Smith", "jane.smith@example.com");

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(any(Employee.class))).thenReturn(updatedEmployee);

        EmployeeDto result = employeeService.updateEmployee(1L, updateRequest);

        assertThat(result.getFirstName()).isEqualTo("Jane");
        assertThat(result.getEmail()).isEqualTo("jane.smith@example.com");
    }

    @Test
    void updateEmployee_nonExistingId_throwsResourceNotFoundException() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.updateEmployee(99L, employeeDto))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteEmployee_existingId_deletesSuccessfully() {
        when(employeeRepository.existsById(1L)).thenReturn(true);

        employeeService.deleteEmployee(1L);

        verify(employeeRepository).deleteById(1L);
    }

    @Test
    void deleteEmployee_nonExistingId_throwsResourceNotFoundException() {
        when(employeeRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> employeeService.deleteEmployee(99L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(employeeRepository, never()).deleteById(any());
    }
}
