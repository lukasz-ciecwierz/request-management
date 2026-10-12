package io.github.lukaszciecwierz.requestmanagement.infra;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface RequestStateChangeSpringDataRepository extends JpaRepository<RequestStateChangeEntity, UUID> {

    List<RequestStateChangeEntity> findAllByRequestIdOrderByCreatedAscIdAsc(UUID requestId);
}
