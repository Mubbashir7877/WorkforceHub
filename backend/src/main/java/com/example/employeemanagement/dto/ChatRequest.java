package com.example.employeemanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChatRequest {

    private Long conversationId;

    @NotBlank(message = "Message must not be blank")
    @Size(max = 2000, message = "Message must be at most 2000 characters")
    private String message;
}
