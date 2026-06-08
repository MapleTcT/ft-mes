import { request } from 'sup-rc-utility';

// 按钮权限获取
export function getAuthority(code) {
  return request.get(
    `/inter-api/rbac/v1/userPermission/findUserOperate?menuInfoCode=${code}`
  );
}
