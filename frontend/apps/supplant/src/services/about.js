import axios from './request';

export function getPlatformInfo() {
  return axios.get('/msService/baseService/platform/info');
}
