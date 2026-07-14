-- Keep line-scoped production-context revisions monotonic across adapter rebuilds
-- and deterministic acceptance publishers that may share a Kafka topic.

CREATE OR REPLACE FUNCTION public.wom_bpi_apply_context_revision_clock_floor()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    v_clock_floor BIGINT;
BEGIN
    v_clock_floor := floor(extract(epoch FROM clock_timestamp()) * 1000)::BIGINT;
    NEW.last_revision := greatest(NEW.last_revision, v_clock_floor);
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_wom_bpi_context_revision_clock_floor
    ON public.wom_bpi_context_revisions;
CREATE TRIGGER trg_wom_bpi_context_revision_clock_floor
BEFORE INSERT OR UPDATE OF last_revision
ON public.wom_bpi_context_revisions
FOR EACH ROW
EXECUTE FUNCTION public.wom_bpi_apply_context_revision_clock_floor();

UPDATE public.wom_bpi_context_revisions
   SET last_revision = greatest(
           last_revision,
           floor(extract(epoch FROM clock_timestamp()) * 1000)::BIGINT
       ),
       updated_at = CURRENT_TIMESTAMP;

COMMENT ON FUNCTION public.wom_bpi_apply_context_revision_clock_floor() IS
    'Prevents low revision reuse after adapter rebuilds while preserving per-line increments within the same millisecond.';
