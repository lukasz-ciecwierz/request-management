package io.github.lukaszciecwierz.requestmanagement.api;

import lombok.Builder;
import lombok.Value;
import io.github.lukaszciecwierz.requestmanagement.domain.RequestId;
import io.github.lukaszciecwierz.requestmanagement.domain.RequestStateChange;

import java.util.List;
import java.util.Objects;

@Value
@Builder
class RequestHistoryResponse {

    String requestId;

    List<RequestStateChangeResponse> stateChanges;

    @Value
    @Builder
    static class RequestStateChangeResponse {

        String stateFrom;

        String stateTo;

        String created;

        private static RequestStateChangeResponse from(RequestStateChange requestStateChange) {
            return RequestStateChangeResponse.builder()
                    .stateFrom(requestStateChange.getStateFrom().map(Objects::toString).orElse(null))
                    .stateTo(Objects.toString(requestStateChange.getStateTo(), null))
                    .created(Objects.toString(requestStateChange.getCreated(), null))
                    .build();
        }
    }

    public static RequestHistoryResponse from(RequestId requestId, List<RequestStateChange> requestStateChanges) {
        var stateChangesResponses = requestStateChanges.stream()
                .map(RequestStateChangeResponse::from)
                .toList();
        return builder()
                .requestId(requestId.asString())
                .stateChanges(stateChangesResponses)
                .build();
    }
}
