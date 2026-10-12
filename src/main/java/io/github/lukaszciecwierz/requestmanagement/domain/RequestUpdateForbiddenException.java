package io.github.lukaszciecwierz.requestmanagement.domain;

public class RequestUpdateForbiddenException extends RuntimeException {

    private RequestUpdateForbiddenException(String message) {
        super(message);
    }

    public static RequestUpdateForbiddenException of(RequestState currentState) {
        return new RequestUpdateForbiddenException("Request update for state %s is forbidden".formatted(currentState));
    }
}
