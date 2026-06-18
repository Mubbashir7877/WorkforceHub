package com.example.employeemanagement.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Fields an EMPLOYEE may update on their own linked profile - notably, not email. */
@Getter
@Setter
@NoArgsConstructor
public class EmployeeSelfUpdateRequest {

    @NotBlank(message = "First name must not be blank")
    private String firstName;

    @NotBlank(message = "Last name must not be blank")
    private String lastName;
}
