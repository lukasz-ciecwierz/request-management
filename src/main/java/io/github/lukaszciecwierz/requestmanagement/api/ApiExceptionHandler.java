package io.github.lukaszciecwierz.requestmanagement.api;

import lombok.extern.slf4j.Slf4j;
import io.github.lukaszciecwierz.requestmanagement.domain.InvalidRequestStateTransitionException;
import io.github.lukaszciecwierz.requestmanagement.domain.MalformedRequestIdException;
import io.github.lukaszciecwierz.requestmanagement.domain.RequestNotFoundException;
import io.github.lukaszciecwierz.requestmanagement.domain.RequestUpdateForbiddenException;
import io.github.lukaszciecwierz.requestmanagement.domain.StaleVersionSentException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Map;
import java.util.Objects;

@Slf4j
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(RequestNotFoundException.class)
    ProblemDetail handleRequestNotFoundException(RequestNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
    }

    @ExceptionHandler({
            InvalidRequestStateTransitionException.class,
            RequestUpdateForbiddenException.class
    })
    ProblemDetail handleCustom422Exceptions(Exception ex) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_CONTENT,
                ex.getMessage()
        );
    }

    @ExceptionHandler({
            StaleVersionSentException.class,
            OptimisticLockingFailureException.class
    })
    ProblemDetail handleOptimisticLockingFailureExceptions(Exception ex) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "Sent version number is stale, refresh and try again"
        );
    }

    @ExceptionHandler(MalformedRequestIdException.class)
    ProblemDetail handleMalformedRequestIdException(MalformedRequestIdException ex) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        var errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> Map.of(
                        "field", fieldError.getField(),
                        "message", Objects.requireNonNullElse(fieldError.getDefaultMessage(), "")
                ))
                .toList();

        var problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Invalid request"
        );
        problemDetail.setProperty("errors", errors);

        return handleExceptionInternal(ex, problemDetail, headers, status, request);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleOtherExceptions(Exception ex) {
        log.error("Unhandled exception", ex);

        var error = HttpStatus.INTERNAL_SERVER_ERROR;
        return ProblemDetail.forStatusAndDetail(
                error,
                error.getReasonPhrase()
        );
    }
}
