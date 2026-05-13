package com.skyways.booking.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PassengerDto {

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @NotBlank
    @Pattern(regexp = "[A-Z0-9]{6,9}", message = "Invalid passport number format")
    private String passportNo;

    @NotBlank
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "Passport expiry must be YYYY-MM-DD")
    private String passportExpiry;

    @NotBlank
    private String nationality;

    @NotBlank
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "Date of birth must be YYYY-MM-DD")
    private String dateOfBirth;

    @NotBlank @Email
    private String email;

    private String phone;
}
