package io.github.lukaszciecwierz.requestmanagement.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import io.github.lukaszciecwierz.requestmanagement.app.RequestService;
import io.github.lukaszciecwierz.requestmanagement.domain.RequestId;
import io.github.lukaszciecwierz.requestmanagement.domain.RequestState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class RequestController {

    private final RequestService requestService;

    @PostMapping("/requests")
    ResponseEntity<RequestResponse> createRequest(@Valid @RequestBody CreateRequest createRequest) {
        var request = requestService.createNew(createRequest.name(), createRequest.content());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(RequestResponse.from(request));
    }

    @GetMapping("/requests/{id}")
    ResponseEntity<RequestResponse> getRequest(@PathVariable String id) {
        var request = requestService.get(RequestId.parse(id));
        return ResponseEntity.ok(RequestResponse.from(request));
    }

    @GetMapping("/requests")
    PagedModel<RequestSummaryResponse> listRequests(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) RequestState state,
            Pageable pageable) {
        Page<RequestSummaryResponse> requests = requestService.findAll(name, state, pageable)
                .map(RequestSummaryResponse::from);
        return new PagedModel<>(requests);
    }

    @PatchMapping("/requests/{id}")
    ResponseEntity<RequestResponse> updateRequest(@PathVariable String id, @Valid @RequestBody UpdateRequest updateRequest) {
        var request = requestService.update(RequestId.parse(id), updateRequest.version(), updateRequest.content());
        return ResponseEntity.ok(RequestResponse.from(request));
    }

    @PostMapping("/requests/{id}/delete")
    ResponseEntity<RequestResponse> deleteRequest(@PathVariable String id, @Valid @RequestBody DeleteRequest deleteRequest) {
        var request = requestService.delete(RequestId.parse(id), deleteRequest.version(), deleteRequest.reason());
        return ResponseEntity.ok(RequestResponse.from(request));
    }

    @PostMapping("/requests/{id}/verify")
    ResponseEntity<RequestResponse> verifyRequest(@PathVariable String id, @Valid @RequestBody VersionRequest versionRequest) {
        var request = requestService.verify(RequestId.parse(id), versionRequest.version());
        return ResponseEntity.ok(RequestResponse.from(request));
    }

    @PostMapping("/requests/{id}/accept")
    ResponseEntity<RequestResponse> acceptRequest(@PathVariable String id, @Valid @RequestBody VersionRequest versionRequest) {
        var request = requestService.accept(RequestId.parse(id), versionRequest.version());
        return ResponseEntity.ok(RequestResponse.from(request));
    }

    @PostMapping("/requests/{id}/publish")
    ResponseEntity<RequestResponse> publishRequest(@PathVariable String id, @Valid @RequestBody VersionRequest versionRequest) {
        var request = requestService.publish(RequestId.parse(id), versionRequest.version());
        return ResponseEntity.ok(RequestResponse.from(request));
    }

    @PostMapping("/requests/{id}/reject")
    ResponseEntity<RequestResponse> rejectRequest(@PathVariable String id, @Valid @RequestBody RejectRequest rejectRequest) {
        var request = requestService.reject(RequestId.parse(id), rejectRequest.version(), rejectRequest.reason());
        return ResponseEntity.ok(RequestResponse.from(request));
    }

    @GetMapping("/requests/{id}/history")
    ResponseEntity<RequestHistoryResponse> getRequestHistory(@PathVariable String id) {
        var requestId = RequestId.parse(id);
        var requestStateChanges = requestService.getHistory(requestId);
        return ResponseEntity.ok(RequestHistoryResponse.from(requestId, requestStateChanges));
    }
}
