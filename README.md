# Request Management

## Description

REST application for managing the lifecycle of requests with every state change recorded in the request's history.
Every state change is validated according to these rules:

- `CREATED` -> `VERIFIED` | `DELETED`
- `VERIFIED` -> `ACCEPTED` | `REJECTED`
- `ACCEPTED` -> `PUBLISHED` | `REJECTED`
- `PUBLISHED`, `REJECTED`, `DELETED` are terminal, no state change is possible

Published requests get a unique numeric identifier (publication number) that is provided by db sequence.
Concurrent modifications are safe, optimistic locking rejects stale updates with 409.

Stack: Java 25, Spring Boot 4.1, PostgreSQL 18, Liquibase, Spock + Testcontainers

## Design

- domain owns rules and contracts, infra implements them
- valid state transitions are described in `RequestState`, so changing them means modifying only this one file
- configuration is done by properties and env vars (page sizes, datasource), field limits are kept in a single class

## Running application & tests

Java 25, Maven and Docker are required. To start application using bundled PostgreSQL execute:

```bash
mvn spring-boot:run
```

To use existing PostgreSQL database, set these env vars:

```bash
export SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:5432/db_name'
export SPRING_DATASOURCE_USERNAME='user_name'
export SPRING_DATASOURCE_PASSWORD='password'
export SPRING_DOCKER_COMPOSE_ENABLED=false
```

Or configure them in application.yml:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/db_name
    username: user_name
    password: password
  docker:
    compose:
      enabled: false
```

Running tests with Docker running:

```bash
mvn clean test
```

For faster local test runs, enable container reuse in `~/.testcontainers.properties`:

```properties
testcontainers.reuse.enable=true
```

## Decisions

- field length limits are my assumptions and they match db columns max length
- GET endpoint was added so client can retrieve request's details without performing any modifying action
- delete is a soft delete, the row stays in the db, but state changes and provided delete reason are saved
- browsing requests
    - search by name is case insensitive and returns every request which name contains provided "name" param
    - special chars (%, _) are escaped so names containing them can be matched
    - only filters specified in requirements were added (name, state)
    - sorting order is fixed, requests are sorted by creation timestamp in descending order and id ascending (so order is stable)
    - default page size and max page size are configurable by properties
- request's name
    - can't be modified, only its content - nobody asked for modifiable name so I'm treating it as immutable label
    - doesn't have to be unique
- optimistic locking with a version column is used to prevent races when multiple HTTP requests try to modify single request at once
    - requests that mutate state are required to provide version number supplied by GET endpoint
- when client sends request with stale version and transition that is forbidden in current state then 409 is returned (not 422)
    - so the client knows to retrieve request details once more and check the current state
- publication numbers
    - when request is published backend assigns unique number, client can't provide its own id
    - these are ascending natural numbers starting at 1, but gaps in numbering are possible
- different 4xx status codes are used for invalid requests, so it's possible to determine what kind of bad request was sent
    - 400
      - invalid request body (e.g. wrong parameter name or required parameter is missing)
      - invalid parameter value (e.g. request's name is too long, malformed request id)
    - 409 - object that you want to modify has changed since you retrieved it
    - 422 - requested transition is forbidden or content edit is forbidden in current state
- audit log
    - stores also "record created", so we can later determine when request was created and answer question "how long it waited for verification?"
    - only request's state changes are stored so editing content is not recorded in audit log
    - field "reason" is not stored in audit log because once set it never changes, so it can always be retrieved from "request" table

## Possible future improvements

- requests browsing
    - add support for more filters
    - add support for sort parameter
- replace Spring objects in domain (Page, Pageable) with custom domain objects
- add tests for all rollback scenarios not only for creating request failure

## API

Check separate file [API.md](API.md)
