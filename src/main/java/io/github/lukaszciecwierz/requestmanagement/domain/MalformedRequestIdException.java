package io.github.lukaszciecwierz.requestmanagement.domain;

public class MalformedRequestIdException extends RuntimeException {

    private MalformedRequestIdException(String message) {
        super(message);
    }

    public static MalformedRequestIdException of(String malformedId) {
        return new MalformedRequestIdException("Malformed request id: " + malformedId);
    }
}
