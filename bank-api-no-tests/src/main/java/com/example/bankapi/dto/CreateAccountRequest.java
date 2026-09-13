package com.example.bankapi.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record CreateAccountRequest(

        @NotBlank(message = "ownerName must not be blank")
        String ownerName,

        @NotBlank
        @Pattern(regexp = "[A-Z]{3}", message = "currency must be a 3-letter ISO 4217 code, e.g. USD")
        String currency,

        @NotNull
        @DecimalMin(value = "0.00", message = "openingBalance must not be negative")
        BigDecimal openingBalance
) {
}
