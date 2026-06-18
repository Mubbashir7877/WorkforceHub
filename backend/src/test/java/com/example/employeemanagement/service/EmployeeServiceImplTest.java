package com.example.employeemanagement.service;

import com.example.employeemanagement.dto.EmployeeDto;
import com.example.employeemanagement.dto.EmployeeSelfUpdateRequest;
import com.example.employeemanagement.entity.Employee;
import com.example.employeemanagement.entity.User;
import com.example.employeemanagement.exception.ResourceNotFoundException;
import com.example.employeemanagement.kafka.EmployeeActivityEventProducer;
import com.example.employeemanagement.repository.EmployeeRepository;
import com.example.employeemanagement.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmployeeActivityEventProducer eventProducer;

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
    void createEmployee_ignoresClientSuppliedIdAndForcesActiveTrue() {
        EmployeeDto withClientId = new EmployeeDto(999L, "John", "Doe", "john.doe@example.com", false);
        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        when(employeeRepository.save(any(Employee.class))).thenReturn(employee);

        employeeService.createEmployee(withClientId);

        verify(employeeRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isNull();
        assertThat(captor.getValue().isActive()).isTrue();
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
    void deactivateEmployee_existingId_setsInactiveAndSaves() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);

        employeeService.deactivateEmployee(1L);

        verify(employeeRepository).save(captor.capture());
        assertThat(captor.getValue().isActive()).isFalse();
    }

    @Test
    void deactivateEmployee_nonExistingId_throwsResourceNotFoundException() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.deactivateEmployee(99L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void getOwnEmployee_userWithLinkedEmployee_returnsDto() {
        User user = new User("john.doe@example.com", "hash");
        user.setEmployee(employee);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        EmployeeDto result = employeeService.getOwnEmployee(7L);

        assertThat(result.getEmail()).isEqualTo("john.doe@example.com");
    }

    @Test
    void getOwnEmployee_userWithoutLinkedEmployee_throwsResourceNotFoundException() {
        User user = new User("noemployee@example.com", "hash");
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> employeeService.getOwnEmployee(7L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getOwnEmployee_unknownUserId_throwsResourceNotFoundException() {
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.getOwnEmployee(404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateOwnEmployee_updatesOnlyNameFields() {
        User user = new User("john.doe@example.com", "hash");
        user.setEmployee(employee);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EmployeeSelfUpdateRequest request = new EmployeeSelfUpdateRequest();
        request.setFirstName("Johnny");
        request.setLastName("Doerite");

        EmployeeDto result = employeeService.updateOwnEmployee(7L, request);

        assertThat(result.getFirstName()).isEqualTo("Johnny");
        assertThat(result.getLastName()).isEqualTo("Doerite");
        assertThat(result.getEmail()).isEqualTo("john.doe@example.com");
    }
}
