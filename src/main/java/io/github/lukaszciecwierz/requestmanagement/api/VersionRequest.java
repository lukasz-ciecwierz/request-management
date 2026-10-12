package io.github.lukaszciecwierz.requestmanagement.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

record VersionRequest(

        @NotNull(message = "Value is required")
        @PositiveOrZero(message = "Only zero or positive integers are valid")
        Integer version
) {
}
