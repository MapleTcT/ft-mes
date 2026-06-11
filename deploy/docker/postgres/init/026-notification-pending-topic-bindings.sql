WITH pending_topic AS (
  SELECT id
  FROM public.notice_topic
  WHERE code = 'pending_topic'
  LIMIT 1
),
pending_templates AS (
  SELECT id, notice_protocol_id
  FROM public.notice_tmpl
  WHERE code IN ('dingtalk_pending', 'mobile_pending', 'wechat_pending')
),
missing_bindings AS (
  SELECT
    (SELECT id FROM pending_topic) AS topic_id,
    t.id AS tmpl_id,
    t.notice_protocol_id,
    row_number() OVER (ORDER BY t.notice_protocol_id, t.id) AS rn
  FROM pending_templates t
  WHERE EXISTS (SELECT 1 FROM pending_topic)
    AND NOT EXISTS (
      SELECT 1
      FROM public.notice_topic_tmpl_rel r
      WHERE r.notice_topic_id = (SELECT id FROM pending_topic)
        AND r.notice_protocol_id = t.notice_protocol_id
    )
),
id_base AS (
  SELECT coalesce(max(id), 0) AS max_id
  FROM public.notice_topic_tmpl_rel
)
INSERT INTO public.notice_topic_tmpl_rel
  (id, notice_topic_id, notice_tmpl_id, notice_protocol_id, creator, create_staff_id)
SELECT
  id_base.max_id + missing_bindings.rn,
  missing_bindings.topic_id,
  missing_bindings.tmpl_id,
  missing_bindings.notice_protocol_id,
  'compat',
  0
FROM missing_bindings
CROSS JOIN id_base
ON CONFLICT DO NOTHING;
