package io.github.lukaszciecwierz.requestmanagement.domain;

import jakarta.annotation.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface RequestRepository {

    Optional<Request> findById(RequestId id);

    Page<Request> findAll(@Nullable String name, @Nullable RequestState state, Pageable pageable);

    Request save(Request request);
}
