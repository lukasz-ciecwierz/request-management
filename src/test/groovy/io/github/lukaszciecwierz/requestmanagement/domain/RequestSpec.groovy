package io.github.lukaszciecwierz.requestmanagement.domain

import spock.lang.Specification

import java.util.function.LongSupplier

import static org.assertj.core.api.Assertions.assertThatThrownBy

class RequestSpec extends Specification {

    private static final String REASON = "reason"
    private static final LongSupplier PUBLICATION_NUMBER_SUPPLIER = () -> 123L

    def 'verify allowed transitions from state CREATED'() {
        given:
            def request = newRequest()

            with(request) {
                state == RequestState.CREATED
                reason == null
                publicationNumber == null
            }

        expect:
            // -> DELETED
            with(request.deleteWithReason(REASON)) {
                state == RequestState.DELETED
                reason == REASON
            }

            // -> REJECTED
            throwsInvalidTransitionException(() -> request.rejectWithReason(REASON))

            // -> VERIFIED
            with(request.verify()) {
                state == RequestState.VERIFIED
                reason == null
                publicationNumber == null
            }

            // -> ACCEPTED
            throwsInvalidTransitionException(() -> request.accept())

            // -> PUBLISHED
            throwsInvalidTransitionException(() -> request.publishWithNumber(PUBLICATION_NUMBER_SUPPLIER))
    }

    def 'verify allowed transitions from state VERIFIED'() {
        given:
            def request = verifiedRequest()

            with(request) {
                state == RequestState.VERIFIED
                reason == null
                publicationNumber == null
            }

        expect:
            // -> DELETED
            throwsInvalidTransitionException(() -> request.deleteWithReason(REASON))

            // -> REJECTED
            with(request.rejectWithReason(REASON)) {
                state == RequestState.REJECTED
                reason == REASON
                publicationNumber == null
            }

            // -> VERIFIED
            throwsInvalidTransitionException(() -> request.verify())

            // -> ACCEPTED
            with(request.accept()) {
                state == RequestState.ACCEPTED
                reason == null
                publicationNumber == null
            }

            // -> PUBLISHED
            throwsInvalidTransitionException(() -> request.publishWithNumber(PUBLICATION_NUMBER_SUPPLIER))
    }

    def 'verify allowed transitions from state ACCEPTED'() {
        given:
            def request = acceptedRequest()

            with(request) {
                state == RequestState.ACCEPTED
                reason == null
                publicationNumber == null
            }

        expect:
            // -> DELETED
            throwsInvalidTransitionException(() -> request.deleteWithReason(REASON))

            // -> REJECTED
            with(request.rejectWithReason(REASON)) {
                state == RequestState.REJECTED
                reason == REASON
                publicationNumber == null
            }

            // -> VERIFIED
            throwsInvalidTransitionException(() -> request.verify())

            // -> ACCEPTED
            throwsInvalidTransitionException(() -> request.accept())

            // -> PUBLISHED
            with(request.publishWithNumber(PUBLICATION_NUMBER_SUPPLIER)) {
                state == RequestState.PUBLISHED
                reason == null
                publicationNumber == PUBLICATION_NUMBER_SUPPLIER.getAsLong()
            }
    }

    def 'verify allowed transitions from state PUBLISHED'() {
        given:
            def request = publishedRequest(PUBLICATION_NUMBER_SUPPLIER.getAsLong())

            with(request) {
                state == RequestState.PUBLISHED
                reason == null
                publicationNumber == PUBLICATION_NUMBER_SUPPLIER.getAsLong()
            }

        expect:
            noTransitionsAreAllowed(request)
    }

    def 'verify allowed transitions from state REJECTED'() {
        given:
            def request = rejectedRequest(REASON)

            with(request) {
                state == RequestState.REJECTED
                reason == REASON
                publicationNumber == null
            }

        expect:
            noTransitionsAreAllowed(request)
    }

    def 'verify allowed transitions from state DELETED'() {
        given:
            def request = deletedRequest(REASON)

            with(request) {
                state == RequestState.DELETED
                reason == REASON
                publicationNumber == null
            }

        expect:
            noTransitionsAreAllowed(request)
    }

    def 'update is allowed for state: #state'() {
        given:
            def newContent = UUID.randomUUID().toString()

        when:
            def updatedRequest = request.update(newContent)

        then:
            updatedRequest.getContent() == newContent

        where:
            state                 | request
            RequestState.CREATED  | newRequest()
            RequestState.VERIFIED | verifiedRequest()
    }

    def 'update is forbidden for state: #state'() {
        given:
            def newContent = UUID.randomUUID().toString()

        when:
            request.update(newContent)

        then:
            def ex = thrown(RequestUpdateForbiddenException)
            ex.message == "Request update for state ${state} is forbidden"

        where:
            state                  | request
            RequestState.DELETED   | deletedRequest(REASON)
            RequestState.REJECTED  | rejectedRequest(REASON)
            RequestState.ACCEPTED  | acceptedRequest()
            RequestState.PUBLISHED | publishedRequest(PUBLICATION_NUMBER_SUPPLIER.getAsLong())
    }

    private static Request newRequest() {
        Request.createNew("a", "b")
    }

    private static Request verifiedRequest() {
        newRequest()
                .verify()
    }

    private static Request acceptedRequest() {
        verifiedRequest()
                .accept()
    }

    private static Request publishedRequest(long publicationNumber) {
        acceptedRequest()
                .publishWithNumber(() -> publicationNumber)
    }

    private static Request rejectedRequest(String reason) {
        verifiedRequest()
                .rejectWithReason(reason)
    }

    private static Request deletedRequest(String reason) {
        newRequest()
                .deleteWithReason(reason)
    }

    static def throwsInvalidTransitionException(Closure<Request> requestClosure) {
        assertThatThrownBy { requestClosure.run() }
                .isExactlyInstanceOf(InvalidRequestStateTransitionException)
    }

    static def noTransitionsAreAllowed(Request request) {
        // -> DELETED
        throwsInvalidTransitionException(() -> request.deleteWithReason(REASON))
                .hasMessage("Request state transition from %s to %s is not allowed.", request.getState(), RequestState.DELETED)

        // -> REJECTED
        throwsInvalidTransitionException(() -> request.rejectWithReason(REASON))
                .hasMessage("Request state transition from %s to %s is not allowed.", request.getState(), RequestState.REJECTED)

        // -> VERIFIED
        throwsInvalidTransitionException(() -> request.verify())
                .hasMessage("Request state transition from %s to %s is not allowed.", request.getState(), RequestState.VERIFIED)

        // -> ACCEPTED
        throwsInvalidTransitionException(() -> request.accept())
                .hasMessage("Request state transition from %s to %s is not allowed.", request.getState(), RequestState.ACCEPTED)

        // -> PUBLISHED
        throwsInvalidTransitionException(() -> request.publishWithNumber(PUBLICATION_NUMBER_SUPPLIER))
                .hasMessage("Request state transition from %s to %s is not allowed.", request.getState(), RequestState.PUBLISHED)
    }
}
