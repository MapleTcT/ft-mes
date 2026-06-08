export function filterMenu(menus, v) {
  if (!v || !v.toLowerCase().trim()) return null;
  v = v.toLowerCase().trim();

  const expandedKeys = [];

  const addExp = (key) => !expandedKeys.includes(key) && expandedKeys.push(key);

  function mapFilteredData(menu, parent) {
    let isMatch = false;
    const { title } = menu;
    if (title && title.toLowerCase().includes(v)) {
      isMatch = true;
      if (parent) {
        addExp(String(parent.key));
      }
    }

    const newMenu = { ...menu };
    if (newMenu.children && newMenu.children.length) {
      newMenu.children = newMenu.children
        .map((m) => mapFilteredData(m, newMenu))
        .filter((m) => m);
    }

    if (!parent || (parent && isMatch)) {
      return newMenu;
    } else {
      return null;
    }
  }

  return {
    treeData: menus.map((m) => mapFilteredData(m)).filter((m) => m),
    expandedKeys
  };
}

export const extractResData = (data) => data.data.data;

export const delay = (fn, timeout = 100) => {
  if (process.env.NODE_ENV === 'development') {
    setTimeout(fn, timeout);
  } else {
    fn();
  }
};
