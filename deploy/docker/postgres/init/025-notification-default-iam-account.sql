INSERT INTO public.iam_account (
  id,
  access_key,
  secret_key,
  username,
  description,
  system,
  download_mark,
  creator,
  modifier
)
VALUES (
  101,
  'default',
  'default',
  'notification-default',
  'Docker test notification default AK/SK account',
  1,
  0,
  'system',
  'system'
)
ON CONFLICT (access_key) DO UPDATE
SET secret_key = EXCLUDED.secret_key,
    username = EXCLUDED.username,
    description = EXCLUDED.description,
    system = EXCLUDED.system,
    download_mark = EXCLUDED.download_mark,
    modifier = EXCLUDED.modifier,
    modify_time = CURRENT_TIMESTAMP;

UPDATE public.iam_account
SET secret_key = 'default',
    username = 'notification-default',
    description = 'Docker test notification default AK/SK account',
    modify_time = CURRENT_TIMESTAMP
WHERE access_key = 'default';
