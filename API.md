## API

### Request

Returned by every endpoint that creates, reads or modifies a single request

```text
{
    id: uuid
    name: string
    content: string
    state: string
    publicationNumber: integer | null
    reason: string | null
    created: timestamp
    updated: timestamp
    version: integer
}
```

### Errors

Errors are returned as `ProblemDetail`. Validation errors (400) also list invalid fields:

```json
{
  "title": "Bad Request",
  "status": 400,
  "detail": "Invalid request",
  "instance": "/requests",
  "errors": [
    { "field": "name", "message": "Value is required" }
  ]
}
```

### POST `/requests` - create request

```json
{
  "name": "Example request",
  "content": "Example content"
}
```

- `name` - required, max 100 chars
- `content` - required, max 1000 chars

Responses: `201` created request, `400` invalid body

### GET `/requests/{id}` - get request

Responses: `200` request, `400` malformed id, `404` not found

### GET `/requests` - list requests

Query params, all optional:

- `name` - case-insensitive substring
- `state` - exact state, e.g. `VERIFIED`
- `page` - default `0`
- `size` - default `10`, values above `100` are capped at `100`

Responses: `200` page of requests, `400` invalid state

```text
{
    content: [
        {
            id: uuid
            name: string
            state: string
            publicationNumber: integer | null
            created: timestamp
            updated: timestamp
        }
    ]
    page: {
        size: integer
        number: integer
        totalElements: integer
        totalPages: integer
    }
}
```

### Modifying requests

Every modifying endpoint requires `version` - the current version of the request (from the last response or GET)

| Endpoint                      | Allowed in state   | Body                 |
| ----------------------------- | ------------------ | -------------------- |
| PATCH `/requests/{id}`        | CREATED, VERIFIED  | `version`, `content` |
| POST `/requests/{id}/verify`  | CREATED            | `version`            |
| POST `/requests/{id}/accept`  | VERIFIED           | `version`            |
| POST `/requests/{id}/publish` | ACCEPTED           | `version`            |
| POST `/requests/{id}/reject`  | VERIFIED, ACCEPTED | `version`, `reason`  |
| POST `/requests/{id}/delete`  | CREATED            | `version`, `reason`  |

- `version` - required, non-negative integer
- `content` - required, max 1000 chars
- `reason` - required, max 500 chars

Example (reject):

```json
{
  "reason": "Does not meet the requirements",
  "version": 1
}
```

Responses:

- `200` - modified request
- `400` - invalid body or malformed id
- `404` - request not found
- `409` - stale version, fetch the request again and retry
- `422` - action not allowed in the current state

### GET `/requests/{id}/history` - list state changes

Responses: `200` history, `400` malformed id, `404` not found

```text
{
    requestId: uuid
    stateChanges: [
        {
            stateFrom: string | null
            stateTo: string
            created: timestamp
        }
    ]
}
```

`stateFrom` is `null` for the entry recording request creation
