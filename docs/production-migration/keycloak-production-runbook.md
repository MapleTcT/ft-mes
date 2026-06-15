# Keycloak Production Runbook

## Status

Status: `PLANNED`

## Required Assets

| Asset | Evidence | Owner |
| --- | --- | --- |
| Realm export | TBD | Identity owner |
| Client list and redirect URI review | TBD | Identity owner |
| Client secret rotation plan | TBD | Identity owner |
| Admin account rotation plan | TBD | Security owner |
| PostgreSQL backup and restore rehearsal | TBD | DBA |
| JWT public key sync proof in Nacos | TBD | Runtime owner |

## Rehearsal Steps

1. Export the current realm from the source Keycloak environment.
2. Restore Keycloak database backup into the rehearsal target.
3. Import the realm and verify clients, redirect URIs and roles.
4. Rotate admin credentials and client secrets in rehearsal.
5. Run `sync-keycloak-jwt-public-key.sh` against the target realm.
6. Restart gateway/auth consumers and run login/current-user smoke.

## Acceptance

Keycloak production readiness is not `READY` until login, current user, menu,
RBAC authority and organization smoke pass after the production-style import.
