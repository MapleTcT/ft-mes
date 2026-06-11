-- Runtime compatibility for the base ADP package:
-- current-user menu construction expects this optional favorites table even
-- when the user has not collected any menus.
CREATE TABLE IF NOT EXISTS public.rbac_menu_collection (
  id BIGINT PRIMARY KEY,
  version INTEGER DEFAULT 0,
  delete_time TIMESTAMP,
  modify_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  terminator VARCHAR(32),
  modifier VARCHAR(32),
  creator VARCHAR(32),
  create_staff_id BIGINT,
  modify_staff_id BIGINT,
  menuinfo_id BIGINT,
  menuinfo_code VARCHAR(510),
  sort DOUBLE PRECISION DEFAULT 0,
  user_id BIGINT,
  cid BIGINT
);

CREATE INDEX IF NOT EXISTS idx_rbac_menu_collection_user_cid
  ON public.rbac_menu_collection (user_id, cid);

CREATE INDEX IF NOT EXISTS idx_rbac_menu_collection_menuinfo
  ON public.rbac_menu_collection (menuinfo_id);

CREATE UNIQUE INDEX IF NOT EXISTS idx_rbac_menu_collection_user_cid_menu
  ON public.rbac_menu_collection (user_id, cid, menuinfo_id)
  WHERE menuinfo_id IS NOT NULL;
