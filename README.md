# BusinessSystem — Task Management Backend

A Spring Boot 4 / Java 21 backend that handles a supervisor-employee task workflow. It covers the full lifecycle: creating and assigning tasks, tracking progress through status transitions, uploading supporting files, and sending in-app notifications along the way. There's also a complete audit trail so you can see exactly what happened to a task and when.

---

## Table of Contents

- [Overview](#overview)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [Authentication](#authentication)
- [API Reference](#api-reference)
- [Task Workflow](#task-workflow)
- [End-to-End Walkthrough](#end-to-end-walkthrough)
- [Error Handling](#error-handling)

---

## Overview

The system revolves around two roles:

- **Supervisor** — creates employees, creates and assigns tasks, uploads files, and confirms completed work.
- **Employee** — receives task assignments, starts and resolves tasks, views and downloads files, and gets notified at each stage.

Everything is protected by HTTP Basic Auth with BCrypt-hashed passwords. Role-based access is enforced at the method level, and invalid workflow transitions (e.g. trying to start a task that hasn't been assigned yet) are rejected with a clear error message rather than silently ignored.

---

## Getting Started

**Prerequisites:** Java 21, Maven, PostgreSQL

```bash
# Clone and run
./mvnw spring-boot:run
```

The app starts on `http://localhost:8085`.

Uploaded files land on disk under `./uploads/task-<id>/` — the directory is created automatically when the first file is uploaded for a given task.

On first boot, a default supervisor account is created if none exists in the database (see [Configuration](#configuration) to set your own credentials before launching).

---

## Configuration

Open `src/main/resources/application.properties` and fill in your details:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/employee_system
spring.datasource.username=YOUR_USER
spring.datasource.password=YOUR_PASSWORD

app.upload-dir=./uploads

app.bootstrap.supervisor-email=admin@business.local
app.bootstrap.supervisor-password=admin123
```

> **Heads up:** Change the bootstrap email and password *before* the first run if you don't want the defaults. Once the supervisor record is seeded, modifying these properties won't overwrite it.

---

## Authentication

Every endpoint (except `OPTIONS /**`) requires HTTP Basic Auth — just pass your email and password with each request.

**In Postman:** go to the Authorization tab, pick "Basic Auth", and enter your credentials.

The default supervisor account seeded on first run:

| Email | Password |
|---|---|
| `admin@business.local` | `admin123` |

New employee accounts are created by a supervisor via `POST /api/users` — there's no open self-registration.

---

## API Reference

Base URL: `http://localhost:8085`

### Auth

| Method | Path | Description |
|---|---|---|
| POST | `/api/auth/login` | Verify credentials, returns current user |
| GET | `/api/auth/me` | Returns the profile of whoever is authenticated |

---

### Users
> Supervisor only

| Method | Path | Description |
|---|---|---|
| POST | `/api/users` | Create a new employee |
| GET | `/api/users` | List all users |
| GET | `/api/users?role=EMPLOYEE` | Filter by role |
| GET | `/api/users/{id}` | Get a specific user |
| GET | `/api/users/count` | Total user count |
| DELETE | `/api/users/{id}` | Delete a user |

**Create user body:**
```json
{
  "firstName": "Jane",
  "lastName": "Doe",
  "email": "jane@business.local",
  "password": "pass123",
  "role": "EMPLOYEE"
}
```
`role` defaults to `EMPLOYEE` if omitted.

---

### Tasks

| Method | Path | Who can call it |
|---|---|---|
| POST | `/api/tasks` | Supervisor |
| GET | `/api/tasks` | Any (results scoped by role) |
| GET | `/api/tasks/{id}` | Any with access |
| PUT | `/api/tasks/{id}/assign?employeeId=2` | Supervisor |
| PUT | `/api/tasks/{id}/start` | Assigned employee |
| PUT | `/api/tasks/{id}/resolve` | Assigned employee |
| PUT | `/api/tasks/{id}/confirm` | Creating supervisor |
| GET | `/api/tasks/{id}/history` | Any with access |
| DELETE | `/api/tasks/{id}` | Creating supervisor |

**Create task body:**
```json
{
  "title": "Fix the printer",
  "description": "Out of toner in Bay 3",
  "priority": "HIGH",
  "dueDate": "2026-05-20",
  "assignedTo": 2
}
```

- `title` is required; everything else is optional.
- `priority`: `LOW`, `MEDIUM`, or `HIGH` — defaults to `MEDIUM`.
- `dueDate`: ISO format `YYYY-MM-DD`.
- `assignedTo`: if you include an employee ID here, the task skips straight to `ASSIGNED` and the employee gets a notification.

---

### Files

| Method | Path | Who can call it |
|---|---|---|
| POST | `/api/tasks/{taskId}/files` | Supervisor or assigned employee |
| GET | `/api/tasks/{taskId}/files` | Any with task access |
| GET | `/api/files/{fileId}` | Any with task access |
| GET | `/api/files/{fileId}/download` | Any with task access |
| DELETE | `/api/files/{fileId}` | Uploader or any supervisor |

**Uploading in Postman:**
1. Set method to `POST`, URL to `http://localhost:8085/api/tasks/{taskId}/files`
2. Go to Body → form-data
3. Add a key named `file`, change the type dropdown from "Text" to **"File"**, then pick your file

**Downloading in Postman:** hit `GET /api/files/{id}/download` and use **"Send and Download"** to save the file locally.

Max file size: **10 MB**.

---

### Notifications

| Method | Path | Description |
|---|---|---|
| GET | `/api/notifications` | List all notifications for the current user |
| GET | `/api/notifications/unread-count` | Returns `{ "count": N }` |
| PUT | `/api/notifications/{id}/read` | Mark a notification as read |

Notifications are sent automatically at three points in the workflow:

- **Task assigned** → assignee is notified
- **Task resolved** → creating supervisor is notified
- **Task confirmed done** → assignee is notified

---

## Task Workflow

Tasks move through states in a fixed order. Skipping steps or going backward isn't allowed.

```
CREATED ──assign──▶ ASSIGNED ──start──▶ IN_PROGRESS ──resolve──▶ RESOLVED ──confirm──▶ DONE
```

| Transition | Who triggers it |
|---|---|
| `assign` | Supervisor |
| `start` | Assigned employee |
| `resolve` | Assigned employee |
| `confirm` | Creating supervisor |

Attempting an invalid transition (e.g. calling `/start` on a `CREATED` task) returns HTTP `409` with a message explaining what state the task was actually in.

Every state change is recorded in the task's history log — accessible via `GET /api/tasks/{id}/history` — with the actor's ID and a timestamp.

---

## End-to-End Walkthrough

Here's a complete run-through of the happy path, useful for manual testing in Postman.

### 1. Create an employee
*Auth: admin@business.local / admin123*

```
POST /api/users
```
```json
{
  "firstName": "Jane",
  "lastName": "Doe",
  "email": "jane@business.local",
  "password": "pass123"
}
```
Note the `id` in the response — you'll need it for task assignment.

### 2. Verify Jane can log in
*Auth: jane@business.local / pass123*

```
GET /api/auth/me
```
Should return her profile with `role: "EMPLOYEE"`.

### 3. Create and assign a task
*Auth: admin*

```
POST /api/tasks
```
```json
{
  "title": "Replace toner in Bay 3 printer",
  "description": "Magenta is empty",
  "priority": "HIGH",
  "dueDate": "2026-05-20",
  "assignedTo": <JANE_ID>
}
```
The task comes back with `status: "ASSIGNED"`. Save the task `id`.

### 4. Jane checks her work queue
*Auth: Jane*

- `GET /api/tasks` — the assigned task should appear.
- `GET /api/notifications` — one unread notification about the assignment.
- `GET /api/notifications/unread-count` — returns `{ "count": 1 }`.

### 5. Upload a supporting file
*Auth: admin*

```
POST /api/tasks/{TASK_ID}/files
```
Body: form-data, key `file`, attach a file from your machine.

### 6. Jane downloads the file
*Auth: Jane*

- `GET /api/tasks/{TASK_ID}/files` — get the file `id`.
- `GET /api/files/{FILE_ID}/download` — use "Send and Download" in Postman.

### 7. Jane works through the task
*Auth: Jane*

```
PUT /api/tasks/{TASK_ID}/start    → status: IN_PROGRESS
PUT /api/tasks/{TASK_ID}/resolve  → status: RESOLVED
```

Try calling `/confirm` as Jane — you should get a `403`.

### 8. Supervisor signs off
*Auth: admin*

- `GET /api/notifications` — there's a notification that Jane submitted the task for review.
- `PUT /api/tasks/{TASK_ID}/confirm` → `status: "DONE"`.

Back as Jane, `GET /api/notifications` now shows a "Task confirmed complete" notification.

### 9. Review the audit trail

```
GET /api/tasks/{TASK_ID}/history
```

Five entries: CREATED → ASSIGNED → STARTED → RESOLVED → CONFIRMED, each showing who did it and when.

### 10. Test the guardrails

A few things worth verifying while you're in there:

- As admin, try `/start` on the completed task — expect `409` (can't modify a DONE task) or `403` (you're not the assignee).
- Create a task without assigning anyone, then immediately call `/start` — expect `409`.
- As Jane, try `POST /api/users` — expect `403`.

---

## Error Handling

All errors come back as JSON, so there's no parsing ambiguity:

```json
{
  "timestamp": "2026-05-14T18:32:11.142Z",
  "status": 409,
  "error": "Conflict",
  "message": "Task must be ASSIGNED to start, was IN_PROGRESS"
}
```

| Status | Meaning |
|---|---|
| `400` | Bad input — missing required field, empty file, etc. |
| `401` | Missing or incorrect Basic Auth credentials |
| `403` | Wrong role, or you're not the creator/assignee for this action |
| `404` | Task, user, file, or notification not found |
| `409` | Workflow violation — the task isn't in the right state for that transition |
| `413` | File exceeds the 10 MB limit |
