package io.github.lukaszciecwierz.requestmanagement.api;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

record DeleteRequest(

        @NotNull(message = "Value is required")
        @PositiveOrZero(message = "Only zero or positive integers are valid")
        Integer version,

        @NotBlank(message = "Value is required")
        @Size(
                max = RequestValidationLimits.REASON_MAX_LENGTH,
                message = "Value must be at most {max} chars"
        )
        String reason
) {
}
