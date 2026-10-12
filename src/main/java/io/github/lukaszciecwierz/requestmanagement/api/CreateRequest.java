package io.github.lukaszciecwierz.requestmanagement.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record CreateRequest(

        @NotBlank(message = "Value is required")
        @Size(
                max = RequestValidationLimits.NAME_MAX_LENGTH,
                message = "Value must be at most {max} chars"
        )
        String name,

        @NotBlank(message = "Value is required")
        @Size(
                max = RequestValidationLimits.CONTENT_MAX_LENGTH,
                message = "Value must be at most {max} chars"
        )
        String content
) {
}
