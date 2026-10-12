package io.github.lukaszciecwierz.requestmanagement.infra;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import io.github.lukaszciecwierz.requestmanagement.domain.RequestId;
import io.github.lukaszciecwierz.requestmanagement.domain.RequestState;
import io.github.lukaszciecwierz.requestmanagement.domain.RequestStateChange;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Builder
@Table(name = "request_state_change")
@NoArgsConstructor
@AllArgsConstructor
class RequestStateChangeEntity {

    @Id
    UUID id;

    UUID requestId;

    @Enumerated(EnumType.STRING)
    RequestState stateFrom;

    @Enumerated(EnumType.STRING)
    RequestState stateTo;

    @CreationTimestamp
    Instant created;

    public RequestStateChange toDomain() {
        return RequestStateChange.builder()
                .id(id)
                .requestId(RequestId.fromTrusted(requestId))
                .stateFrom(stateFrom)
                .stateTo(stateTo)
                .created(created)
                .build();
    }

    public static RequestStateChangeEntity from(RequestStateChange requestStateChange) {
        return builder()
                .id(requestStateChange.getId())
                .requestId(requestStateChange.getRequestId().getValue())
                .stateFrom(requestStateChange.getStateFrom().orElse(null))
                .stateTo(requestStateChange.getStateTo())
                .created(requestStateChange.getCreated())
                .build();
    }
}
