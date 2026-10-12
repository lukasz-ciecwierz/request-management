package io.github.lukaszciecwierz.requestmanagement.api

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.assertj.MockMvcTester
import org.springframework.test.web.servlet.assertj.MvcTestResult
import spock.lang.Specification
import tools.jackson.databind.ObjectMapper

import java.time.Instant

abstract class ApiTestSpec extends Specification {

    @Autowired
    MockMvcTester mvc

    static final ObjectMapper objectMapper = new ObjectMapper()

    void assertRequestIdentity(RequestResponse actual, RequestResponse original) {
        verifyAll(actual) {
            id == original.id
            name == original.name
            content == original.content
            created == original.created
            Instant.parse(created)
            Instant.parse(updated)
        }
    }

    static assertResultIs(MvcTestResult result, HttpStatus status, String detail, String instance, String errors = '') {
        if (errors) {
            errors = "\"errors\":${errors},"
        }

        result.assertThat()
                .hasStatus(status)
                .bodyJson()
                .isLenientlyEqualTo("""
{
    "title":"${status.reasonPhrase}",
    "status":${status.value()},
    "detail":"${detail}",
    ${errors}
    "instance":"${instance}"
}
""")
    }

    static Map<String, String> requestWithName(String name) {
        [name: name, content: UUID.randomUUID().toString()]
    }

    RequestResponse newRequestIsCreated(Map<String, String> req = newRequestRandomValues()) {
        def result = createNewRequest(req)
        is201WithRequestResponse(result)
    }

    RequestResponse newRequestIsCreatedAndVerified(Map<String, String> req = newRequestRandomValues()) {
        def created = newRequestIsCreated(req)
        is200WithRequestResponse(verifyRequest(created.id, 0))
    }

    RequestResponse newRequestIsCreatedAndAccepted(Map<String, String> req = newRequestRandomValues()) {
        def verified = newRequestIsCreatedAndVerified(req)
        is200WithRequestResponse(acceptRequest(verified.id, verified.version))
    }

    MvcTestResult createNewRequest(Map<String, String> request) {
        sendPost("/requests", request)
    }

    MvcTestResult updateRequest(String id, Integer version, String content) {
        def req = [version: version, content: content]
        mvc.patch()
                .uri("/requests/${id}")
                .content(objectMapper.writeValueAsString(req))
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
    }

    MvcTestResult getRequest(String id) {
        mvc.get()
                .uri("/requests/${id}")
                .exchange()
    }

    MvcTestResult getRequestHistory(String id) {
        mvc.get()
                .uri("/requests/${id}/history")
                .exchange()
    }

    MvcTestResult listRequests(Map<String, Object> params = [:]) {
        def reqBuilder = mvc.get()
                .uri("/requests")

        params.each { key, value ->
            reqBuilder.queryParam(key, value as String)
        }

        reqBuilder.exchange()
    }

    MvcTestResult deleteRequest(String id, Integer version, String reason = 'reason') {
        def req = [version: version, reason: reason]
        sendPost("/requests/${id}/delete", req)
    }

    MvcTestResult verifyRequest(String id, Integer version) {
        def req = [version: version]
        sendPost("/requests/${id}/verify", req)
    }

    MvcTestResult acceptRequest(String id, Integer version) {
        def req = [version: version]
        sendPost("/requests/${id}/accept", req)
    }

    MvcTestResult publishRequest(String id, Integer version) {
        def req = [version: version]
        sendPost("/requests/${id}/publish", req)
    }

    MvcTestResult rejectRequest(String id, Integer version, String reason = 'reason') {
        def req = [version: version, reason: reason]
        sendPost("/requests/${id}/reject", req)
    }

    MvcTestResult sendPost(String uri, Map request) {
        mvc.post()
                .uri(uri)
                .content(objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
    }

    static RequestResponse is201WithRequestResponse(MvcTestResult result) {
        assertResponse(result, RequestResponse, HttpStatus.CREATED)
    }

    static RequestResponse is200WithRequestResponse(MvcTestResult result) {
        assertResponse(result, RequestResponse)
    }

    static RequestsListResponse is200WithRequestsListResponse(MvcTestResult result) {
        assertResponse(result, RequestsListResponse)
    }

    static RequestHistoryResponse is200WithRequestHistoryResponse(MvcTestResult result) {
        assertResponse(result, RequestHistoryResponse)
    }

    static ProblemDetailResponse is409WithProblemDetailResponse(MvcTestResult result) {
        assertResponse(result, ProblemDetailResponse, HttpStatus.CONFLICT)
    }

    static <T> T assertResponse(MvcTestResult result, Class<T> responseType, HttpStatus expectedStatus = HttpStatus.OK) {
        result.assertThat()
                .hasStatus(expectedStatus)
                .bodyJson()
                .convertTo(responseType)
                .actual()
    }

    void assertHistory(String requestId, List<List<String>> expectedTransitions) {
        def history = is200WithRequestHistoryResponse(getRequestHistory(requestId))

        assert history.requestId == requestId
        assert history.stateChanges.collect {
            [it.stateFrom, it.stateTo]
        } == expectedTransitions

        verifyEach(history.stateChanges) {
            Instant.parse(created)
        }
    }

    static Map<String, String> newRequestRandomValues() {
        [name: UUID.randomUUID().toString(), content: UUID.randomUUID().toString()]
    }

    static class RequestResponse {
        String id
        String name
        String content
        String state
        Integer publicationNumber
        String reason
        String created
        String updated
        Integer version
    }

    static class ProblemDetailResponse {
        String title
        int status
        String detail
        String instance
    }

    static class RequestsListResponse {
        List<RequestSummaryResponse> content
        Page page

        static class RequestSummaryResponse {
            String id
            String name
            String state
            Long publicationNumber
            String created
            String updated
        }

        static class Page {
            int size
            int number
            int totalElements
            int totalPages
        }
    }

    static class RequestHistoryResponse {

        String requestId

        List<RequestStateChangeResponse> stateChanges

        static class RequestStateChangeResponse {
            String stateFrom
            String stateTo
            String created
        }
    }
}
