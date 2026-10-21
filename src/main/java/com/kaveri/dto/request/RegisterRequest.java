package com.kaveri.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d).+$",
             message = "Password must contain at least one letter and one number")
    private String password;

    @Pattern(regexp = "^$|^(?:\\+91|0)?[\\s-]?[6-9](?:[\\s-]?\\d){9}$",
             message = "Please provide a valid 10-digit Indian phone number (e.g. 9876543210 or +91 9876543210)")
    private String phone;

    public void setPhone(String phone) {
        this.phone = phone != null ? phone.trim() : null;
    }
}
