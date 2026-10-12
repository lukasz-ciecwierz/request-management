package io.github.lukaszciecwierz.requestmanagement.domain;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.util.UUID;

@Value
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class RequestId {

    UUID value;

    public static RequestId createNew() {
        return new RequestId(UUID.randomUUID());
    }

    public static RequestId parse(String raw) {
        try {
            return new RequestId(UUID.fromString(raw));
        } catch (IllegalArgumentException ex) {
            throw MalformedRequestIdException.of(raw);
        }
    }

    public static RequestId fromTrusted(@NonNull UUID value) {
        return new RequestId(value);
    }

    public String asString() {
        return value.toString();
    }
}
