-- Minimal TeamInfo reference data used by PostgreSQL smoke/persistence tests.
-- The recovered base package can render TeamInfo pages without carrying any
-- shift/team/schedule-rule records, which blocks creating a schedule plan from
-- the UI. These rows are intentionally marked with ADP_SMOKE_* codes.

DO $$
DECLARE
    base_id bigint;
    smoke_shift_rule_id bigint;
    smoke_shift_detail_id bigint;
    smoke_team_id bigint;
    smoke_schedule_rule_id bigint;
    smoke_sche_rule_detail_id bigint;
BEGIN
    SELECT greatest(
        COALESCE((SELECT max(id) FROM public.team_shift_rules), 0),
        COALESCE((SELECT max(id) FROM public.team_shift_details), 0),
        COALESCE((SELECT max(id) FROM public.team_teams), 0),
        COALESCE((SELECT max(id) FROM public.team_schedule_rules), 0),
        COALESCE((SELECT max(id) FROM public.team_sche_rule_details), 0),
        6590000000000000
    ) + 1000 INTO base_id;

    SELECT id INTO smoke_shift_rule_id
    FROM public.team_shift_rules
    WHERE code = 'ADP_SMOKE_TEAMINFO_SHIFT_RULE'
    ORDER BY id DESC
    LIMIT 1;

    IF smoke_shift_rule_id IS NULL THEN
        smoke_shift_rule_id := base_id + 1;
        INSERT INTO public.team_shift_rules (
            id, version, cid, valid, code, name, create_staff_id,
            modify_staff_id, create_time, modify_time, effective_state, status
        )
        VALUES (
            smoke_shift_rule_id, 0, 1000, true,
            'ADP_SMOKE_TEAMINFO_SHIFT_RULE',
            'ADP_SMOKE_TEAMINFO_SHIFT_RULE',
            1, 1, now(), now(), 99, 99
        );
    END IF;

    SELECT id INTO smoke_shift_detail_id
    FROM public.team_shift_details
    WHERE code = 'ADP_SMOKE_TEAMINFO_SHIFT_DETAIL'
    ORDER BY id DESC
    LIMIT 1;

    IF smoke_shift_detail_id IS NULL THEN
        smoke_shift_detail_id := base_id + 2;
        INSERT INTO public.team_shift_details (
            id, version, cid, valid, code, name, shift_rule, start_timen,
            end_timen, sort, create_staff_id, modify_staff_id,
            create_time, modify_time, effective_state, status
        )
        VALUES (
            smoke_shift_detail_id, 0, 1000, 1,
            'ADP_SMOKE_TEAMINFO_SHIFT_DETAIL',
            'ADP_SMOKE_TEAMINFO_SHIFT_DETAIL',
            smoke_shift_rule_id, '08:00', '16:00',
            1, 1, 1, now(), now(), 99, 99
        );
    END IF;

    SELECT id INTO smoke_team_id
    FROM public.team_teams
    WHERE code = 'ADP_SMOKE_TEAMINFO_TEAM'
    ORDER BY id DESC
    LIMIT 1;

    IF smoke_team_id IS NULL THEN
        smoke_team_id := base_id + 3;
        INSERT INTO public.team_teams (
            id, version, cid, valid, code, name, shift_rule, is_default,
            create_staff_id, modify_staff_id, create_time,
            modify_time, effective_state, status
        )
        VALUES (
            smoke_team_id, 0, 1000, true,
            'ADP_SMOKE_TEAMINFO_TEAM',
            'ADP_SMOKE_TEAMINFO_TEAM',
            smoke_shift_rule_id, false,
            1, 1, now(), now(), 99, 99
        );
    END IF;

    SELECT id INTO smoke_schedule_rule_id
    FROM public.team_schedule_rules
    WHERE code = 'ADP_SMOKE_TEAMINFO_SCHEDULE_RULE'
    ORDER BY id DESC
    LIMIT 1;

    IF smoke_schedule_rule_id IS NULL THEN
        smoke_schedule_rule_id := base_id + 4;
        INSERT INTO public.team_schedule_rules (
            id, version, cid, valid, code, name, team, start_shift_detail,
            end_shift_detail, start_date, end_date, create_staff_id,
            modify_staff_id, create_time, modify_time, effective_state, status
        )
        VALUES (
            smoke_schedule_rule_id, 0, 1000, true,
            'ADP_SMOKE_TEAMINFO_SCHEDULE_RULE',
            'ADP_SMOKE_TEAMINFO_SCHEDULE_RULE',
            smoke_team_id, smoke_shift_detail_id, smoke_shift_detail_id,
            current_date + 1, current_date + 30,
            1, 1, now(), now(), 99, 99
        );
    END IF;

    SELECT id INTO smoke_sche_rule_detail_id
    FROM public.team_sche_rule_details
    WHERE code = 'ADP_SMOKE_TEAMINFO_SCHE_RULE_DETAIL'
    ORDER BY id DESC
    LIMIT 1;

    IF smoke_sche_rule_detail_id IS NULL THEN
        smoke_sche_rule_detail_id := base_id + 5;
        INSERT INTO public.team_sche_rule_details (
            id, version, cid, valid, code, schedule_rule, shift_detail,
            sort, create_staff_id, modify_staff_id, create_time, modify_time
        )
        VALUES (
            smoke_sche_rule_detail_id, 0, 1000, 1,
            'ADP_SMOKE_TEAMINFO_SCHE_RULE_DETAIL',
            smoke_schedule_rule_id, smoke_shift_detail_id,
            1, 1, 1, now(), now()
        );
    END IF;

    -- PostgreSQL JDBC reads Hibernate CLOB mappings as large-object OIDs.
    -- Keep these @Lob String smoke fields null so entity hydration does not
    -- try to parse ordinary text as a long OID.
    UPDATE public.team_shift_rules
    SET memo_field = NULL
    WHERE code = 'ADP_SMOKE_TEAMINFO_SHIFT_RULE';

    UPDATE public.team_shift_details
    SET memo_field = NULL
    WHERE code = 'ADP_SMOKE_TEAMINFO_SHIFT_DETAIL';

    UPDATE public.team_teams
    SET memo_field = NULL
    WHERE code = 'ADP_SMOKE_TEAMINFO_TEAM';

    UPDATE public.team_schedule_rules
    SET memo_field = NULL
    WHERE code = 'ADP_SMOKE_TEAMINFO_SCHEDULE_RULE';
END $$;
