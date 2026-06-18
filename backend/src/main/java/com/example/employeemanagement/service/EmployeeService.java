package com.example.employeemanagement.service;

import com.example.employeemanagement.dto.EmployeeDto;
import com.example.employeemanagement.dto.EmployeeSelfUpdateRequest;

import java.util.List;

public interface EmployeeService {

    List<EmployeeDto> getAllEmployees();

    EmployeeDto getEmployeeById(Long id);

    EmployeeDto createEmployee(EmployeeDto employeeDto);

    EmployeeDto updateEmployee(Long id, EmployeeDto employeeDto);

    void deactivateEmployee(Long id);

    EmployeeDto getOwnEmployee(Long userId);

    EmployeeDto updateOwnEmployee(Long userId, EmployeeSelfUpdateRequest request);
}
