package io.github.lukaszciecwierz.requestmanagement.infra;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import io.github.lukaszciecwierz.requestmanagement.domain.Request;
import io.github.lukaszciecwierz.requestmanagement.domain.RequestId;
import io.github.lukaszciecwierz.requestmanagement.domain.RequestRepository;
import io.github.lukaszciecwierz.requestmanagement.domain.RequestState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
class RequestJpaRepository implements RequestRepository {

    private final RequestSpringDataRepository repository;

    @Override
    public Optional<Request> findById(RequestId id) {
        return repository.findById(id.getValue())
                .map(RequestEntity::toDomain);
    }

    @Override
    public Page<Request> findAll(@Nullable String name, @Nullable RequestState state, Pageable pageable) {
        return repository.findAll(RequestSpecifications.matching(name, state), withFixedSortOrder(pageable))
                .map(RequestEntity::toDomain);
    }

    @Override
    public Request save(Request request) {
        // flush is necessary so service's transactional methods return updated request
        return repository.saveAndFlush(RequestEntity.from(request))
                .toDomain();
    }

    private Pageable withFixedSortOrder(Pageable pageable) {
        return PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(
                        Sort.Order.desc(RequestEntity_.CREATED),
                        Sort.Order.asc(RequestEntity_.ID)
                )
        );
    }
}
