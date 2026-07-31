-- Restore the two protected qualification roots shipped by the original module.
-- Source: Qualify_6.1.2.1/META-INF/init/custom/*/init.sql
-- These are product master-data roots required before users can create child
-- certificate categories. They are not acceptance-test business records.

INSERT INTO public.qlf_certificate_types (
    id,
    version,
    valid,
    cid,
    full_path_name,
    parent_id,
    lay_no,
    lay_rec,
    name,
    code
)
VALUES
    (1000, 0, true, 1000, '1000', -1, 1, '1000', '人员资质', 'staffCert'),
    (1001, 0, true, 1000, '1001', -1, 1, '1001', '企业资质', 'companyCert')
ON CONFLICT (id) DO UPDATE SET
    valid = true,
    cid = EXCLUDED.cid,
    full_path_name = EXCLUDED.full_path_name,
    parent_id = EXCLUDED.parent_id,
    lay_no = EXCLUDED.lay_no,
    lay_rec = EXCLUDED.lay_rec,
    name = EXCLUDED.name,
    code = EXCLUDED.code;

DO $$
DECLARE
    root_count integer;
BEGIN
    SELECT count(*)
      INTO root_count
      FROM public.qlf_certificate_types
     WHERE id IN (1000, 1001)
       AND valid = true
       AND cid = 1000
       AND parent_id = -1
       AND (
           (id = 1000 AND code = 'staffCert' AND name = '人员资质')
           OR
           (id = 1001 AND code = 'companyCert' AND name = '企业资质')
       );

    IF root_count <> 2 THEN
        RAISE EXCEPTION
            'Qualify certificate root bootstrap failed: expected 2 valid roots, got %',
            root_count;
    END IF;
END $$;
