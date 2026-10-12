package io.github.lukaszciecwierz.requestmanagement.domain;

public class RequestNotFoundException extends RuntimeException {

    private RequestNotFoundException(String message) {
        super(message);
    }

    public static RequestNotFoundException of(RequestId id) {
        return new RequestNotFoundException("Request not found for id: " + id.asString());
    }
}
