package io.github.lukaszciecwierz.requestmanagement.infra;

import lombok.RequiredArgsConstructor;
import io.github.lukaszciecwierz.requestmanagement.domain.RequestId;
import io.github.lukaszciecwierz.requestmanagement.domain.RequestStateChange;
import io.github.lukaszciecwierz.requestmanagement.domain.RequestStateChangeRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
class RequestStateChangeJpaRepository implements RequestStateChangeRepository {

    private final RequestStateChangeSpringDataRepository repository;

    @Override
    public List<RequestStateChange> findAllByRequestId(RequestId requestId) {
        return repository.findAllByRequestIdOrderByCreatedAscIdAsc(requestId.getValue()).stream()
                .map(RequestStateChangeEntity::toDomain)
                .toList();
    }

    @Override
    public RequestStateChange save(RequestStateChange requestStateChange) {
        return repository.save(RequestStateChangeEntity.from(requestStateChange))
                .toDomain();
    }
}
