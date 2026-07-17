# RM batch formula export compatibility service

This internal-only Java 8 service restores PostgreSQL-backed list queries and
XLSX export for `RM_1.0.0_formula_batchFormulaList`.

Nginx authenticates the caller through the ADP gateway before adding the
internal `X-ADP-Auth-Checked: 1` proof header. The service has no published host
port and does not accept a browser-supplied token as proof by itself.

Runtime settings:

- `RM_EXPORT_PORT` defaults to `18090`.
- `RM_EXPORT_MAX_ROWS` defaults to `5000` and is capped at `100000`.
- `RM_EXPORT_MAX_REQUEST_BYTES` defaults to `1048576`.
- `RM_EXPORT_THREADS` defaults to `4`.
- `RM_EXPORT_QUERY_TIMEOUT_SECONDS` defaults to `30`.

The output is a genuine OOXML workbook generated from `public.rm_formulas`.
Product codes and names are resolved from `public.baseset_materials`; numeric
database identifiers are serialized as JSON strings so old browser runtimes do
not lose precision. Requests above `RM_EXPORT_MAX_ROWS` fail with HTTP `422`
instead of returning a silently truncated workbook. The route is read-only and
does not mutate business data.
