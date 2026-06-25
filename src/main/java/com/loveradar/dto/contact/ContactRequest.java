package com.loveradar.dto.contact;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ContactRequest {

    @NotBlank(message = "Contact name is required")
    @Size(max = 150, message = "Contact name must be at most 150 characters")
    private String contactName;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[+0-9 ()-]{6,30}$", message = "Phone number format is invalid")
    private String phoneNumber;

    private boolean trusted = true;
}
