import { request } from 'sup-rc-utility';

const XLS = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';
export const XLSX_TYPE = XLS;

export function flatRoleTagTree(roleTagTree) {
  const tagTree = [];
  const tagMap = {};
  roleTagTree.forEach((role) => {
    const { tag } = role;
    if (!tagMap[tag]) {
      tagMap[tag] = {
        id: Math.random().toString(16),
        title: tag,
        children: [],
        isTag: true
      };
      tagTree.push(tagMap[tag]);
    }
    tagMap[tag].children.push({
      ...role,
      title: role.name
    });
  });

  return tagTree;
}

export function downloadFile(
  data,
  filename,
  mime = 'application/octet-stream'
) {
  if (window.navigator.msSaveOrOpenBlob) {
    // Internet Explorer
    window.navigator.msSaveOrOpenBlob(
      new Blob([data], { type: mime }),
      filename
    );
  } else {
    const blob = new Blob([data], { type: mime });
    const blobURL = window.URL.createObjectURL(blob);
    const tempLink = document.createElement('a');
    tempLink.style.display = 'none';
    tempLink.href = blobURL;
    tempLink.setAttribute('download', filename);

    document.body.appendChild(tempLink);
    tempLink.click();

    setTimeout(() => {
      document.body.removeChild(tempLink);
      window.URL.revokeObjectURL(blobURL);
    }, 200);
  }
}

export function fetchDownloadFile(url, params = {}, fileName, cb) {
  return request({
    method: 'GET',
    url,
    params,
    responseType: 'blob',
    timeout: 60e3
  }).then(
    ({ headers, data }) => {
      if (!fileName) {
        const [, file] = decodeURIComponent(
          headers['content-disposition']
        ).split('filename=');
        fileName = file;
      }
      downloadFile(data, fileName, XLSX_TYPE);
    },
    (err) => {
      if (cb) {
        cb(err);
      } else {
        throw err;
      }
    }
  );
}

export function disableDisconnectRole(row) {
  return row.fromPosition === 2;
}

export function getTableCheckboxProps(row) {
  const disabled = disableDisconnectRole(row);
  return { disabled };
}

export function getRoleSourceText(value) {
  const roleSource = {
    1: 'roleSourceRole', // 来自角色
    2: 'roleSourcePosition', // 来自岗位
    3: 'roleSourceRolePosition' // 来自角色岗位
  };
  return roleSource[value];
}

export function checkDisabledConnectedRoles(rows) {
  return rows
    .filter((d) => disableDisconnectRole(d))
    .filter((v, i, s) => {
      return s.findIndex((vv) => vv.role.code === v.role.code) === i;
    })
    .map((d) => d.role.name)
    .join(',');
}
