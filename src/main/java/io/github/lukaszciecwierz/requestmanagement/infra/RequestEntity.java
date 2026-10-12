package io.github.lukaszciecwierz.requestmanagement.infra;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import io.github.lukaszciecwierz.requestmanagement.domain.Request;
import io.github.lukaszciecwierz.requestmanagement.domain.RequestId;
import io.github.lukaszciecwierz.requestmanagement.domain.RequestState;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Builder
@Table(name = "request")
@NoArgsConstructor
@AllArgsConstructor
class RequestEntity {

    @Id
    UUID id;

    String name;

    String content;

    @Enumerated(EnumType.STRING)
    RequestState state;

    Long publicationNumber;

    String reason;

    @CreationTimestamp
    Instant created;

    @UpdateTimestamp
    Instant updated;

    @Version
    int version;

    public Request toDomain() {
        return Request.builder()
                .id(RequestId.fromTrusted(id))
                .name(name)
                .content(content)
                .state(state)
                .publicationNumber(publicationNumber)
                .reason(reason)
                .created(created)
                .updated(updated)
                .version(version)
                .build();
    }

    public static RequestEntity from(Request request) {
        return builder()
                .id(request.getId().getValue())
                .name(request.getName())
                .content(request.getContent())
                .state(request.getState())
                .publicationNumber(request.getPublicationNumber())
                .reason(request.getReason())
                .created(request.getCreated())
                .version(request.getVersion())
                .build();
    }
}
