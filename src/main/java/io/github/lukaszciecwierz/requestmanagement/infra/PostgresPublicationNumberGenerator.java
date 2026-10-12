package io.github.lukaszciecwierz.requestmanagement.infra;

import lombok.RequiredArgsConstructor;
import io.github.lukaszciecwierz.requestmanagement.domain.PublicationNumberGenerator;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class PostgresPublicationNumberGenerator implements PublicationNumberGenerator {

    private final JdbcClient jdbcClient;

    @Override
    public long next() {
        return jdbcClient
                .sql("select nextval('publication_number_seq')")
                .query(Long.class)
                .single();
    }
}
