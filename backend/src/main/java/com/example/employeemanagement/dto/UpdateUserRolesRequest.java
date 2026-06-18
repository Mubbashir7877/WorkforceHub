package com.example.employeemanagement.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
public class UpdateUserRolesRequest {

    @NotEmpty(message = "At least one role must be assigned")
    private Set<String> roles;
}
