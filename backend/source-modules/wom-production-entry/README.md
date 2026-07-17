# WOM Production Entry

`wom-production-entry` restores the visible manual manufacturing-instruction entry without
duplicating the recovered WOM workflow implementation.

The module:

- reads valid product/formula/line combinations from PostgreSQL;
- validates production-unit readiness before creation;
- provides request idempotency through `wom_manual_task_requests`;
- forwards the authenticated request to WOM `produceTaskCreated2`;
- resolves the created instruction and pending-work URL from PostgreSQL;
- serves the manual-entry page used by the WOM list toolbar.

The legacy WOM service remains the owner of instruction, batch, pending-task, and workflow
persistence. Oracle is not a runtime dependency of this module.
