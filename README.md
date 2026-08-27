# Forex Transaction Service

Spring Boot 3.2 microservice — records every forex conversion to MongoDB.

## Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.2 |
| Database | MongoDB 7 (Spring Data MongoDB) |
| Auth | JWT validation (shared secret with Auth Service) |
| Docs | SpringDoc OpenAPI / Swagger UI |
| Tests | Flapdoodle Embedded MongoDB (no real Mongo needed) |
| Java | 21 |

## Quick start

```bash
# 1. Start MongoDB (replica set — required for @Transactional)
docker-compose up mongodb mongo-init -d

# 2. Run the service
./mvnw spring-boot:run

# 3. Open Swagger UI
open http://localhost:8082/swagger-ui.html

# Optional: Mongo Express web UI
docker-compose up mongo-express -d
open http://localhost:8081
```

## Endpoints

All routes require `Authorization: Bearer <token>` from the Auth Service.

| Method | Path | Description |
|---|---|---|
| POST | `/api/v1/transactions/convert` | Record a conversion |
| GET | `/api/v1/transactions/{id}` | Get one transaction |
| GET | `/api/v1/transactions/history` | Paginated history (filterable) |
| GET | `/api/v1/transactions/stats` | User stats summary |
| GET | `/api/v1/transactions/admin/all` | All transactions (ADMIN only) |

## Example requests

### Record a conversion
```bash
curl -X POST http://localhost:8082/api/v1/transactions/convert \
  -H "Authorization: Bearer <your-jwt>" \
  -H "Content-Type: application/json" \
  -d '{
    "fromCurrency": "USD",
    "toCurrency": "ZAR",
    "amount": 1000.00,
    "exchangeRate": 18.62,
    "notes": "Client payment"
  }'
```

Response:
```json
{
  "id": "665f3a2b1c4e7d0012ab34cd",
  "userId": 1,
  "userEmail": "sipho@forex.local",
  "fromCurrency": "USD",
  "toCurrency": "ZAR",
  "sourceAmount": 1000.00,
  "convertedAmount": 18620.000000,
  "exchangeRate": 18.62,
  "status": "COMPLETED",
  "createdAt": "2024-06-04T10:22:31Z",
  "ipAddress": "127.0.0.1",
  "notes": "Client payment"
}
```

### Filtered history
```bash
# All USD→ZAR conversions, newest first
curl "http://localhost:8082/api/v1/transactions/history?from=USD&to=ZAR&page=0&size=10" \
  -H "Authorization: Bearer <your-jwt>"

# Date range
curl "http://localhost:8082/api/v1/transactions/history?after=2024-01-01T00:00:00Z" \
  -H "Authorization: Bearer <your-jwt>"
```

### Stats
```bash
curl http://localhost:8082/api/v1/transactions/stats \
  -H "Authorization: Bearer <your-jwt>"
```

Response:
```json
{
  "userId": 1,
  "userEmail": "sipho@forex.local",
  "totalTransactions": 47,
  "totalSourceVolume": 152300.000000,
  "topPair": "USD/ZAR",
  "lastTransactionAt": "2024-06-04T10:22:31Z"
}
```

## MongoDB document structure

```json
{
  "_id": "665f3a2b1c4e7d0012ab34cd",
  "userId": 1,
  "userEmail": "sipho@forex.local",
  "fromCurrency": "USD",
  "toCurrency": "ZAR",
  "sourceAmount": NumberDecimal("1000.000000"),
  "convertedAmount": NumberDecimal("18620.000000"),
  "exchangeRate": NumberDecimal("18.62000000"),
  "status": "COMPLETED",
  "createdAt": ISODate("2024-06-04T10:22:31Z"),
  "ipAddress": "127.0.0.1",
  "notes": "Client payment"
}
```

## Indexes

| Index | Fields | Purpose |
|---|---|---|
| `idx_user_date` | `userId ASC, createdAt DESC` | User history queries — most common |
| `idx_currency_pair` | `fromCurrency, toCurrency` | Pair filtering and reporting |
| `idx_status_date` | `status, createdAt DESC` | Admin status filtering |

## Why userId is denormalised

`userId` and `userEmail` are extracted from the JWT and stored directly in
each document. This means:

- No foreign key join across service databases
- No HTTP call to Auth Service on every query
- Each service deploys and scales independently
- Historical records remain accurate even if a user changes their email

This is the standard pattern for microservices — **share data by value, not by reference**.

## Run tests

```bash
./mvnw test
# Flapdoodle embedded MongoDB starts in-process — no Docker needed
```

## How JWT auth works in this service

1. Client sends `Authorization: Bearer <token>`
2. `JwtAuthenticationFilter` validates the token using the **shared secret**
3. `userId` and `userEmail` are extracted directly from JWT claims
4. `ForexUserPrincipal` is set in the `SecurityContext`
5. Controllers receive it via `@AuthenticationPrincipal ForexUserPrincipal principal`
6. Service uses `principal.getUserId()` to scope all queries

No database lookup needed — identity comes entirely from the token.

## Project structure

```
src/main/java/com/forex/transaction/
├── TransactionServiceApplication.java
├── config/
│   ├── SecurityConfig.java
│   ├── MongoConfig.java
│   ├── OpenApiConfig.java
│   └── GlobalExceptionHandler.java
├── controller/
│   └── TransactionController.java
├── document/
│   └── Transaction.java          ← @Document (replaces @Entity)
├── dto/
│   └── TransactionDtos.java
├── repository/
│   └── TransactionRepository.java  ← MongoRepository + @Aggregation
├── security/
│   ├── JwtService.java
│   ├── JwtAuthenticationFilter.java
│   └── ForexUserPrincipal.java   ← custom principal with userId + email
└── service/
    └── TransactionService.java   ← MongoTemplate for dynamic filtering
```
