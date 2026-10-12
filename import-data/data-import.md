# CATS data import (`cats-import-v1`)

The **Admin → Import data** screen accepts a UTF-8 JSON file of at most 500 KB.
Import is available to MFA-verified administrators. The service never executes
arbitrary uploaded SQL. A file may contain any nonempty combination of `users`,
`providers`, `courses`, `holidays`, and `applications` (missing collections
mean empty).

## Schema

```json
{
  "format": "cats-import-v1",
  "source": "optional provenance description",
  "users": [{
    "name": "Example", "username": "staff1", "email": "staff@example.org",
    "role": "ROLE_EMPLOYEE", "passwordHash": "$2a$10$...60-character-BCrypt-hash...",
    "active": true, "admin": false, "designation": "PROFESSIONAL",
    "managerUsername": null,
    "budgetedAllowance": 2000, "actualAllowance": 2000,
    "budgetedDays": 10, "actualDays": 10
  }],
  "providers": [{"name": "Example Provider", "enabled": true}],
  "courses": [{
    "name": "Example Course", "type": "INTERNAL", "duration": "FULLDAY",
    "startDate": "2026-11-01", "endDate": "2026-11-02",
    "location": "Example", "provider": "Example Provider", "fee": 0,
    "archived": false
  }],
  "holidays": [{"date": "2026-12-25", "name": "Holiday"}],
  "applications": [{
    "username": "staff1", "courseName": "Example Course", "status": "APPLIED",
    "userReason": "Training", "managerReason": null, "workDissemination": null
  }]
}
```

All collections are optional but at least one must contain data. Dates use
ISO `YYYY-MM-DD`. Roles: `ROLE_EMPLOYEE`, `ROLE_MANAGER`, `ROLE_ADMIN`.
Course types: `INTERNAL`, `EXTERNAL`, `PROFESSIONAL`. Durations:
`FULLDAY`, `HALFDAYAM`, `HALFDAYPM`. Application statuses follow the
`Status` enum in the source repository. `passwordHash` must contain a valid
**60-character BCrypt hash**, not a plaintext password.

`active` and `admin` are optional: omitted `active` remains NULL (which the
current application treats as enabled); omitted `admin` follows the imported
role. Importing preserves the provided email addresses, password hashes,
account states, roles, providers, and balances without anonymizing or
silently disabling accounts.

Users can refer to managers included in the file or already in the database.
Applications can refer to users and courses in the file or already in the
database. Courses may create an enabled provider when none exists. Provider
rows can be imported without courses. All imported records are **insert-only**:
collisions in usernames, course names, provider names or holiday dates reject
the whole import. Database transactions roll back on failure.
