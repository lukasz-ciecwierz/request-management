package io.github.lukaszciecwierz.requestmanagement.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.time.Instant;
import java.util.function.LongSupplier;

@Value
@Builder
public class Request {

    RequestId id;

    String name;

    @With(value = AccessLevel.PRIVATE)
    String content;

    @With(value = AccessLevel.PRIVATE)
    RequestState state;

    @With(value = AccessLevel.PRIVATE)
    Long publicationNumber;

    @With(value = AccessLevel.PRIVATE)
    String reason;

    Instant created;

    Instant updated;

    int version;

    public static Request createNew(String name, String content) {
        return Request.builder()
                .id(RequestId.createNew())
                .name(name)
                .content(content)
                .state(RequestState.CREATED)
                .build();
    }

    public Request update(String newContent) {
        if (!state.isUpdateAllowedForThisState()) {
            throw RequestUpdateForbiddenException.of(state);
        }
        return withContent(newContent);
    }

    public Request deleteWithReason(String reason) {
        return changeStateTo(RequestState.DELETED)
                .withReason(reason);
    }

    public Request rejectWithReason(String reason) {
        return changeStateTo(RequestState.REJECTED)
                .withReason(reason);
    }

    public Request verify() {
        return changeStateTo(RequestState.VERIFIED);
    }

    public Request accept() {
        return changeStateTo(RequestState.ACCEPTED);
    }

    public Request publishWithNumber(LongSupplier publicationNumberSupplier) {
        return changeStateTo(RequestState.PUBLISHED)
                .withPublicationNumber(publicationNumberSupplier.getAsLong());
    }

    private Request changeStateTo(RequestState newState) {
        if (!state.isTransitionToAllowed(newState)) {
            throw InvalidRequestStateTransitionException.of(state, newState);
        }
        return withState(newState);
    }
}
