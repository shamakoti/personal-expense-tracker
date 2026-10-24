# API Documentation

Base URL: `http://localhost:8080`

All endpoints except `/api/auth/csrf`, `/api/auth/register` and `/api/auth/login` require a logged-in session cookie.
Requests that change data (`POST`, `PUT`, `DELETE`) must also send the CSRF token in the `X-CSRF-TOKEN` header; get it from `GET /api/auth/csrf`.

## Authentication

| Method | Endpoint | Description | Success |
|---|---|---|---|
| GET | `/api/auth/csrf` | Returns `{ "token": "...", "headerName": "X-CSRF-TOKEN" }` | 200 |
| POST | `/api/auth/register` | Create an account and log in | 201 |
| POST | `/api/auth/login` | Log in | 200 |
| POST | `/api/auth/logout` | Log out | 200 |
| GET | `/api/auth/me` | The logged-in user | 200 |

Register request:
```json
{ "displayName": "Asha", "email": "asha@example.com", "password": "secret123" }
```

## Transactions

| Method | Endpoint | Description | Success |
|---|---|---|---|
| GET | `/api/transactions` | Search, filter, sort and paginate | 200 |
| GET | `/api/transactions/{id}` | One transaction | 200 |
| POST | `/api/transactions` | Create | 201 |
| PUT | `/api/transactions/{id}` | Update | 200 |
| DELETE | `/api/transactions/{id}` | Delete | 204 |

Query parameters for `GET /api/transactions` (all optional):

| Parameter | Example | Meaning |
|---|---|---|
| `search` | `lunch` | Description contains the text (case-insensitive) |
| `type` | `EXPENSE` | `INCOME` or `EXPENSE` |
| `categoryId` | `3` | A category id |
| `month` | `2026-10` | A whole month (overrides `from` and `to`) |
| `from`, `to` | `2026-10-01` | Date range, format `yyyy-MM-dd` |
| `sort` | `amount-desc` | `date-desc` (default), `date-asc`, `amount-desc`, `amount-asc` |
| `page`, `size` | `0`, `10` | Page number (from 0) and page size (maximum 100) |

Create / update request:
```json
{
  "amount": 250.50,
  "type": "EXPENSE",
  "categoryId": 1,
  "transactionDate": "2026-10-15",
  "description": "Lunch"
}
```

List response:
```json
{ "content": [ { "id": 40, "amount": 250.50, "type": "EXPENSE", "categoryId": 1,
                 "categoryName": "Food", "transactionDate": "2026-10-15",
                 "description": "Lunch", "createdAt": "...", "updatedAt": "..." } ],
  "page": 0, "size": 10, "totalElements": 18, "totalPages": 2 }
```

## Categories

| Method | Endpoint | Description | Success |
|---|---|---|---|
| GET | `/api/categories?type=EXPENSE` | Default categories plus your own (`type` optional) | 200 |
| POST | `/api/categories` | Create a custom category: `{ "name": "Gym", "type": "EXPENSE" }` | 201 |

## Reports

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/reports/summary?month=2026-10` | `{ totalIncome, totalExpenses, savings }`; omit `month` for all time |
| GET | `/api/reports/monthly` | One entry per month: `{ month, income, expenses, savings }` |
| GET | `/api/reports/categories?month=2026-10&type=EXPENSE` | Totals per category: `{ categoryId, categoryName, total, percentage }` |

Savings = income − expenses.

## Errors

Every error has the same shape:

```json
{
  "timestamp": "2026-10-24T15:30:24",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "fieldErrors": { "amount": "Amount must be greater than zero" }
}
```

| Status | Meaning |
|---|---|
| 400 | Invalid input (validation error, bad date or month, wrong category type, unknown sort) |
| 401 | Not logged in, or wrong email or password |
| 403 | Missing or invalid CSRF token |
| 404 | Not found (also returned for another user's data) |
| 409 | Conflict (duplicate email or category) |
| 500 | Unexpected server error (details are logged on the server only) |