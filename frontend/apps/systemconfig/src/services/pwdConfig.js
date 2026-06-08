import { request } from 'sup-rc-utility';

export function fetchPwdConfig() {
  return request({
    url: '/inter-api/auth/v1/password/config',
    method: 'GET'
  });
}

export function updatePwdConfig(data) {
  return request({
    url: '/inter-api/auth/v1/password/config',
    method: 'PUT',
    data
  });
}

export function resetPWdConfig() {
  return request({
    url: '/inter-api/auth/v1/password/config/reset',
    method: 'POST'
  });
}
