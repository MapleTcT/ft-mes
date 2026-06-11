CREATE TABLE IF NOT EXISTS public.iam_account (
    id BIGINT PRIMARY KEY,
    access_key VARCHAR(32) NOT NULL,
    secret_key VARCHAR(32) NOT NULL,
    username VARCHAR(64) NOT NULL,
    description VARCHAR(256),
    system SMALLINT DEFAULT 1,
    download_mark SMALLINT DEFAULT 0,
    creator VARCHAR(32) NOT NULL DEFAULT 'system',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_staff_id BIGINT,
    modifier VARCHAR(32),
    modify_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modify_staff_id BIGINT
);

ALTER TABLE public.iam_account ADD COLUMN IF NOT EXISTS access_key VARCHAR(32);
ALTER TABLE public.iam_account ADD COLUMN IF NOT EXISTS secret_key VARCHAR(32);
ALTER TABLE public.iam_account ADD COLUMN IF NOT EXISTS username VARCHAR(64);
ALTER TABLE public.iam_account ADD COLUMN IF NOT EXISTS description VARCHAR(256);
ALTER TABLE public.iam_account ADD COLUMN IF NOT EXISTS system SMALLINT DEFAULT 1;
ALTER TABLE public.iam_account ADD COLUMN IF NOT EXISTS download_mark SMALLINT DEFAULT 0;
ALTER TABLE public.iam_account ADD COLUMN IF NOT EXISTS creator VARCHAR(32) DEFAULT 'system';
ALTER TABLE public.iam_account ADD COLUMN IF NOT EXISTS create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE public.iam_account ADD COLUMN IF NOT EXISTS create_staff_id BIGINT;
ALTER TABLE public.iam_account ADD COLUMN IF NOT EXISTS modifier VARCHAR(32);
ALTER TABLE public.iam_account ADD COLUMN IF NOT EXISTS modify_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE public.iam_account ADD COLUMN IF NOT EXISTS modify_staff_id BIGINT;

CREATE UNIQUE INDEX IF NOT EXISTS udx_account_username ON public.iam_account(username);
CREATE UNIQUE INDEX IF NOT EXISTS udx_account_ak ON public.iam_account(access_key);

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
    100,
    '3ba8a60b48185a9e23d38e962b415603',
    '335e481694545a53e7385db62bab8bd9',
    'admin',
    'Built-in IAM admin account',
    1,
    0,
    'system',
    'system'
)
ON CONFLICT (id) DO NOTHING;
