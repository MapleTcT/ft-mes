CREATE OR REPLACE FUNCTION public.notice_protocol_before_insert_idempotent()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM public.notice_protocol
    WHERE protocol = NEW.protocol
  ) THEN
    UPDATE public.notice_protocol
    SET name = NEW.name,
        app_name = NEW.app_name,
        vender_name = NEW.vender_name,
        service_name = NEW.service_name,
        send_url = NEW.send_url,
        config_url = NEW.config_url,
        system_config_app_code = NEW.system_config_app_code,
        system_config_code = NEW.system_config_code,
        default_template_code = NEW.default_template_code,
        content_type = NEW.content_type,
        doc = NEW.doc,
        i18n_module = NEW.i18n_module,
        i18n_key = NEW.i18n_key,
        system = NEW.system,
        valid = COALESCE(NEW.valid, valid),
        modifier = COALESCE(NEW.modifier, modifier),
        modify_time = CURRENT_TIMESTAMP,
        modify_staff_id = COALESCE(NEW.modify_staff_id, modify_staff_id)
    WHERE protocol = NEW.protocol;
    RETURN NULL;
  END IF;

  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_notice_protocol_before_insert_idempotent ON public.notice_protocol;
CREATE TRIGGER trg_notice_protocol_before_insert_idempotent
BEFORE INSERT ON public.notice_protocol
FOR EACH ROW
EXECUTE FUNCTION public.notice_protocol_before_insert_idempotent();

CREATE OR REPLACE FUNCTION public.notice_protocol_tmpl_before_insert_idempotent()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM public.notice_protocol_tmpl
    WHERE code = NEW.code
  ) THEN
    UPDATE public.notice_protocol_tmpl
    SET name = NEW.name,
        i18n_key = NEW.i18n_key,
        description = NEW.description,
        template = NEW.template,
        system = NEW.system,
        modifier = COALESCE(NEW.modifier, modifier),
        modify_time = CURRENT_TIMESTAMP,
        modify_staff_id = COALESCE(NEW.modify_staff_id, modify_staff_id)
    WHERE code = NEW.code;
    RETURN NULL;
  END IF;

  IF EXISTS (
    SELECT 1
    FROM public.notice_protocol_tmpl
    WHERE notice_protocol_id = NEW.notice_protocol_id
      AND name = NEW.name
  ) THEN
    UPDATE public.notice_protocol_tmpl
    SET code = NEW.code,
        i18n_key = NEW.i18n_key,
        description = NEW.description,
        template = NEW.template,
        system = NEW.system,
        modifier = COALESCE(NEW.modifier, modifier),
        modify_time = CURRENT_TIMESTAMP,
        modify_staff_id = COALESCE(NEW.modify_staff_id, modify_staff_id)
    WHERE notice_protocol_id = NEW.notice_protocol_id
      AND name = NEW.name;
    RETURN NULL;
  END IF;

  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_notice_protocol_tmpl_before_insert_idempotent ON public.notice_protocol_tmpl;
CREATE TRIGGER trg_notice_protocol_tmpl_before_insert_idempotent
BEFORE INSERT ON public.notice_protocol_tmpl
FOR EACH ROW
EXECUTE FUNCTION public.notice_protocol_tmpl_before_insert_idempotent();

CREATE OR REPLACE FUNCTION public.notice_tmpl_before_insert_idempotent()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM public.notice_tmpl
    WHERE code = NEW.code
  ) THEN
    UPDATE public.notice_tmpl
    SET name = NEW.name,
        params = COALESCE(NEW.params, params),
        description = COALESCE(NEW.description, description),
        template = COALESCE(NEW.template, template),
        source = COALESCE(NEW.source, source),
        modify_sign = COALESCE(NEW.modify_sign, modify_sign),
        cover_sign = COALESCE(NEW.cover_sign, cover_sign),
        notice_protocol_id = COALESCE(NEW.notice_protocol_id, notice_protocol_id),
        version = COALESCE(NEW.version, version),
        sort_value = COALESCE(NEW.sort_value, sort_value),
        valid = COALESCE(NEW.valid, valid),
        modifier = COALESCE(NEW.modifier, modifier),
        modify_time = CURRENT_TIMESTAMP,
        modify_staff_id = COALESCE(NEW.modify_staff_id, modify_staff_id)
    WHERE code = NEW.code;
    RETURN NULL;
  END IF;

  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_notice_tmpl_before_insert_idempotent ON public.notice_tmpl;
CREATE TRIGGER trg_notice_tmpl_before_insert_idempotent
BEFORE INSERT ON public.notice_tmpl
FOR EACH ROW
EXECUTE FUNCTION public.notice_tmpl_before_insert_idempotent();

CREATE OR REPLACE FUNCTION public.notice_topic_before_insert_idempotent()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM public.notice_topic
    WHERE code = NEW.code
  ) THEN
    UPDATE public.notice_topic
    SET name = NEW.name,
        source = COALESCE(NEW.source, source),
        modify_sign = COALESCE(NEW.modify_sign, modify_sign),
        cover_sign = COALESCE(NEW.cover_sign, cover_sign),
        notice_topic_type_id = COALESCE(NEW.notice_topic_type_id, notice_topic_type_id),
        description = COALESCE(NEW.description, description),
        version = COALESCE(NEW.version, version),
        valid = COALESCE(NEW.valid, valid),
        sort_value = COALESCE(NEW.sort_value, sort_value),
        modifier = COALESCE(NEW.modifier, modifier),
        modify_time = CURRENT_TIMESTAMP,
        modify_staff_id = COALESCE(NEW.modify_staff_id, modify_staff_id)
    WHERE code = NEW.code;
    RETURN NULL;
  END IF;

  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_notice_topic_before_insert_idempotent ON public.notice_topic;
CREATE TRIGGER trg_notice_topic_before_insert_idempotent
BEFORE INSERT ON public.notice_topic
FOR EACH ROW
EXECUTE FUNCTION public.notice_topic_before_insert_idempotent();

CREATE OR REPLACE FUNCTION public.notice_topic_tmpl_rel_before_insert_idempotent()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM public.notice_topic_tmpl_rel
    WHERE notice_topic_id = NEW.notice_topic_id
      AND notice_tmpl_id = NEW.notice_tmpl_id
      AND notice_protocol_id = NEW.notice_protocol_id
  ) THEN
    UPDATE public.notice_topic_tmpl_rel
    SET modifier = COALESCE(NEW.modifier, modifier),
        modify_time = CURRENT_TIMESTAMP,
        modify_staff_id = COALESCE(NEW.modify_staff_id, modify_staff_id)
    WHERE notice_topic_id = NEW.notice_topic_id
      AND notice_tmpl_id = NEW.notice_tmpl_id
      AND notice_protocol_id = NEW.notice_protocol_id;
    RETURN NULL;
  END IF;

  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_notice_topic_tmpl_rel_before_insert_idempotent ON public.notice_topic_tmpl_rel;
CREATE TRIGGER trg_notice_topic_tmpl_rel_before_insert_idempotent
BEFORE INSERT ON public.notice_topic_tmpl_rel
FOR EACH ROW
EXECUTE FUNCTION public.notice_topic_tmpl_rel_before_insert_idempotent();

DO $$
DECLARE
  month_cursor date := DATE '2021-01-01';
  end_month date := (date_trunc('month', CURRENT_DATE) + INTERVAL '1 month')::date;
  month_value text;
  protocol_value text;
  task_table text;
  task_protocol_table text;
  msg_table text;
BEGIN
  WHILE month_cursor <= end_month LOOP
    month_value := to_char(month_cursor, 'YYYYMM');
    task_table := 'notice_task_' || month_value;
    task_protocol_table := 'notice_task_protocol_' || month_value;

    EXECUTE format(
      'CREATE TABLE IF NOT EXISTS public.%I (LIKE public.notice_task INCLUDING DEFAULTS INCLUDING CONSTRAINTS)',
      task_table
    );
    EXECUTE format(
      'CREATE TABLE IF NOT EXISTS public.%I (LIKE public.notice_task_protocol INCLUDING DEFAULTS INCLUDING CONSTRAINTS)',
      task_protocol_table
    );
    EXECUTE format(
      'CREATE UNIQUE INDEX IF NOT EXISTS %I ON public.%I (notice_task_id, notice_protocol_id)',
      'udx_ntp_' || month_value,
      task_protocol_table
    );

    FOR protocol_value IN
      SELECT protocol
      FROM public.notice_protocol
      WHERE valid = 1
    LOOP
      msg_table := 'notice_msg_' || protocol_value || month_value;

      EXECUTE format(
        'CREATE TABLE IF NOT EXISTS public.%I (LIKE public.notice_msg INCLUDING DEFAULTS INCLUDING CONSTRAINTS)',
        msg_table
      );
      EXECUTE format(
        'CREATE INDEX IF NOT EXISTS %I ON public.%I (staff_code, notice_protocol_id, read_status)',
        left('idx_' || lower(msg_table) || '_staff_protocol', 63),
        msg_table
      );
      EXECUTE format(
        'CREATE INDEX IF NOT EXISTS %I ON public.%I (staff_code, topic_id)',
        left('idx_' || lower(msg_table) || '_staff_topic', 63),
        msg_table
      );
      EXECUTE format(
        'CREATE INDEX IF NOT EXISTS %I ON public.%I (notice_task_id)',
        left('idx_' || lower(msg_table) || '_task', 63),
        msg_table
      );
    END LOOP;

    month_cursor := (month_cursor + INTERVAL '1 month')::date;
  END LOOP;
END $$;
