package io.github.lukaszciecwierz.requestmanagement.api;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
class RequestValidationLimits {

    static final int NAME_MAX_LENGTH = 100;
    static final int CONTENT_MAX_LENGTH = 1000;
    static final int REASON_MAX_LENGTH = 500;
}
