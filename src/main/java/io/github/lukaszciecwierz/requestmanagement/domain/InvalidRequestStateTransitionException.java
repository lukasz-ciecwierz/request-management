package io.github.lukaszciecwierz.requestmanagement.domain;

public class InvalidRequestStateTransitionException extends RuntimeException {

    private InvalidRequestStateTransitionException(String message) {
        super(message);
    }

    public static InvalidRequestStateTransitionException of(RequestState oldState, RequestState newState) {
        return new InvalidRequestStateTransitionException("Request state transition from %s to %s is not allowed.".formatted(oldState, newState));
    }
}
