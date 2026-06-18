package com.example.employeemanagement.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LinkEmployeeRequest {

    @NotNull(message = "Employee id must not be null")
    private Long employeeId;
}
