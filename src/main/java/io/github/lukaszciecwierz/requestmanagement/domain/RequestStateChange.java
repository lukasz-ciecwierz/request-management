package io.github.lukaszciecwierz.requestmanagement.domain;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Value
@Builder
public class RequestStateChange {

    UUID id;

    RequestId requestId;

    RequestState stateFrom;

    RequestState stateTo;

    Instant created;

    public Optional<RequestState> getStateFrom() {
        return Optional.ofNullable(stateFrom);
    }

    public static RequestStateChange forCreation(Request request) {
        return builder()
                .id(UUID.randomUUID())
                .requestId(request.getId())
                .stateTo(request.getState())
                .build();
    }

    public static RequestStateChange forTransition(Request before, Request after) {
        return builder()
                .id(UUID.randomUUID())
                .requestId(before.getId())
                .stateFrom(before.getState())
                .stateTo(after.getState())
                .build();
    }
}
