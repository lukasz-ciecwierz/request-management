package io.github.lukaszciecwierz.requestmanagement.api

import io.github.lukaszciecwierz.requestmanagement.TestConfig
import io.github.lukaszciecwierz.requestmanagement.domain.RequestStateChange
import io.github.lukaszciecwierz.requestmanagement.domain.RequestStateChangeRepository
import org.spockframework.spring.SpringBean
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestConfig)
class RequestControllerRollbackSpec extends ApiTestSpec {

    @SpringBean
    RequestStateChangeRepository requestStateChangeRepository = Mock()

    def 'creating request rolls back when saving its history fails'() {
        given:
            def request = newRequestRandomValues()

        when:
            def result = createNewRequest(request)

        then:
            1 * requestStateChangeRepository.save(_ as RequestStateChange) >> {
                throw new IllegalStateException('Simulated audit persistence failure')
            }

        and:
            assertResultIs(result,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Internal Server Error",
                    "/requests"
            )

        and:
            def foundRequests = is200WithRequestsListResponse(listRequests([name: request.name]))
            foundRequests.content.empty
    }
}
