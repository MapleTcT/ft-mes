# Business Smoke Signoff Template

## Status

Status: `BLOCKED`

This template becomes a signoff artifact only after real frontend smoke and
PostgreSQL persistence evidence are attached.

## Environment

| Field | Value |
| --- | --- |
| Date | TBD |
| Environment | TBD |
| Frontend URL | TBD |
| Backend build / commit | TBD |
| Database | PostgreSQL |
| Tester | TBD |
| Business owner | TBD |

## Required Smoke

| Area | Required Evidence | Result |
| --- | --- | --- |
| Login / authentication | Browser smoke and API record | BLOCKED |
| User / organization / permissions | CRUD and PostgreSQL evidence | BLOCKED |
| Menu / todo / dashboard | Browser smoke with console/network record | BLOCKED |
| Basic configuration | Browser smoke and persistence proof where applicable | BLOCKED |
| Nacos / Keycloak / runtime patch | Config evidence and health checks | BLOCKED |
| Production main flow | Full WOM flow and persistence proof | BLOCKED |
| Import / export / upload if used | Browser and file evidence | BLOCKED |

## Signoff

| Role | Name | Decision | Date | Notes |
| --- | --- | --- | --- | --- |
| Business owner | TBD | BLOCKED | TBD | WOM action persistence is not complete |
| Technical owner | TBD | BLOCKED | TBD | Pending production migration rehearsal |
| Release owner | TBD | BLOCKED | TBD | Rollback rehearsal required |
