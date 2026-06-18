package com.example.employeemanagement.mapper;

import com.example.employeemanagement.dto.EmployeeSummaryDto;
import com.example.employeemanagement.dto.UserResponse;
import com.example.employeemanagement.entity.Employee;
import com.example.employeemanagement.entity.User;

import java.util.stream.Collectors;

public final class UserMapper {

    private UserMapper() {}

    public static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.isEnabled(),
                user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .collect(Collectors.toSet()),
                toEmployeeSummary(user.getEmployee())
        );
    }

    private static EmployeeSummaryDto toEmployeeSummary(Employee employee) {
        if (employee == null) {
            return null;
        }
        return new EmployeeSummaryDto(
                employee.getId(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getEmail(),
                employee.isActive()
        );
    }
}
