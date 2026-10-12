package io.github.lukaszciecwierz.requestmanagement.app;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import io.github.lukaszciecwierz.requestmanagement.domain.PublicationNumberGenerator;
import io.github.lukaszciecwierz.requestmanagement.domain.Request;
import io.github.lukaszciecwierz.requestmanagement.domain.RequestId;
import io.github.lukaszciecwierz.requestmanagement.domain.RequestNotFoundException;
import io.github.lukaszciecwierz.requestmanagement.domain.RequestRepository;
import io.github.lukaszciecwierz.requestmanagement.domain.RequestState;
import io.github.lukaszciecwierz.requestmanagement.domain.RequestStateChange;
import io.github.lukaszciecwierz.requestmanagement.domain.RequestStateChangeRepository;
import io.github.lukaszciecwierz.requestmanagement.domain.StaleVersionSentException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RequestService {

    private final RequestRepository requestRepository;

    private final RequestStateChangeRepository requestStateChangeRepository;

    private final PublicationNumberGenerator publicationNumberGenerator;

    @Transactional
    public Request createNew(String name, String content) {
        var request = Request.createNew(name, content);

        var saved = requestRepository.save(request);
        requestStateChangeRepository.save(RequestStateChange.forCreation(saved));

        return saved;
    }

    public Request get(RequestId id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> RequestNotFoundException.of(id));
    }

    private Request get(RequestId id, int version) {
        var request = get(id);

        if (request.getVersion() != version) {
            throw new StaleVersionSentException();
        }

        return request;
    }

    public Page<Request> findAll(@Nullable String name, @Nullable RequestState state, Pageable pageable) {
        return requestRepository.findAll(name, state, pageable);
    }

    public Request update(RequestId id, int version, String content) {
        var request = get(id, version);

        request = request.update(content);
        return requestRepository.save(request);
    }

    @Transactional
    public Request delete(RequestId id, int version, String reason) {
        var request = get(id, version);
        var deleted = request.deleteWithReason(reason);

        var saved = requestRepository.save(deleted);
        requestStateChangeRepository.save(RequestStateChange.forTransition(request, saved));

        return saved;
    }

    @Transactional
    public Request reject(RequestId id, int version, String reason) {
        var request = get(id, version);
        var rejected = request.rejectWithReason(reason);

        var saved = requestRepository.save(rejected);
        requestStateChangeRepository.save(RequestStateChange.forTransition(request, saved));

        return saved;
    }

    @Transactional
    public Request verify(RequestId id, int version) {
        var request = get(id, version);
        var verified = request.verify();

        var saved = requestRepository.save(verified);
        requestStateChangeRepository.save(RequestStateChange.forTransition(request, saved));

        return saved;
    }

    @Transactional
    public Request accept(RequestId id, int version) {
        var request = get(id, version);
        var accepted = request.accept();

        var saved = requestRepository.save(accepted);
        requestStateChangeRepository.save(RequestStateChange.forTransition(request, saved));

        return saved;
    }

    @Transactional
    public Request publish(RequestId id, int version) {
        var request = get(id, version);
        var published = request.publishWithNumber(publicationNumberGenerator::next);

        var saved = requestRepository.save(published);
        requestStateChangeRepository.save(RequestStateChange.forTransition(request, saved));

        return saved;
    }

    public List<RequestStateChange> getHistory(RequestId id) {
        get(id);
        return requestStateChangeRepository.findAllByRequestId(id);
    }
}
