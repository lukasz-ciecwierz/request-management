package io.github.lukaszciecwierz.requestmanagement.domain;

import java.util.List;

public interface RequestStateChangeRepository {

    List<RequestStateChange> findAllByRequestId(RequestId id);

    RequestStateChange save(RequestStateChange requestStateChange);
}
