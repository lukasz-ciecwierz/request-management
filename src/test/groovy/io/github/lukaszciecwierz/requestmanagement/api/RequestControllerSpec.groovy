package io.github.lukaszciecwierz.requestmanagement.api

import io.github.lukaszciecwierz.requestmanagement.TestConfig
import io.github.lukaszciecwierz.requestmanagement.domain.RequestState
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType

import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@SpringBootTest(properties = ["spring.data.web.pageable.max-page-size=20"])
@AutoConfigureMockMvc
@Import(TestConfig)
class RequestControllerSpec extends ApiTestSpec {

    static final int MAX_NAME_LENGTH = 100
    static final int MAX_CONTENT_LENGTH = 1000
    static final int MAX_REASON_LENGTH = 500

    static final int DEFAULT_PAGE_NUMBER = 0
    static final int DEFAULT_PAGE_SIZE = 10
    static final int MAX_PAGE_SIZE = 20 //test value

    def 'creating request returns its details'() {
        given:
            def request = newRequestRandomValues()

        when:
            def result = createNewRequest(request)

        then:
            def response = is201WithRequestResponse(result)
            with(response) {
                id
                name == request.name
                content == request.content
                state == 'CREATED'
                publicationNumber == null
                reason == null
                Instant.parse(created)
                Instant.parse(updated)
                version == 0
            }

        and:
            assertHistory(response.id, [
                    [null, 'CREATED'],
            ])
    }

    def 'request with #type field lengths is created'() {
        given:
            def request = [name: name, content: content]

        when:
            def result = createNewRequest(request)

        then:
            def response = is201WithRequestResponse(result)
            response.name == request.name
            response.content == request.content

        where:
            name                  | content                  | type
            'a'                   | 'b'                      | 'min'
            'a' * MAX_NAME_LENGTH | 'b' * MAX_CONTENT_LENGTH | 'max'
    }

    def 'creating request fails for invalid value with 400: #errorType'() {
        given:
            def request = [name: name, content: content]

        when:
            def result = createNewRequest(request)

        then:
            assertResultIs(result,
                    HttpStatus.BAD_REQUEST,
                    "Invalid request",
                    "/requests",
                    '[{"field":"' + errorField + '", "message":"' + errorMessage + '"}]'
            )

        where:
            name                        | content                        | errorField | errorMessage                       | errorType
            null                        | 'b'                            | 'name'     | 'Value is required'                | 'missing name'
            'a'                         | null                           | 'content'  | 'Value is required'                | 'missing content'
            ''                          | 'b'                            | 'name'     | 'Value is required'                | 'empty name'
            'a'                         | ''                             | 'content'  | 'Value is required'                | 'empty content'
            ' '                         | 'b'                            | 'name'     | 'Value is required'                | 'blank name'
            'a'                         | ' '                            | 'content'  | 'Value is required'                | 'blank content'
            'a' * (MAX_NAME_LENGTH + 1) | 'b'                            | 'name'     | 'Value must be at most 100 chars'  | 'name too long'
            'a'                         | 'b' * (MAX_CONTENT_LENGTH + 1) | 'content'  | 'Value must be at most 1000 chars' | 'content too long'
    }

    def 'request\'s content can be updated: #type'() {
        given:
            def createdRequest = newRequestIsCreated()

        when:
            def result = updateRequest(createdRequest.id, 0, newContent)

        then:
            def updatedRequest = is200WithRequestResponse(result)
            updatedRequest.content != createdRequest.content
            updatedRequest.content == newContent
            with(updatedRequest) {
                id == createdRequest.id
                name == createdRequest.name
                state == 'CREATED'
                publicationNumber == null
                reason == null
                created == createdRequest.created
                Instant.parse(created)
                updated != createdRequest.updated
                Instant.parse(updated)
                version == 1
            }

        and:
            assertHistory(updatedRequest.id, [
                    [null, 'CREATED'],
            ])

        where:
            [newContent, type] << [['b', 'min'], ['b' * MAX_CONTENT_LENGTH, 'max']]
    }

    def 'updating content fails for invalid values: #errorType'() {
        given:
            def createdRequest = newRequestIsCreated()

        when:
            def result = updateRequest(createdRequest.id, 0, content)

        then:
            assertResultIs(result,
                    HttpStatus.BAD_REQUEST,
                    "Invalid request",
                    "/requests/${createdRequest.id}",
                    '[{"field":"content", "message":"' + errorMessage + '"}]'
            )

        where:
            content                        | errorMessage                       | errorType
            null                           | 'Value is required'                | 'value missing'
            ''                             | 'Value is required'                | 'empty value'
            ' '                            | 'Value is required'                | 'blank value'
            'a' * (MAX_CONTENT_LENGTH + 1) | 'Value must be at most 1000 chars' | 'value too long'
    }

    def 'updating content when request\'s state forbids it is rejected with 422'() {
        given:
            def createdRequest = newRequestIsCreated()
            is200WithRequestResponse(deleteRequest(createdRequest.id, 0))

        when:
            def result = updateRequest(createdRequest.id, 1, 'new content')

        then:
            assertResultIs(result,
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "Request update for state DELETED is forbidden",
                    "/requests/${createdRequest.id}"
            )
    }

    def 'reason field length is valid on delete: #type'() {
        given:
            def created = newRequestIsCreated()

        when:
            def result = deleteRequest(created.id, 0, reason)

        then:
            def response = is200WithRequestResponse(result)
            response.reason == reason

        where:
            [reason, type] << [['a', 'min'], ['a' * MAX_REASON_LENGTH, 'max']]
    }

    def 'reason field length is valid on reject: #type'() {
        given:
            def verified = newRequestIsCreatedAndVerified()

        when:
            def result = rejectRequest(verified.id, 1, reason)

        then:
            def response = is200WithRequestResponse(result)
            response.reason == reason

        where:
            [reason, type] << [['a', 'min'], ['a' * MAX_REASON_LENGTH, 'max']]
    }

    def 'reason field is mandatory: #errorType'() {
        given:
            def created = newRequestIsCreated()
            def verified = newRequestIsCreatedAndVerified()

        when:
            def notDeleted = deleteRequest(created.id, 0, reason)
            def notRejected = rejectRequest(verified.id, 1, reason)

        then:
            assertResultIs(notDeleted,
                    HttpStatus.BAD_REQUEST,
                    "Invalid request",
                    "/requests/${created.id}/delete",
                    '[{"field":"reason", "message":"' + errorMessage + '"}]'
            )
            assertResultIs(notRejected,
                    HttpStatus.BAD_REQUEST,
                    "Invalid request",
                    "/requests/${verified.id}/reject",
                    '[{"field":"reason", "message":"' + errorMessage + '"}]'
            )

        and:
            assertHistory(created.id, [
                    [null, 'CREATED'],
            ])
            assertHistory(verified.id, [
                    [null, 'CREATED'],
                    ['CREATED', 'VERIFIED'],
            ])

        where:
            reason                        | errorMessage                      | errorType
            null                          | 'Value is required'               | 'value missing'
            ''                            | 'Value is required'               | 'empty value'
            ' '                           | 'Value is required'               | 'blank value'
            'a' * (MAX_REASON_LENGTH + 1) | 'Value must be at most 500 chars' | 'value too long'
    }

    def 'existing request details can be retrieved'() {
        given:
            def request = newRequestIsCreated()

        when:
            def result = getRequest(request.id)

        then:
            def response = is200WithRequestResponse(result)
            assertRequestIdentity(response, request)
            with(response) {
                state == 'CREATED'
                publicationNumber == null
                reason == null
                updated == request.updated
                version == 0
            }
    }

    def 'retrieving non-existing request returns 404'() {
        given:
            def unknownRequestId = UUID.randomUUID().toString()

        when:
            def result = getRequest(unknownRequestId)

        then:
            assertResultIs(result,
                    HttpStatus.NOT_FOUND,
                    "Request not found for id: ${unknownRequestId}",
                    "/requests/${unknownRequestId}"
            )
    }

    def 'performing forbidden state transition returns 422 and request is not modified'() {
        given:
            def request = newRequestIsCreated()

        when:
            def result = rejectRequest(request.id, request.version)

        then:
            assertResultIs(result,
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "Request state transition from ${request.state} to REJECTED is not allowed.",
                    "/requests/${request.id}/reject"
            )

        and:
            def response = is200WithRequestResponse(getRequest(request.id))
            assertRequestIdentity(response, request)
            with(response) {
                state == 'CREATED'
                publicationNumber == null
                reason == null
                updated == request.updated
                version == 0
            }

        and:
            assertHistory(request.id, [
                    [null, 'CREATED'],
            ])
    }

    def 'request can be deleted and history of state changes is recorded'() {
        given:
            def deleteReason = UUID.randomUUID().toString()
            def request = newRequestIsCreated()

        when:
            def result = deleteRequest(request.id, request.version, deleteReason)

        then:
            def deleted = is200WithRequestResponse(result)
            assertRequestIdentity(deleted, request)
            with(deleted) {
                state == 'DELETED'
                publicationNumber == null
                reason == deleteReason
                version == 1
            }

        and:
            assertHistory(deleted.id, [
                    [null, 'CREATED'],
                    ['CREATED', 'DELETED'],
            ])
    }

    def 'request can be verified -> accepted -> published and history of state changes is recorded'() {
        given:
            def request = newRequestIsCreated()

        when:
            def result = verifyRequest(request.id, request.version)

        then:
            def verified = is200WithRequestResponse(result)
            assertRequestIdentity(verified, request)
            with(verified) {
                state == 'VERIFIED'
                publicationNumber == null
                reason == null
                version == 1
            }

        when:
            result = acceptRequest(request.id, verified.version)

        then:
            def accepted = is200WithRequestResponse(result)
            assertRequestIdentity(accepted, request)
            with(accepted) {
                state == 'ACCEPTED'
                publicationNumber == null
                reason == null
                version == 2
            }

        when:
            result = publishRequest(request.id, accepted.version)

        then:
            def published = is200WithRequestResponse(result)
            assertRequestIdentity(published, request)
            with(published) {
                state == 'PUBLISHED'
                publicationNumber > 0
                reason == null
                version == 3
            }

        and:
            assertHistory(published.id, [
                    [null, 'CREATED'],
                    ['CREATED', 'VERIFIED'],
                    ['VERIFIED', 'ACCEPTED'],
                    ['ACCEPTED', 'PUBLISHED'],
            ])
    }

    def 'request can be verified -> accepted -> rejected and history of state changes is recorded'() {
        given:
            def accepted = newRequestIsCreatedAndAccepted()
            def rejectReason = UUID.randomUUID().toString()

        when:
            def rejected = is200WithRequestResponse(rejectRequest(accepted.id, accepted.version, rejectReason))

        then:
            assertRequestIdentity(accepted, rejected)
            with(rejected) {
                state == 'REJECTED'
                publicationNumber == null
                reason == rejectReason
                version == 3
            }

        and:
            assertHistory(rejected.id, [
                    [null, 'CREATED'],
                    ['CREATED', 'VERIFIED'],
                    ['VERIFIED', 'ACCEPTED'],
                    ['ACCEPTED', 'REJECTED'],
            ])
    }

    def 'request can be verified -> rejected and history of state changes is recorded'() {
        given:
            def rejectReason = UUID.randomUUID().toString()
            def request = newRequestIsCreated()

        when:
            def result = verifyRequest(request.id, request.version)

        then:
            def verified = is200WithRequestResponse(result)
            assertRequestIdentity(verified, request)
            with(verified) {
                state == 'VERIFIED'
                publicationNumber == null
                reason == null
                version == 1
            }

        when:
            result = rejectRequest(request.id, verified.version, rejectReason)

        then:
            def rejected = is200WithRequestResponse(result)
            assertRequestIdentity(rejected, request)
            with(rejected) {
                state == 'REJECTED'
                publicationNumber == null
                reason == rejectReason
                version == 2
            }

        and:
            assertHistory(rejected.id, [
                    [null, 'CREATED'],
                    ['CREATED', 'VERIFIED'],
                    ['VERIFIED', 'REJECTED'],
            ])
    }

    def 'concurrent publishes get distinct publication numbers'() {
        given:
            def numberOfClients = 20
            def pool = Executors.newFixedThreadPool(numberOfClients)
            def latch = new CountDownLatch(1)

            def acceptedRequests = (1..numberOfClients).collect {
                newRequestIsCreatedAndAccepted()
            }

        when:
            def futures = acceptedRequests.collect { request ->
                pool.submit {
                    latch.await()
                    is200WithRequestResponse(publishRequest(request.id, request.version))
                }
            }
            latch.countDown()

            def publishedRequests = futures*.get(5, TimeUnit.SECONDS)

        then:
            verifyEach(publishedRequests) {
                it.state == RequestState.PUBLISHED.name()
                it.publicationNumber > 0
            }
            def publicationNumbers = publishedRequests*.publicationNumber.toSet()
            publicationNumbers.size() == numberOfClients

        cleanup:
            pool.shutdownNow()
    }

    def 'concurrent modification of single request succeeds for one client and is rejected with 409 for others'() {
        given:
            def numberOfClients = 20
            def pool = Executors.newFixedThreadPool(numberOfClients)
            def latch = new CountDownLatch(1)

            def request = newRequestIsCreated()

        when:
            def futures = (1..numberOfClients).collect {
                pool.submit {
                    latch.await()
                    verifyRequest(request.id, request.version)
                }
            }
            latch.countDown()

            def results = futures*.get(5, TimeUnit.SECONDS)

        then:
            results.countBy {
                it.response.status
            } == [200: 1, 409: numberOfClients - 1]

            def conflictResults = results.findAll {
                it.getResponse().status == HttpStatus.CONFLICT.value()
            }
            def problemDetails = conflictResults.collect {
                is409WithProblemDetailResponse(it)
            }

            verifyEach(problemDetails) {
                detail == 'Sent version number is stale, refresh and try again'
            }

        and:
            with(is200WithRequestResponse(getRequest(request.id))) {
                state == 'VERIFIED'
                version == 1
            }

        and:
            assertHistory(request.id, [
                    [null, 'CREATED'],
                    ['CREATED', 'VERIFIED'],
            ])

        cleanup:
            pool.shutdownNow()
    }

    def 'using stale version number returns 409 error'() {
        given:
            def deleteReason1 = UUID.randomUUID().toString()
            def deleteReason2 = UUID.randomUUID().toString()
            def request = newRequestIsCreated()

        when:
            def result1 = deleteRequest(request.id, request.version, deleteReason1)
            def result2 = deleteRequest(request.id, request.version, deleteReason2)

        then:
            result1.assertThat()
                    .hasStatusOk()
            assertResultIs(result2,
                    HttpStatus.CONFLICT,
                    "Sent version number is stale, refresh and try again",
                    "/requests/${request.id}/delete"
            )

        and:
            def deleted = is200WithRequestResponse(getRequest(request.id))
            assertRequestIdentity(deleted, request)
            with(deleted) {
                state == 'DELETED'
                publicationNumber == null
                reason == deleteReason1
                version == 1
            }

        and:
            assertHistory(deleted.id, [
                    [null, 'CREATED'],
                    ['CREATED', 'DELETED'],
            ])
    }

    def 'malformed request ids are rejected with 400'() {
        given:
            def malformedId = 'abc'

        when:
            def result = getRequest(malformedId)

        then:
            assertResultIs(result,
                    HttpStatus.BAD_REQUEST,
                    "Malformed request id: ${malformedId}",
                    "/requests/${malformedId}"
            )
    }

    def 'missing version is rejected with 400 on #action'() {
        given:
            def requestId = UUID.randomUUID().toString()

        when:
            def result = sendPost("/requests/${requestId}/${action}", [reason: 'reason'])

        then:
            assertResultIs(result,
                    HttpStatus.BAD_REQUEST,
                    "Invalid request",
                    "/requests/${requestId}/${action}",
                    '[{"field":"version", "message":"Value is required"}]'
            )

        where:
            action << ['delete', 'verify', 'reject', 'accept', 'publish']
    }

    def 'invalid version is rejected with 400 on update: #errorType'() {
        given:
            def created = newRequestIsCreatedAndVerified()

        when:
            def result = updateRequest(created.id, version, 'new content')

        then:
            assertResultIs(result,
                    HttpStatus.BAD_REQUEST,
                    "Invalid request",
                    "/requests/${created.id}",
                    '[{"field":"version", "message":"' + errorMessage + '"}]'
            )

        where:
            version | errorMessage                               | errorType
            null    | 'Value is required'                        | 'missing value'
            -1      | 'Only zero or positive integers are valid' | 'negative value'
    }

    def 'negative version is rejected with 400 on #action'() {
        given:
            def negativeVersion = -1
            def requestId = UUID.randomUUID().toString()

        when:
            def result = sendPost("/requests/${requestId}/${action}", [version: negativeVersion, reason: 'reason'])

        then:
            assertResultIs(result,
                    HttpStatus.BAD_REQUEST,
                    "Invalid request",
                    "/requests/${requestId}/${action}",
                    '[{"field":"version", "message":"Only zero or positive integers are valid"}]'
            )

        where:
            action << ['delete', 'verify', 'reject', 'accept', 'publish']
    }

    def 'malformed JSON is rejected with 400'() {
        given:
            def notJsonBody = '{"foo":"bar"'
            def requestId = UUID.randomUUID().toString()

        when:
            def result = mvc.post()
                    .uri("/requests/${requestId}/delete")
                    .content(notJsonBody)
                    .contentType(MediaType.APPLICATION_JSON)
                    .exchange()

        then:
            assertResultIs(result,
                    HttpStatus.BAD_REQUEST,
                    "Failed to read request",
                    "/requests/${requestId}/delete"
            )
    }

    def 'listing requests works without any parameter provided'() {
        given:
            def requestCreated = newRequestIsCreated()
            def requestAccepted = newRequestIsCreatedAndAccepted()

        when:
            def result = listRequests()

        then:
            def requestsListResponse = is200WithRequestsListResponse(result)
            def foundRequestIds = requestsListResponse.content*.id
            foundRequestIds.containsAll([requestCreated.id, requestAccepted.id])

            with(requestsListResponse.page) {
                number == DEFAULT_PAGE_NUMBER
                size == DEFAULT_PAGE_SIZE
            }
    }

    def 'listing requests rejects invalid state param with 400'() {
        given:
            def invalidState = 'FOO'

        when:
            def result = listRequests([state: invalidState])

        then:
            assertResultIs(result,
                    HttpStatus.BAD_REQUEST,
                    "Failed to convert 'state' with value: '${invalidState}'",
                    "/requests"
            )
    }

    def 'listing requests respects page size up to max value and caps larger values'() {
        given:
            (1..MAX_PAGE_SIZE + 1).each {
                newRequestIsCreated()
            }

        when:
            def pageSizeMaxResults = listRequests([size: MAX_PAGE_SIZE])
            def pageSizeOverMaxResult = listRequests([size: MAX_PAGE_SIZE + 1])

        then:
            def pageSizeMaxResponse = is200WithRequestsListResponse(pageSizeMaxResults)
            def pageSizeOverMaxResponse = is200WithRequestsListResponse(pageSizeOverMaxResult)

            verifyEach([pageSizeMaxResponse, pageSizeOverMaxResponse]) {
                page.size == MAX_PAGE_SIZE
                content.size() == MAX_PAGE_SIZE
            }
    }

    def 'listing requests accepts both parameters at once and searching by name is case insensitive'() {
        given:
            def commonSubstring = UUID.randomUUID().toString()
            def name = "before ${commonSubstring.toUpperCase()} after"

            def created = newRequestIsCreated(requestWithName(name))
            def verified = newRequestIsCreatedAndVerified(requestWithName(name))
            def accepted = newRequestIsCreatedAndAccepted(requestWithName(name))

        when:
            def result = listRequests([name: commonSubstring, state: 'VERIFIED'])
            def resultWithoutState = listRequests([name: commonSubstring])

        then:
            def response = is200WithRequestsListResponse(result)
            def responseWithoutState = is200WithRequestsListResponse(resultWithoutState)

            response.content*.id == [verified.id]

            def expectedIds = [created.id, verified.id, accepted.id].toSet()
            responseWithoutState.content.size() == 3
            responseWithoutState.content*.id.toSet() == expectedIds
    }

    def 'searching by name treats _ and % as literal characters'() {
        given:
            def token = UUID.randomUUID().toString()
            def literalName = "${token} a_c 50%"
            def literal = newRequestIsCreated(requestWithName(literalName))
            newRequestIsCreated(requestWithName("${token} abc 50%")) //won't be found because _ is treated as literal
            newRequestIsCreated(requestWithName("${token} a_c 50xx")) //won't be found because % is treated as literal

        when:
            def result = listRequests([name: literalName])

        then:
            is200WithRequestsListResponse(result).content*.id == [literal.id]
    }

    def 'listing requests returns them in descending order by created time'() {
        given:
            def name = UUID.randomUUID().toString()
            def created1 = newRequestIsCreated(requestWithName(name))
            def created2 = newRequestIsCreated(requestWithName(name))
            def created3 = newRequestIsCreated(requestWithName(name))
        and:
            def expectedOrder = [created3.id, created2.id, created1.id]

        when:
            def result = listRequests([name: name])

        then:
            def response = is200WithRequestsListResponse(result)
            response.content*.id == expectedOrder
    }

    def "listing request's history is rejected with 400 for invalid id and 404 for unknown request"() {
        given:
            def invalidId = 'foo'
            def unknownId = UUID.randomUUID().toString()

        when:
            def responseInvalidId = getRequestHistory(invalidId)
            def responseUnknownId = getRequestHistory(unknownId)

        then:
            assertResultIs(responseInvalidId,
                    HttpStatus.BAD_REQUEST,
                    "Malformed request id: ${invalidId}",
                    "/requests/${invalidId}/history"
            )
            assertResultIs(responseUnknownId,
                    HttpStatus.NOT_FOUND,
                    "Request not found for id: ${unknownId}",
                    "/requests/${unknownId}/history"
            )
    }
}
