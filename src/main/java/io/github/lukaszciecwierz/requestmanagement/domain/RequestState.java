package io.github.lukaszciecwierz.requestmanagement.domain;

import java.util.List;
import java.util.Map;

public enum RequestState {
    CREATED,
    VERIFIED,
    ACCEPTED,
    PUBLISHED,
    REJECTED,
    DELETED;

    private static final Map<RequestState, List<RequestState>> ALLOWED_TRANSITIONS = Map.of(
            CREATED, List.of(DELETED, VERIFIED),
            VERIFIED, List.of(REJECTED, ACCEPTED),
            ACCEPTED, List.of(REJECTED, PUBLISHED),
            PUBLISHED, List.of(),
            REJECTED, List.of(),
            DELETED, List.of()
    );

    private static final List<RequestState> UPDATE_ALLOWED_STATES = List.of(
            RequestState.CREATED,
            RequestState.VERIFIED
    );

    public boolean isTransitionToAllowed(RequestState newState) {
        return ALLOWED_TRANSITIONS.get(this).contains(newState);
    }

    public boolean isUpdateAllowedForThisState() {
        return UPDATE_ALLOWED_STATES.contains(this);
    }
}
