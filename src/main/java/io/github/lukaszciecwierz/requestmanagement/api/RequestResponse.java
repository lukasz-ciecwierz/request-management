package io.github.lukaszciecwierz.requestmanagement.api;

import lombok.Builder;
import lombok.Value;
import io.github.lukaszciecwierz.requestmanagement.domain.Request;

import java.util.Objects;

@Value
@Builder
class RequestResponse {

    String id;

    String name;

    String content;

    String state;

    Long publicationNumber;

    String reason;

    String created;

    String updated;

    int version;

    static RequestResponse from(Request request) {
        return RequestResponse.builder()
                .id(request.getId().asString())
                .name(request.getName())
                .content(request.getContent())
                .state(Objects.toString(request.getState(), null))
                .publicationNumber(request.getPublicationNumber())
                .reason(request.getReason())
                .created(Objects.toString(request.getCreated(), null))
                .updated(Objects.toString(request.getUpdated(), null))
                .version(request.getVersion())
                .build();
    }
}
