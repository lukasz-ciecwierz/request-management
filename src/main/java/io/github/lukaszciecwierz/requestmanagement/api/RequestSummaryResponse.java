package io.github.lukaszciecwierz.requestmanagement.api;

import lombok.Builder;
import lombok.Value;
import io.github.lukaszciecwierz.requestmanagement.domain.Request;

import java.util.Objects;

@Value
@Builder
class RequestSummaryResponse {

    String id;

    String name;

    String state;

    Long publicationNumber;

    String created;

    String updated;

    static RequestSummaryResponse from(Request request) {
        return RequestSummaryResponse.builder()
                .id(request.getId().asString())
                .name(request.getName())
                .state(Objects.toString(request.getState()))
                .publicationNumber(request.getPublicationNumber())
                .created(Objects.toString(request.getCreated(), null))
                .updated(Objects.toString(request.getUpdated(), null))
                .build();
    }
}
